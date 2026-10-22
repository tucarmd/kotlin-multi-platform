#!/usr/bin/env bash
# Clone the TipSplit frame into apps/<NewApp>: tracked files only, package + names renamed.
# Usage: new-app.sh <NewApp> [SourceApp=TipSplit]   (run from anywhere inside the repo)
set -euo pipefail
NEW="${1:?Usage: new-app.sh <NewApp> [SourceApp]}"
SRC="${2:-TipSplit}"
[[ "$NEW" =~ ^[A-Z][A-Za-z0-9]+$ ]] || { echo "NewApp must be PascalCase, e.g. FuelLog"; exit 1; }
ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT/apps"
[ -d "$SRC" ] || { echo "apps/$SRC not found"; exit 1; }
[ -e "$NEW" ] && { echo "apps/$NEW already exists"; exit 1; }

src_pkg="$(echo "$SRC" | tr '[:upper:]' '[:lower:]')"
new_pkg="$(echo "$NEW" | tr '[:upper:]' '[:lower:]')"

from="/$src_pkg/"; to="/$new_pkg/"
# Copy only what git tracks: no build/, .gradle/, local.properties, DerivedData.
git -C "$SRC" ls-files -z | while IFS= read -r -d '' f; do
  dest="$NEW/${f//$from/$to}"   # bash 3.2 on macOS: keep pattern/replacement unquoted
  dest="${dest//$SRC/$NEW}"
  mkdir -p "$(dirname "$dest")"
  cp -p "$SRC/$f" "$dest"
done
[ -f "$SRC/local.properties" ] && cp "$SRC/local.properties" "$NEW/"

# Rename text in every non-binary file.
grep -rlI --exclude=gradle-wrapper.jar -e "$SRC" -e "$src_pkg" "$NEW" | while read -r f; do
  sed -i '' -e "s/$SRC/$NEW/g" -e "s/dev\.$src_pkg/dev.$new_pkg/g" -e "s#dev/$src_pkg#dev/$new_pkg#g" "$f"
done

echo "Created apps/$NEW (package dev.$new_pkg)."
echo "Still named after $SRC — rename by hand (domain words, not mechanical):"
words="$(echo "$SRC" | sed -E 's/([A-Z])/ \1/g' | xargs | tr ' ' '|')"
grep -rlE "$words" "$NEW" --include=*.kt --include=*.swift --include=*.yaml 2>/dev/null | sed 's/^/  /' || true
