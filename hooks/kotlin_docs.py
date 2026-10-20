"""Tab "Học Kotlin": lấy nguyên site repo kotlin-multi-platform vào site này lúc build, không copy vào git.

Nguồn duy nhất vẫn là repo kotlin-multi-platform → sửa bài ở đó, build site này là có bản mới.

    docs/kotlin  ──symlink──►  <repo kotlin-multi-platform>/docs        (gitignore, hook tự tạo)
    nav "Học Kotlin"         ◄─ nav trong mkdocs.yml của repo đó, thêm tiền tố kotlin/
    home.html                ◄─ overrides/ của repo đó → build/kotlin-overrides (đổi url sang kotlin/)
    lesson_bar.py            ◄─ hook "Bài N/14" của repo đó, chỉ đổi đường dẫn bài
    link ra code apps/…      ─► link GitHub của repo đó

Repo đó nằm chỗ khác thì chỉ đường: KOTLIN_DOCS_REPO=/đường/dẫn mkdocs serve
"""
import importlib.util
import os
import posixpath
import re
import shutil

import yaml
from markdown.extensions.toc import slugify as default_slugify
from mkdocs.exceptions import PluginError

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REPO = os.path.abspath(os.environ.get(
    "KOTLIN_DOCS_REPO", os.path.join(ROOT, "../../carmd-connect-app-build/kotlin-multi-platform")))
PREFIX = "kotlin/"
SECTION = "Học Kotlin"

LINK = re.compile(r"(\]\()([^)\s]+)(\))")
EXTERNAL = re.compile(r"^([a-z][a-z0-9+.-]*:|#|/)", re.I)
URL_IN_TEMPLATE = re.compile(r"\{\{ '([^']*)' \| url \}\}")

_state = {}


class _Loader(yaml.SafeLoader):
    """mkdocs.yml có tag !!python/… (emoji, mermaid) → bỏ qua, chỉ cần nav + repo_url."""


_Loader.add_multi_constructor("tag:yaml.org,2002:python/", lambda loader, suffix, node: None)


def _prefix(items):
    out = []
    for item in items:
        if isinstance(item, str):
            out.append(PREFIX + item)
        else:
            (title, value), = item.items()
            out.append({title: _prefix(value) if isinstance(value, list) else PREFIX + value})
    return out


def _link_docs():
    src = os.path.join(REPO, "docs")
    if not os.path.isdir(src):
        raise PluginError(f"Không thấy repo kotlin-multi-platform ở {REPO}. Đặt KOTLIN_DOCS_REPO=<đường dẫn repo>.")
    link = os.path.join(ROOT, "docs", "kotlin")
    if os.path.islink(link) and os.path.realpath(link) == os.path.realpath(src):
        return
    if os.path.lexists(link):
        os.remove(link)
    os.symlink(src, link)


def _overrides():
    """home.html dùng url gốc site ('roadmap/') → thêm kotlin/. Dropdown tab dùng bản gọn của site này (overrides/)."""
    out = os.path.join(ROOT, "build", "kotlin-overrides")
    shutil.rmtree(out, ignore_errors=True)
    os.makedirs(out)
    with open(os.path.join(REPO, "overrides", "home.html"), encoding="utf-8") as f:
        home = URL_IN_TEMPLATE.sub(lambda m: "{{ '" + PREFIX + m.group(1) + "' | url }}", f.read())
    with open(os.path.join(out, "home.html"), "w", encoding="utf-8") as f:
        f.write(home)
    return out


def _lesson_bar():
    spec = importlib.util.spec_from_file_location("kotlin_lesson_bar", os.path.join(REPO, "hooks", "lesson_bar.py"))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    mod.LESSON = re.compile(r"^" + PREFIX + r"lessons/(\d\d)-.+\.md$")
    return mod


def on_config(config):
    _link_docs()
    with open(os.path.join(REPO, "mkdocs.yml"), encoding="utf-8") as f:
        kotlin = yaml.load(f, Loader=_Loader)

    # nav: [Trang chủ Kotlin] + các trang khai báo sẵn trong mkdocs.yml + phần còn lại của site Kotlin
    knav = _prefix(kotlin["nav"])
    for item in config["nav"]:
        if isinstance(item, dict) and SECTION in item:
            item[SECTION] = knav[:1] + item[SECTION] + knav[1:]
            break
    else:
        raise PluginError(f'mkdocs.yml thiếu mục nav "{SECTION}"')

    config["theme"].dirs.insert(0, _overrides())
    config["extra_css"].insert(0, PREFIX + "stylesheets/extra.css")  # CSS của site này đứng sau → thắng khi trùng
    config["watch"].extend([os.path.join(REPO, "docs"), os.path.join(REPO, "overrides")])

    _state["repo_blob"] = kotlin["repo_url"].rstrip("/") + "/blob/main/"
    _state["slugify"] = config["mdx_configs"]["toc"]["slugify"]
    _state["lesson_bar"] = _lesson_bar()
    return config


def on_page_markdown(markdown, page, config, files):
    toc = config["mdx_configs"]["toc"]
    if not page.file.src_uri.startswith(PREFIX):
        toc["slugify"] = _state["slugify"]
        return markdown

    # Site Kotlin dùng slug mặc định (bỏ dấu) → giữ y vậy cho trang của nó, link #anchor bên đó mới đúng.
    # Page.render() đọc mdx_configs ngay sau hook này, từng trang một.
    toc["slugify"] = default_slugify

    page_dir = posixpath.dirname(page.file.src_uri)

    def fix(m):
        target = m.group(2)
        if EXTERNAL.match(target):
            return m.group(0)
        path, sep, anchor = target.partition("#")
        in_docs = posixpath.normpath(posixpath.join(page_dir, path))
        if in_docs.startswith(PREFIX):
            return m.group(0)  # trong docs/ của site Kotlin
        # ra ngoài docs/ của repo Kotlin (code apps/, README…) → GitHub của repo đó
        repo_path = posixpath.relpath(in_docs, "kotlin")[len("../"):]  # apps/x.kt ← docs/../apps/x.kt
        return m.group(1) + _state["repo_blob"] + repo_path + sep + anchor + m.group(3)

    markdown = LINK.sub(fix, markdown)
    return _state["lesson_bar"].on_page_markdown(markdown, page, config, files)
