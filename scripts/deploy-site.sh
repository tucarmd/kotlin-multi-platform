#!/usr/bin/env bash
# Build site MkDocs rồi đẩy HTML lên repo PUBLIC tucarmd/native-device-monitor-kmp-docs (nhánh gh-pages).
# Repo code này private → GitHub Free không bật Pages, nên chỉ HTML đã build được public.
# Link "mở file code" trong site trỏ về repo private → người ngoài sẽ thấy 404.
#
# Cần (1 lần / máy): remote `site` + push bằng tài khoản tucarmd qua gh —
#   git remote add site https://github.com/tucarmd/native-device-monitor-kmp-docs.git
#   K='credential.https://github.com/tucarmd/native-device-monitor-kmp-docs.git'
#   git config "$K.username" tucarmd; git config --add "$K.helper" ""; git config --add "$K.helper" '!gh auth git-credential'
set -euo pipefail
cd "$(dirname "$0")/.."
mkdocs build --strict
mkdocs gh-deploy --no-history --remote-name site --remote-branch gh-pages --force -m "deploy {sha}"
echo "→ https://tucarmd.github.io/native-device-monitor-kmp-docs/ (Pages build ~30–60s)"
