"""Ghi README.md vào site/ sau khi build → repo public native-device-monitor-kmp-docs (nhánh gh-pages) có README.

`mkdocs gh-deploy --force` thay cả nhánh gh-pages bằng site/, nên README phải sinh ra mỗi lần build.
Danh sách link lấy từ `nav` trong mkdocs.yml → thêm trang mới thì README tự có.
"""
import posixpath


def _url(site_url, src):
    # plan/README-KMP-MIGRATION.md → <site>/plan/README-KMP-MIGRATION/ ; index.md → <site>/
    path = src[:-3]
    if path == "index" or path.endswith("/index"):
        path = path[: -len("index")]
    else:
        path += "/"
    return site_url + path


def _lines(nav, site_url, depth=0):
    for item in nav:
        for title, value in item.items():
            pad = "  " * depth
            if isinstance(value, list):
                yield f"{pad}- **{title}**"
                yield from _lines(value, site_url, depth + 1)
            else:
                yield f"{pad}- [{title}]({_url(site_url, value)})"


def on_post_build(config):
    site_url = config["site_url"].rstrip("/") + "/"
    pages = "\n".join(_lines(config["nav"], site_url))
    readme = f"""# {config["site_name"]} · tài liệu

## 👉 Xem site: [{site_url}]({site_url})

{config["site_description"]}

Nhánh `gh-pages` này chỉ chứa **HTML đã build** (MkDocs Material), tự sinh bằng `scripts/deploy-site.sh` từ repo
private `{config["repo_name"]}`. Đừng sửa tay, lần deploy sau sẽ ghi đè.

## Các trang

{pages}
"""
    with open(posixpath.join(config["site_dir"], "README.md"), "w", encoding="utf-8") as f:
        f.write(readme)
