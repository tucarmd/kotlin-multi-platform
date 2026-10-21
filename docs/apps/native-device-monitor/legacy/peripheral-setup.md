[← How to implement](README-HOW-TO-IMPLEMENT.md) · [README](index.md)

# Setting up a test peripheral — nRF Connect (+ LightBlue, ESP32)

> Phone **A** = the phone running *Native Device Monitor* (central). Phone **B** = the simulated peripheral.
> Button names in nRF Connect / LightBlue may differ slightly between versions — look for the meaning, not the exact words.
> Reasons + advertising troubleshooting in detail: [How to implement §7.1](README-HOW-TO-IMPLEMENT.md#71-set-up-phone-b-as-the-peripheral-iphone-nrf-connect) · the full list of test cases: [§7.2](README-HOW-TO-IMPLEMENT.md#72-test-cases).

## Which peripheral to pick

| Phone B | Scan / Connect | Read | Notify | Use when |
|---|---|---|---|---|
| **Android + nRF Connect** (Advertiser + *Configure GATT server*) | ✅ | ✅ value you set | ✅ sent by hand | **recommended** — covers the whole flow |
| iPhone + nRF Connect (Advertiser only) | ✅ | ⚠️ only the iPhone's system services (`180A` → `Apple Inc.`) | ❌ practically none | Day 2 only (scan / connect) |
| iPhone / Android + **LightBlue** → *Virtual Devices* | ✅ | ✅ | ✅ | no second Android available |
| **ESP32** + Arduino sketch `BLE_notify` | ✅ | ✅ | ✅ pushes continuously | dense streams, the 50 cap, pulling the plug |

Prefer a **Battery Service `180F` → Battery Level `2A19` (READ + NOTIFY)**: the app picks it as both the *Read target* and the *Notify target* and decodes it as `90 %` — correct at a glance.

---

## 1. Peripheral setup — step by step

### A. Android B + nRF Connect (recommended)

**Step 1 — Create a GATT server** (what phone A will read / subscribe to)

1. Install **nRF Connect for Mobile** (Nordic) on phone B, turn Bluetooth on.
2. Menu ☰ → **Configure GATT server** → add a server config (or edit the *Sample configuration*).
3. **Add service** → pick the predefined **Battery Service** (`180F`).
4. In that service, characteristic **Battery Level** (`2A19`): tick the **Read** + **Notify** properties, set the initial value to `5A` (= 90).
   It comes with a **CCCD `2902`** descriptor — without it Notify reports red `has no CCCD` (case 24).
5. (Optional, to test pairing) add another characteristic with permission **Read encrypted** / **Read encrypted MITM**.
6. Save → select the new config as the active server.

**Step 2 — Advertise so phone A can find it**

1. **Advertiser** tab → `+` → set a name, e.g. `CMD-Test`.
2. *Advertising data* → **Add record** → **Complete Local Name**; add **Service UUID** `180F`.
3. Options: turn on **Connectable** · interval *Low latency* if available.
4. Save → **turn on the packet's toggle**. Keep phone B's screen on.

### B. iPhone B + nRF Connect (Day 2 only)

**Advertiser** tab → `+` → Local Name `CMD-Test` + Service UUID `180D` → *Connectable* → turn on the toggle. **Keep nRF Connect in the foreground** — once iOS goes to the background it drops the Local Name and Android can't see it.

### C. LightBlue (iOS or Android, Punch Through)

1. **Virtual Devices** tab → `+` → pick the **Heart Rate** or **Battery** template → turn on the virtual device switch.
2. Open the virtual device → tap a characteristic → edit the value (hex). Once phone A has subscribed, every edit = 1 notify packet.
3. iPhone: LightBlue must also stay in the foreground.

### D. ESP32

Arduino IDE → *Examples → BLE → BLE_notify* → flash. Service `4fafc201-…`, characteristic `beb5483e-…` (READ · WRITE · NOTIFY · INDICATE), pushes a `uint32` counter whenever a client subscribes. Nothing else to configure.

---

## 2. How connecting and working with it goes

```
 Phone B (peripheral)                                Phone A — Native Device Monitor
 ────────────────────                                ──────────────────────────────
 Advertiser ON  ── advertising packet "CMD-Test" ──► Scan → row CMD-Test
 GATT server    ◄── Connect ──────────────────────── select row → [Connect]
                ── list of services ───────────────► Discover (automatic) → 180F / 2A19 ★ target
 2A19 = 5A      ◄── Read ─────────────────────────── [Read]  → value: 90 %
 CCCD 2902 = 01 ◄── write CCCD (subscribe) ───────── [Notify] → button becomes [Stop]
 edit 2A19 → 50 ── notification ───────────────────► Received Data: NOTIFY 2A19 … → 80 %
                ◄── write CCCD = 00 ──────────────── [Stop]
                ◄── Disconnect ───────────────────── [Disconnect] → grey Status
```

- **Connect** only opens the link; the app **does not pair by itself**. Pairing (bonding) only happens when phone A touches a characteristic that requires encryption → the **OS** shows a pairing dialog.
- **Sending a notify from nRF Connect (Android B):** once A is connected, nRF Connect on B shows an extra tab / screen for that client (the *Server* area) → pick **Battery Level** → the edit-value button (↑ arrow) → enter `50` → choose **send notification** (or tick *notify* then Send). Each send = 1 new line on A.
- Phone A **only receives** notifications after tapping **Notify**; anything sent before that isn't pushed by the peripheral (CCCD is still `00 00`).
- Disconnecting = losing the subscription → after reconnecting you must tap **Notify** again. Why: [§6.0](README-HOW-TO-IMPLEMENT.md#60-how-notify-works-read-before-61).

---

## 3. Check along the flow: Scan → Connect → (Pair) → Read → Notify → Stop → Disconnect

On phone A keep *CarMD tools only* **off** and the Device Name box empty (or type `cmd`). Each step has exactly 1 correct result — if a step is wrong, stop at that step.

| # | Step (phone A) | Phone A must show | Phone B / notes | §7.2 case |
|---|---|---|---|---|
| 1 | **Scan** | row `CMD-Test`, RSSI changing constantly; stops by itself after 10 s | not visible → B's Advertiser toggle isn't on / iPhone B isn't in the foreground | 7 |
| 2 | Tap the row → **Connect** | spinner → `CONNECTED` tag + green dot `Connected to CMD-Test` | nRF on B shows the newly connected client | 8 |
| 3 | *(automatic)* **Discover** | `Discovering services…` → `N services · M characteristics`; row `2A19` labelled `READ` `NOTIFY` + `★ target` | `180F` missing → B hasn't selected the right server config | 19–20 |
| 4 | **Read** `2A19` | *Last read* `raw: 5A` · `value: 90 %` + 1 `READ` line | change the value on B then Read again → new number | 21 |
| 5 | **Pair** *(optional)* — Read the *encrypted* characteristic | the OS pairing dialog → **Pair** → Read succeeds | tap Cancel → red text `Read … failed, status=5/15` (🍎 *insufficient authentication/encryption* error), Status stays green | 22 |
| 6 | **Notify** `2A19` | the button becomes a blue **Stop** · `Notify ON: 2A19` | red `has no CCCD` → `2902` missing in step 1A.4 | 23–24 |
| 7 | B sends a notification (value `50`, `3C`…) | 1 line per send `HH:mm:ss.SSS NOTIFY 2A19 … → 80 %`, counter `N / 50` | with an ESP32 lines flow continuously, capped at the latest 50 | 23 |
| 8 | **Stop** | `Notify OFF: 2A19`, the button back to **Notify**, further sends from B add no lines | | 25 |
| 8b | *(task 004)* **Write** — needs a characteristic with the **Write** property (nRF: add a characteristic with *Write* ticked; ESP32 `BLE_notify` has one) | the GATT block shows `MTU … · max … bytes`; tap **Write** on that row → Hex `01 02` → **Send** → line `WRITE <uuid> 01 02 → 513 (uint16 LE)` | nRF on B sees the new value; no WRITE → no Write button | 31–34 |
| 9 | **Disconnect** | **grey** Status `Disconnected`, GATT block empty, old log kept | | 9, 26 |
| 10 | Connect again → **turn off phone B's Advertiser / Bluetooth** while notifying | **red** Status `Unexpected disconnect…`, no crash, Connect works again | ESP32: pull the plug | 10, 27 |

Logs to compare: 🤖 `adb logcat -s BleClient` · 🍎 Xcode console filtered on `[BleClient]`.
Unpair after testing: Settings → Bluetooth on **both phones** → *Forget / Unpair* `CMD-Test` (otherwise the dialog won't show next time).
