"""Sinh jobs.json + README-MOCKUPS.md cho mockup Device Pulse. Chạy: python3 apps/DevicePulse/design/mockups/gen_mockups.py

Prompt Android lấy lại từ jobs.json (giữ nguyên chữ); prompt iOS và bảng trạng thái nằm trong file này."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
BASE = "apps/DevicePulse/design/mockups"
OUT = ROOT / BASE
old = json.loads((OUT / "jobs.json").read_text(encoding="utf-8"))
ANDROID_STYLE = old["style_lock"]["android"] if isinstance(old["style_lock"], dict) else old["style_lock"]
NEGATIVE = old["jobs"][0]["negative"]

A = "../../androidApp/src/main/kotlin/dev/devicepulse/android/ui/"
I = "../../iosApp/iosApp/Views/"

ANDROID_CODE = {
    "01": ["DashboardScreen.kt"], "02": ["Components.kt"], "03": ["DetailScreen.kt"], "04": ["DetailScreen.kt"],
    "05": ["DetailScreen.kt"], "06": ["HistoryScreen.kt"], "07": ["HistoryScreen.kt"], "08": ["App.kt", "DashboardScreen.kt"],
}
IOS_CODE = {
    "01": ["DashboardView.swift"], "02": ["Components.swift"], "03": ["DetailView.swift"], "04": ["DetailView.swift"],
    "05": ["DetailView.swift"], "06": ["HistoryView.swift"], "07": ["HistoryView.swift"], "08": ["RootView.swift", "DashboardView.swift"],
}
STATUS = {
    "android-01-dashboard": "✅",
    "android-02-dashboard-states": "✅",
    "android-03-detail-battery": "✅",
    "android-04-detail-location": "✅ — lệch: các dòng nằm trong card và Rating là chip; code là dòng chữ thường, không card",
    "android-05-detail-network": "✅ — lệch: card và chip Rating giống 04; giá trị căn trái thay vì căn phải",
    "android-06-history": "✅",
    "android-07-history-empty": "✅",
    "android-08-dashboard-dark": "✅ — lệch: \"Discharging\"/\"Online\" xuống dòng thay vì nằm cạnh giá trị",
}

android_jobs = []
for j in old["jobs"]:
    if j.get("platform", "android") != "android":
        continue
    n, slug = j["id"].removeprefix("android-").split("-", 1)
    j = dict(j)
    j["id"] = f"android-{n}-{slug}"
    j["platform"] = "android"
    j["file"] = f"{j['id']}.png"
    j["dest"] = f"{BASE}/android/{n}-{slug}.png"
    j["code"] = ANDROID_CODE[n]
    if "androidApp" not in j["source"]:
        j["source"] = j["source"].replace("ui/", "androidApp …/android/ui/")
    android_jobs.append(j)

IOS_STYLE = (
    "Style: simple, flat developer UI mockup of a single iPhone screen built with native SwiftUI (iOS 17/18 look). "
    "Clean and readable like a spec screenshot, not a marketing render. Flat front view of the screen only, no hand, "
    "no device frame, no perspective. iOS status bar at top (time 9:41 left, signal, Wi-Fi and battery icons right, "
    "space for the Dynamic Island), home indicator bar at bottom. "
    "Palette: system background #FFFFFF, grouped/secondary background #F2F2F7, app tint #3D5AFF for buttons and toolbar items, "
    "label text #000000, secondary label #8A8A8E, separators #C6C6C8, good/green #2E7D33, warn/amber #B36B00, bad/red #FF3B30. "
    "Grade chips are capsules with the tone colour at 14% opacity as fill and the tone colour as caption semibold text. "
    "Font: San Francisco (SF Pro). Sensor cards are #F2F2F7 rounded rectangles with 16 pt corner radius and 16 pt padding, "
    "no shadow, 12 pt apart, 16 pt screen margins. \".bordered\" buttons are capsule-like rounded rectangles filled with the tint at "
    "about 15% opacity and tint-coloured text (no outline). \".borderedProminent\" buttons are solid tint with white text. "
    "Emoji icons are used exactly as written (🔋 📍 📶 🕘)."
)


def ios_job(n, slug, title, purpose, layout, style=IOS_STYLE):
    jid = f"ios-{n}-{slug}"
    prompt = (
        f"Subject: Device Pulse iOS app — {title}. {purpose}\n"
        f"{style}\n"
        f"Composition (top to bottom, render every text exactly as quoted, nothing else):\n{layout}\n"
        "Spec: portrait 1024x1536, opaque background, one screen only, all text crisp and legible.\n"
        f"Negative: {NEGATIVE}, Android status bar, Material Design, floating action button."
    )
    return {
        "id": jid, "platform": "ios", "file": f"{jid}.png", "size": "1024x1536", "format": "png",
        "background": "opaque", "text": "verbatim", "title": title, "source": purpose, "code": IOS_CODE[n],
        "prompt": prompt, "negative": NEGATIVE, "dest": f"{BASE}/ios/{n}-{slug}.png",
    }


TOOLBAR = "- Navigation bar: a toolbar button \"🕘 History\" in tint colour at the top right; below it the LARGE left-aligned bold navigation title \"Device Pulse\".\n"
SAVE = ("- Bottom: a translucent bar material strip pinned above the home indicator containing a full-width large "
        ".borderedProminent button \"Save reading\" (solid #3D5AFF, white text, rounded corners, 16 pt margin).")

ios_jobs = [
    ios_job("01", "dashboard", "Dashboard (all sensors available)", "Matches iosApp Views/DashboardView.swift with every Reading available.",
        TOOLBAR +
        "- Card 1: header row \"🔋\" (title2 size) + \"Battery\" (headline) and a green capsule chip \"OK\" at the right; "
        "below, \"78%\" in largeTitle bold and on the same baseline \"Charging\" in body size.\n"
        "- Card 2: header \"📍\" + \"Location\" and green chip \"Good\"; below, \"10.77689, 106.70081\" (title3) and on the next line secondary grey \"± 12 m\".\n"
        "- Card 3: header \"📶\" + \"Network\" and green chip \"Fast\"; below, \"Wi-Fi\" (title3) and on the same baseline green \"Online\"; "
        "then a row with \"64 ms\" on the left and a .bordered button \"Measure latency\" on the right.\n" + SAVE),
    ios_job("02", "dashboard-states", "Dashboard (non-value states)", "Shows how ReadingView in Views/Components.swift draws loading, permission denied and unsupported.",
        TOOLBAR +
        "- Card 1: header \"🔋\" + \"Battery\", no chip; body secondary grey text \"Not supported on this device\".\n"
        "- Card 2: header \"📍\" + \"Location\", no chip; body red text \"Permission denied\" and under it a .bordered button \"Grant permission\".\n"
        "- Card 3: header \"📶\" + \"Network\", no chip; body a small iOS activity spinner (grey spokes) followed by \"Reading…\" in subheadline; "
        "then a row with \"Not measured\" left and .bordered button \"Measure latency\" right.\n" + SAVE),
    ios_job("03", "detail-battery", "Detail — Battery", "Matches Views/DetailView.swift for .battery.",
        "- Navigation bar (inline, small centered title): back chevron \"‹ Device Pulse\" in tint at left; centered title \"🔋  Battery\".\n"
        "- Plain white background, no cards. Label/value rows (label secondary grey on the left, value medium weight black on the right):\n"
        "  \"Level\" — \"78%\"; \"Status\" — \"Charging\"; \"Rating\" — \"OK\" (plain text, not a chip).\n"
        "- A thin divider, then footnote grey text \"Critical ≤ 10% · Low ≤ 20% · otherwise OK\".\n- The rest of the screen empty."),
    ios_job("04", "detail-location", "Detail — Location", "Matches Views/DetailView.swift for .location.",
        "- Navigation bar (inline, small centered title): back chevron \"‹ Device Pulse\" in tint at left; centered title \"📍  Location\".\n"
        "- Plain white background, no cards. Label/value rows (label secondary grey left, value medium weight right):\n"
        "  \"Latitude\" — \"10.776889\"; \"Longitude\" — \"106.700806\"; \"Accuracy\" — \"12.4 m\"; \"Speed\" — \"3.6 km/h\"; "
        "\"Updated\" — \"09:41:07 · 14/10/2026\"; \"Rating\" — \"Good\" (plain text, not a chip).\n"
        "- Thin divider, then footnote grey text \"Good ≤ 20 m · Fair ≤ 100 m · otherwise Poor\".\n- No map. The rest of the screen empty."),
    ios_job("05", "detail-network", "Detail — Network", "Matches Views/DetailView.swift for .network.",
        "- Navigation bar (inline, small centered title): back chevron \"‹ Device Pulse\" in tint at left; centered title \"📶  Network\".\n"
        "- Plain white background, no cards. Label/value rows (label grey left, value right): \"Type\" — \"Wi-Fi\"; \"Status\" — \"Online\"; "
        "\"Latency\" — \"64 ms\"; \"Rating\" — \"Fast\" (all plain black text).\n"
        "- A .borderedProminent button \"Measure latency\" (solid #3D5AFF, white text, wrap-content width, left aligned).\n"
        "- Thin divider, then footnote grey text \"Fast < 100 ms · OK < 300 ms · otherwise Slow\".\n- The rest of the screen empty."),
    ios_job("06", "history", "History (with saved readings)", "Matches Views/HistoryView.swift with three snapshots in a SwiftUI List.",
        "- Navigation bar (inline): back chevron \"‹ Device Pulse\" in tint at left; centered title \"🕘 History\"; toolbar button \"Clear\" in tint at right.\n"
        "- Background #F2F2F7. An inset-grouped List: one white rounded-corner group with three rows separated by thin inset separators. "
        "Each row has 4 lines (first line semibold):\n"
        "  Row 1: \"09:41:07 · 14/10/2026\" / \"🔋 78% · Charging\" / \"📍 10.77689, 106.70081 (± 12 m)\" / \"📶 Wi-Fi · 64 ms\".\n"
        "  Row 2: \"08:15:32 · 14/10/2026\" / \"🔋 19% · Discharging\" / \"📍 10.77512, 106.69870 (± 48 m)\" / \"📶 Cellular · 212 ms\".\n"
        "  Row 3: \"22:03:55 · 13/10/2026\" / \"🔋 100% · Full\" / \"📍 —\" / \"📶 No connection · Offline\".\n- The rest of the screen empty grey."),
    ios_job("07", "history-empty", "History (empty)", "Matches the empty branch of Views/HistoryView.swift.",
        "- Navigation bar (inline): back chevron \"‹ Device Pulse\" in tint at left; centered title \"🕘 History\"; no right button.\n"
        "- White background. Centered in the screen, secondary grey centered text on two lines: \"No saved readings yet. Tap \\\"Save reading\\\" on the dashboard.\"\n"
        "- Nothing else."),
    ios_job("08", "dashboard-dark", "Dashboard (dark mode, warning grades)", "Same layout as ios-01 in iOS dark appearance, with readings that trigger warn/bad chips.",
        "- DARK MODE: background #000000, cards #1C1C1E, label text #FFFFFF, secondary label #8D8D93, tint stays #3D5AFF, red #FF453A.\n" + TOOLBAR +
        "- Card 1: \"🔋\" + \"Battery\", red chip \"Critical\"; \"8%\" largeTitle bold and on the same baseline \"Discharging\".\n"
        "- Card 2: \"📍\" + \"Location\", amber chip \"Fair\"; \"10.77512, 106.69870\" and below grey \"± 48 m\".\n"
        "- Card 3: \"📶\" + \"Network\", red chip \"Slow\"; \"Cellular\" with green \"Online\" on the same baseline; row \"412 ms\" left, .bordered button \"Measure latency\" right.\n"
        "- Bottom: dark translucent bar with a full-width .borderedProminent button \"Save reading\" (solid #3D5AFF, white text)."),
]

STATUS.update({
    "ios-01-dashboard": "✅",
    "ios-02-dashboard-states": "✅",
    "ios-04-detail-location": "✅ — lệch: giá trị nằm cột giữa; code dùng Spacer nên giá trị căn phải",
})
for j in ios_jobs:
    STATUS.setdefault(j["id"], "⬜ chưa có (Codex hết quota, sinh lại sau)")

data = {
    "version": 1, "repo": "app", "preset": "ui-mockup", "raw_dir": "raw",
    "style_lock": {"android": ANDROID_STYLE, "ios": IOS_STYLE},
    "jobs": android_jobs + ios_jobs,
}
(OUT / "jobs.json").write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def table(jobs, prefix):
    rows = []
    for j in jobs:
        n, slug = j["id"].split("-", 2)[1:]
        img = f"{j['platform']}/{n}-{slug}.png"
        code = " · ".join(f"[{c}]({prefix}{c})" for c in j["code"])
        rows.append(f"| [{n}-{slug}]({img}) | {j['title']} | {code} | {STATUS[j['id']]} |")
    return "| Ảnh | Màn | Code | Trạng thái |\n|---|---|---|---|\n" + "\n".join(rows)


def gallery(jobs):
    return "\n".join(f"<img src=\"{j['dest'].split('/mockups/')[1]}\" width=\"200\" alt=\"{j['id']}\">" for j in jobs if (ROOT / j['dest']).exists())


GEN = "~/.claude/plugins/cache/tu-skills/core/0.13.4/skills/asset-prompt/scripts/collect_raw.py"
md = f"""# Device Pulse — UI mockups

Mockup đơn giản cho dev nhìn và đối chiếu khi code UI. App giờ tách 3 module:

```
shared/      logic chung KMP (sensor, rules, PulseMonitor, SQLDelight) — không có UI
androidApp/  UI Jetpack Compose (Material 3)   → mockups/android/
iosApp/      UI SwiftUI                         → mockups/ios/
```

Hai bên cùng màn, cùng chuỗi (`androidApp/src/main/res/values/strings.xml` = `iosApp/iosApp/en.lproj/Localizable.strings`),
nhưng control native khác nhau nên mỗi nền tảng một bộ ảnh. Ảnh sinh bằng Codex CLI (`codex exec`, tool image_generation),
1024x1536. Prompt máy đọc: [`jobs.json`](jobs.json).

## Android (Compose)

{table(android_jobs, A)}

{gallery(android_jobs)}

## iOS (SwiftUI)

{table(ios_jobs, I)}

{gallery(ios_jobs)}

Ảnh lệch code → ghi ở cột Trạng thái. Code đổi UI → sửa prompt/trạng thái trong [`gen_mockups.py`](gen_mockups.py), chạy lại nó, rồi sinh lại đúng màn đó.

## Sinh lại một ảnh

```bash
cd apps/DevicePulse/design/mockups
P=$(python3 -c "import json;print(next(j['prompt'] for j in json.load(open('jobs.json'))['jobs'] if j['id']=='ios-01-dashboard'))")
codex exec --skip-git-repo-check -s workspace-write "Use your image generation tool to generate exactly ONE image, portrait 1024x1536, \\
  save it to ./raw/ios-01-dashboard.png. PROMPT: $P"
cd - && python3 {GEN} apps/DevicePulse/design/mockups/jobs.json --apply   # copy raw/ → android/ hoặc ios/
```

Soát chữ trong ảnh trước khi commit — generator hay viết sai chữ. Xong thì xoá ảnh trong `raw/`.

## Style lock

**Android:** {ANDROID_STYLE}

**iOS:** {IOS_STYLE}

## Prompts
"""
for j in android_jobs + ios_jobs:
    md += f"\n### {j['id']} — {j['title']}\n\n```text\n{j['prompt']}\n```\n"
(OUT / "README-MOCKUPS.md").write_text(md, encoding="utf-8")
print("ok", len(android_jobs), len(ios_jobs))
