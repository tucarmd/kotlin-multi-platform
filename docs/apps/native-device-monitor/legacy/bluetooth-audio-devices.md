[← How to implement §8.3](README-HOW-TO-IMPLEMENT.md#83-filter-unknown-device-audio-device)

# Bluetooth audio devices — recognising headphones / speakers in a BLE scan

> Status: 🟡 **coded 22/09** — 🤖 `isAudioDevice()` in `ui/scanner/ScannerActions.kt` (+ `BluetoothDecoder.appearanceOf`, tables in `BluetoothLookup.kt`) · 🍎 `UI/Scanner/ScannerActions.swift`. Unit tests pass; **not yet surveyed in the field** with real headphones (§4), so the set of signs may still need tuning.

## 1. Why headphones end up in the BLE scan list

```
 AirPods / Galaxy Buds / JBL
   ├─ Bluetooth Classic (A2DP, HFP)  ── music + calls      ── does NOT show up in a BLE scan
   └─ BLE advertising                ── quick-pair popup, battery, Find My
                                         └─► the app's scan sees it ─► Scanner list full of headphones
```

Headphones **really do send BLE packets**, but they aren't BLE (GATT) devices this app needs to connect to → the *Hide audio devices* filter hides them so the `CMD-…` dongle is quicker to find.
**Classic-only** headphones never show up in a BLE scan in the first place — no filtering needed.

## 2. How to recognise them

Before connecting, the app only has the **advertising packet**. No field says "I am headphones" → it has to be **inferred** from signs:

| Sign | What it is | 🤖 | 🍎 | Confidence |
|---|---|:-:|:-:|---|
| **Appearance** (AD type `0x19`) | the device declares its type from the Bluetooth SIG code table: `0x0941` Earbud · `0x0942` Headset · `0x0943` Headphones · `0x084x` Audio Sink (speaker) · `0x088x` Audio Source | ✅ read from `scanRecord.bytes` | ❌ CoreBluetooth doesn't expose it | the most reliable, but many vendors don't send it |
| **LE Audio service UUID** | `184E` ASCS · `1850` PACS · `1853` CAP · `184F` BASS — only LE Audio devices have them | ✅ | ✅ | reliable, but few headphones support it yet |
| **Device class** | `device.bluetoothClass.majorDeviceClass == AUDIO_VIDEO` — the system knows it from a previous pairing / Classic scan | ✅ | ❌ | reliable, but only for paired or dual-mode devices |
| **Google Fast Pair** | service data `FE2C` — Android earbuds (Pixel Buds, Sony, JBL…) advertise it for the quick-pair popup | ✅ | ✅ | fairly reliable — **trackers use it too**; the code still hides a device with only `FE2C` (a tracker isn't a dongle either) |
| **Apple proximity pairing** | manufacturer `0x004C` + type byte `0x07` — the packet that pops up AirPods on an iPhone | ✅ | ✅ | fairly reliable for AirPods/Beats; derived from community reverse engineering, Apple doesn't publish it. (`0x12` is Find My / AirTag — **not** audio) |
| Name contains `Buds`, `AirPods`, `JBL`… | guessing from the name | ✅ | ✅ | weak — **not used** |

## 3. Principles (applied in the code)

- **There's no 100% reliable way.** Headphones sending none of the signs above → the filter doesn't recognise them → they stay visible.
- **Better to miss one than wrongly hide a `CMD-…` dongle**: hide only on a reliable sign; the Connected / selected device is never hidden.
- 🍎 recognises **fewer** than 🤖 (no Appearance + device class) — stated plainly, no attempt to make both sides equal by guessing names.
- Pure function `isAudioDevice(adv)` in `ScannerActions.kt` / `ScannerActions.swift`; Appearance / UUID code tables in `BluetoothLookup`; unit tests share the same cases on both platforms (🍎 skips the Appearance / device class cases).
- Fast Pair `FE2C` **on its own** also counts as audio: trackers use `FE2C` too, but a tracker isn't a dongle either → wrongly hiding a tracker is fine.

## 4. Remaining work: field survey

**Survey in the field to confirm / tune the rules:** scan on the Samsung A32 (and an iPhone) next to a few real headphones — AirPods, Galaxy Buds, an Android headset with Fast Pair — record what Appearance / Service UUID / service data / manufacturer data each one sends, fill in the table below, then lock down the set of signs.

| Device | Appearance | Service UUID / service data | Manufacturer data | 🤖 device class | Recognised by |
|---|---|---|---|---|---|
| _(not surveyed yet)_ | | | | | |

## References

- Bluetooth SIG — *Assigned Numbers* (Appearance values, 16-bit UUIDs, company identifiers).
- Android: [`BluetoothClass.Device.Major`](https://developer.android.com/reference/android/bluetooth/BluetoothClass.Device.Major) · [`ScanRecord`](https://developer.android.com/reference/android/bluetooth/le/ScanRecord).
- iOS: [Advertisement data retrieval keys](https://developer.apple.com/documentation/corebluetooth/advertisement-data-retrieval-keys).
- Google [Fast Pair](https://developers.google.com/nearby/fast-pair) (service UUID `0xFE2C`).
