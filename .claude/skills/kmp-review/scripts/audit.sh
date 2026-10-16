#!/usr/bin/env bash
# Static frame audit for apps/<App>. Hints only: read each hit before calling it a bug.
# Usage: audit.sh <App>
set -uo pipefail
APP="${1:?Usage: audit.sh <App>}"
ROOT="$(git rev-parse --show-toplevel)"
DIR="$ROOT/apps/$APP"
[ -d "$DIR" ] || { echo "apps/$APP not found"; exit 1; }
SH="$DIR/shared/src"
HITS="$(mktemp)"; trap 'rm -f "$HITS"' EXIT
check() { # title, then grep output on stdin. Runs in a pipe subshell, so count hits through a file.
  local title="$1" out; out="$(cat)"
  if [ -n "$out" ]; then echo "✗ $title"; echo "$out" | sed "s#$DIR/#  #"; echo x >> "$HITS"; else echo "✓ $title"; fi
}

code_only() { grep -vE ':[0-9]+:[[:space:]]*(\*|//|/\*)'; }  # drop KDoc and comment lines

grep -rnE '^import (androidx|android\.|platform\.UIKit)' "$SH/commonMain" 2>/dev/null \
  | check "commonMain imports no Android/UIKit"
grep -rnE '\bDouble\b' "$SH/commonMain" 2>/dev/null | code_only | grep -iE 'money|price|amount|bill|total|tip|cost' \
  | check "money is never Double"
grep -rnE 'Clock\.System|currentTimeMillis|Random\.' "$SH"/commonMain/kotlin/*/*/rules 2>/dev/null | code_only \
  | check "rules/ does not read clock or random itself"
# Rethrowing CancellationException is required by coroutines, so a throw right after that catch is fine.
grep -rnE -B1 '^\s*throw ' "$SH"/commonMain/kotlin/*/*/core 2>/dev/null \
  | awk '/catch \(e: CancellationException\)/ {skip=1; next} /^--$/ {next} /:[0-9]+:/ && /throw / {if (!skip) print} {skip=0}' \
  | check "core/ does not throw toward the UI"
for f in $(find "$SH" "$DIR/androidApp/src" -name '*.kt' 2>/dev/null); do
  grep -q '\[App\|\[Phụ lục' "$f" || echo "$f"
done | check "every .kt file has an [App N · Bx] header"
grep -rnE 'fun `[^`]*[,.:][^`]*`' "$SH"/commonTest 2>/dev/null \
  | check "test names have no , . : (Kotlin/Native rejects them)"
grep -rnE '/\*[^*]' "$SH" 2>/dev/null | grep -vE '^\S+:\s*/\*' | grep -E '\*\s.*[a-z]/\*' \
  | check "no nested /* inside KDoc"

# Apps moved from <App>/ to apps/<App>/ in 81a6cd8, so ask for both paths to keep the old history.
echo; echo "Commits (look for one per step B1–B7):"; git -C "$ROOT" log --oneline -- "apps/$APP" "$APP" | sed 's/^/  /'

echo "TRAP comments: $(grep -rn 'TRAP:' "$DIR" --include=*.kt --include=*.swift --include=*.kts --include=*.yaml 2>/dev/null | wc -l | tr -d ' ')"
fails=$(wc -l < "$HITS" | tr -d ' ')
echo; [ "$fails" -eq 0 ] && echo "No hits." || echo "$fails check(s) with hits."
