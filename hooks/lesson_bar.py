"""Chèn "Bài N/M" + thanh các bài dưới tiêu đề mỗi trang lessons/NN-*.md (kiểu tutorial của Kotlin docs)."""
import html
import re

LESSON = re.compile(r"^lessons/(\d\d)-.+\.md$")


def on_page_markdown(markdown, page, config, files):
    m = LESSON.match(page.file.src_uri)
    if not m:
        return markdown
    lessons = [f for f in files if LESSON.match(f.src_uri)]
    titles = {}
    for item in config["nav"]:
        for group in item.values() if isinstance(item, dict) else []:
            for entry in group if isinstance(group, list) else []:
                if isinstance(entry, dict):
                    (title, path), = entry.items()
                    titles[path] = title
    n = int(m.group(1))
    steps = []
    for f in sorted(lessons, key=lambda f: f.src_uri):
        i = int(LESSON.match(f.src_uri).group(1))
        state = "now" if i == n else ("done" if i < n else "todo")
        stem = f.src_uri.split("/")[-1][:-3]
        steps.append(f'<a class="tut__step tut__step--{state}" href="../{stem}/" '
                     f'title="{html.escape(titles.get(f.src_uri, stem))}">{i}</a>')
    bar = (f'\n<div class="tut"><p class="tut__label">Bài {n}/{len(lessons)} của lộ trình '
           f'<a href="../../roadmap/">Học Kotlin Multiplatform qua app Device Pulse</a></p>\n'
           f'<nav class="tut__bar">{"".join(steps)}</nav></div>\n')
    return re.sub(r"^(# .+\n)", lambda h: h.group(1) + bar, markdown, count=1, flags=re.M)
