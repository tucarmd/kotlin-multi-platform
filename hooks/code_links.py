"""Đổi link trỏ ra ngoài docs/ (code Kotlin, Swift, Gradle…) thành link GitHub lúc build site.

File .md gốc không đổi nên đọc trên GitHub vẫn đúng. README.md ở gốc repo → trang chủ site (index.md).
"""
import posixpath
import re

LINK = re.compile(r"(\]\()([^)\s]+)(\))")
EXTERNAL = re.compile(r"^([a-z][a-z0-9+.-]*:|#|/)", re.I)


def on_page_markdown(markdown, page, config, files):
    base = config["repo_url"].rstrip("/") + "/blob/main/"
    page_dir = posixpath.dirname(page.file.src_uri)

    def fix(m):
        target = m.group(2)
        if EXTERNAL.match(target):
            return m.group(0)
        path, sep, anchor = target.partition("#")
        in_docs = posixpath.normpath(posixpath.join(page_dir, path))
        if not in_docs.startswith("../"):
            return m.group(0)  # trong docs/: để MkDocs tự kiểm tra
        repo_path = in_docs[len("../"):]
        if repo_path == "README.md":
            new = posixpath.relpath("index.md", page_dir or ".")
        else:
            new = base + repo_path
        return m.group(1) + new + sep + anchor + m.group(3)

    return LINK.sub(fix, markdown)
