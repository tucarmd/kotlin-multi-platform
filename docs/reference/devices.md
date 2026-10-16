# Chạy trên máy thật và máy ảo

Setup một lần cho cả 4 loại máy: Android emulator, điện thoại Android thật, iOS Simulator và iPhone thật. Lệnh mẫu dùng app [Device Pulse](../project/index.md) (`applicationId = dev.devicepulse`, bundle id iOS là `dev.devicepulse.ios`).

| | Android emulator | Android thật | iOS Simulator | iPhone thật |
|---|---|---|---|---|
| Cần gì | Android Studio | Cáp USB hoặc Wi-Fi | Xcode | Xcode + Apple ID |
| Pin, GPS, mạng | Giả lập được | Đo thật | GPS giả lập được, **không có pin** | Đo thật |
| Khi nào dùng | Viết code hằng ngày | Kiểm cảm biến, hiệu năng, BLE | Kiểm UI iOS | Trước khi chốt tính năng iOS |

!!! tip "Nguyên tắc"
    Viết code trên máy ảo cho nhanh, nhưng **mọi tính năng đọc cảm biến phải chạy trên máy thật ít nhất một lần** trước khi đánh dấu xong. Máy ảo trả về số "đẹp" nên không lộ được lỗi xin quyền hay lỗi khi app chạy nền.

## 1. Android emulator

**Tạo máy ảo (AVD):**

1. **View → Tool Windows → Device Manager** (ở màn Welcome thì vào *More Actions → Virtual Device Manager*).
2. **Create Device**, chọn một mẫu Pixel rồi **Next**.
3. Chọn system image:
    - Máy Mac chip Apple (M1 trở lên) phải chọn image **arm64**. Image x86 chạy trên chip này không có tăng tốc phần cứng nên rất chậm.
    - Chọn **Google Play** nếu cần Play Store hoặc đăng nhập Google. Chọn **Google APIs** nếu cần Play Services và quyền root. Chọn AOSP nếu cần máy sạch.
    - API level phải ≥ `minSdk` (Device Pulse là 24). Nên tạo 2 máy: một máy **API mới nhất** để thử tính năng mới, và một máy **API thấp** (ví dụ 26) để bắt lỗi trên máy cũ.
4. **Finish** rồi bấm ▶ để chạy.

**Thao tác hay dùng:**

| Việc | Cách làm |
|---|---|
| Máy ảo lỗi lạ hoặc đầy bộ nhớ | Device Manager → ⋮ → **Wipe Data** (đưa về trạng thái mới mua) |
| Khởi động sạch, không dùng snapshot | ⋮ → **Cold Boot Now** |
| Bơm giá trị pin, GPS, mạng | Nút `⋯` (*Extended Controls*): xem [bài 2](../lessons/02-setup.md#gia-lap-cam-bien-dung-tu-bai-9) |
| Mở máy ảo bằng dòng lệnh | `emulator -list-avds` rồi `emulator -avd <tên>` (nằm trong `$ANDROID_HOME/emulator/`) |

**Giả lập cảm biến bằng lệnh** (nhanh hơn bấm tay, và viết được thành script test):

```bash
adb emu power capacity 15        # pin còn 15%
adb emu power ac off             # rút sạc
adb emu geo fix 106.7009 10.7769 # vị trí (kinh độ trước, vĩ độ sau): Q1, TP.HCM
adb shell svc wifi disable       # tắt Wi-Fi để thử mất mạng
adb shell svc wifi enable
```

## 2. Điện thoại Android thật

**Bật chế độ nhà phát triển:**

1. **Settings → About phone** → chạm **Build number** 7 lần. Máy Xiaomi chạm *MIUI/HyperOS version*; máy Samsung vào *Software information → Build number*.
2. **Settings → System → Developer options** → bật **USB debugging**.
3. Máy Xiaomi/Redmi/POCO phải bật thêm **USB debugging (Security settings)** và **Install via USB**, rồi khởi động lại máy.

**Cắm cáp USB:**

1. Cắm cáp. Trên máy hiện hộp thoại **Allow USB debugging?** → tick *Always allow from this computer* → **Allow**.
2. Kiểm tra bằng lệnh `adb devices`. Thấy `device` là kết nối được; thấy `unauthorized` thì mở khóa máy và bấm Allow lại.
3. Trên Mac không cần cài driver. Nếu `adb devices` không thấy máy thì thử đổi cáp: nhiều cáp chỉ sạc được, không truyền dữ liệu.

**Không dây (Android 11 trở lên):**

1. Máy và Mac phải dùng chung mạng Wi-Fi.
2. Trên máy: **Developer options → Wireless debugging** → bật.
3. Android Studio: menu chọn thiết bị → **Pair Devices Using Wi-Fi** → quét QR bằng máy, hoặc dùng mã 6 số (trên máy chọn *Pair device with pairing code*).
4. Dùng dòng lệnh thì làm như sau. Port để pair khác port để connect, cả hai đều hiện trên màn *Wireless debugging*:

    ```bash
    adb pair 192.168.1.42:37123      # nhập mã 6 số
    adb connect 192.168.1.42:41567
    adb devices
    ```

5. Từ Android 17 và adb 37 có **adb Wi-Fi 2.0**: khi máy ở trên mạng Wi-Fi đã tin cậy thì tự kết nối lại, không phải pair lại mỗi lần.

!!! warning "Mạng lạ"
    Kết nối kiểu `adb tcpip` và kênh từ Android Studio tới adb server **không mã hóa**. Chỉ debug không dây trên mạng nhà hoặc mạng công ty.

**Chạy Device Pulse lên máy:**

```bash
cd apps/DevicePulse
./gradlew :androidApp:installDebug                 # build và cài lên máy đang kết nối
adb shell am start -n dev.devicepulse/.android.MainActivity
adb logcat --pid=$(adb shell pidof dev.devicepulse) # chỉ xem log của app
```

Nếu đang cắm nhiều máy, thêm `-s <serial>` vào lệnh adb (serial lấy từ `adb devices`), hoặc `-d` để chọn máy cắm USB, `-e` để chọn emulator.

**adb hay dùng:**

| Việc | Lệnh |
|---|---|
| Chụp màn hình | `adb exec-out screencap -p > shot.png` |
| Quay video | `adb shell screenrecord /sdcard/demo.mp4` (Ctrl+C để dừng), rồi `adb pull /sdcard/demo.mp4` |
| Thu hồi quyền để test xin quyền lại từ đầu | `adb shell pm revoke dev.devicepulse android.permission.ACCESS_FINE_LOCATION` |
| Xóa dữ liệu app | `adb shell pm clear dev.devicepulse` |
| Giả lập hệ điều hành kill app | Đưa app xuống nền, rồi `adb shell am kill dev.devicepulse` |
| Gỡ app | `adb uninstall dev.devicepulse` |
| Máy không lên dù đã làm đủ bước | `adb kill-server && adb start-server`, hoặc **Tools → Troubleshoot Device Connections** |

## 3. iOS Simulator

1. Mở Xcode một lần. Vào **Xcode → Settings → Components**, tải một iOS runtime. Device Pulse cần iOS ≥ 17.
2. Chạy bằng cấu hình `iosApp` trong Android Studio, hoặc mở `iosApp/iosApp.xcodeproj` trong Xcode → chọn simulator → ⌘R.
3. Giả lập vị trí: menu **Features → Location**. Simulator **không có pin** và dùng chung mạng với máy Mac.

Lệnh hay dùng:

```bash
xcrun simctl list devices booted                     # simulator nào đang chạy
xcrun simctl io booted screenshot shot.png           # chụp màn hình
xcrun simctl location booted set 10.7769,106.7009    # vị trí (vĩ độ trước, kinh độ sau)
xcrun simctl privacy booted reset all dev.devicepulse.ios  # xóa hết quyền đã cấp
```

## 4. iPhone thật

**Một lần cho mỗi máy Mac:**

1. **Xcode → Settings → Accounts → +** → đăng nhập Apple ID.
    - Dùng Apple ID miễn phí (*Personal Team*) vẫn cài được lên máy mình, nhưng app chỉ chạy được **7 ngày** rồi phải cài lại, và không dùng được một số capability như push.
    - Tài khoản Apple Developer trả phí thì không bị giới hạn đó.
2. Lấy Team ID: chạy `kdoctor --team-ids`, hoặc xem ở Xcode → target → **Signing & Capabilities → Team**.

**Một lần cho mỗi iPhone:**

1. Cắm cáp vào Mac → trên iPhone bấm **Trust This Computer**.
2. Mở Xcode → **Window → Devices and Simulators** để Xcode chuẩn bị máy. Lần đầu mất vài phút.
3. iOS 16 trở lên: **Settings → Privacy & Security → Developer Mode** → bật → máy khởi động lại → bấm **Turn On**. Mục này chỉ hiện ra sau khi đã cắm máy vào Xcode.
4. Nếu dùng Personal Team: sau lần cài đầu tiên, vào **Settings → General → VPN & Device Management** → chọn Apple ID của bạn → **Trust**.
5. Muốn chạy không dây: trong *Devices and Simulators*, tick **Connect via network**. iPhone và Mac phải dùng chung Wi-Fi.

**Ký app Device Pulse:**

Project iOS được sinh từ `iosApp/project.yml` bằng XcodeGen, nên **đừng chỉ chọn Team trong Xcode**. Lần `xcodegen generate` sau sẽ ghi đè và làm mất lựa chọn đó. Làm như sau:

1. Trong `iosApp/project.yml`, điền `DEVELOPMENT_TEAM: "<TEAM_ID>"`.
2. Bundle id `dev.devicepulse.ios` phải là duy nhất trên tài khoản Apple. Nếu bị báo trùng, đổi thành `dev.devicepulse.<tên-bạn>.ios`.
3. Chạy `cd iosApp && xcodegen generate`, rồi mở Xcode → chọn iPhone → ⌘R.

| Lỗi | Sửa |
|---|---|
| *Signing for "iosApp" requires a development team* | Chưa điền `DEVELOPMENT_TEAM` (bước 1 ở trên) |
| *Failed to register bundle identifier* | Bundle id đã có người dùng, đổi tên khác |
| *Developer Mode disabled* | Làm bước 3 ở mục "Một lần cho mỗi iPhone" |
| *Untrusted Developer* khi mở app | Làm bước 4 ở mục "Một lần cho mỗi iPhone" |
| *iPhone is busy: Preparing debugger support* | Đợi Xcode chuẩn bị xong, lần đầu mất 5–10 phút |
| Build lỗi ở bước *Compile Kotlin Framework* | Chạy `./gradlew :shared:embedAndSignAppleFrameworkForXcode` trong terminal để xem lỗi Kotlin đầy đủ |

## Checklist

- [ ] Có 2 AVD (API mới nhất + API thấp, image arm64), cả hai đều chạy được Device Pulse.
- [ ] Điện thoại Android thật kết nối được qua USB **và** qua Wi-Fi.
- [ ] Biết chỉnh pin và GPS của emulator bằng lệnh `adb emu`.
- [ ] Thu hồi quyền GPS bằng `pm revoke`, mở lại app và thấy app xin quyền lại.
- [ ] Device Pulse chạy trên iOS Simulator.
- [ ] Device Pulse chạy trên iPhone thật: đã điền Team ID, bật Developer Mode, đọc được pin thật.

## Nguồn

- [Run apps on a hardware device](https://developer.android.com/studio/run/device) · [Create and manage virtual devices](https://developer.android.com/studio/run/managing-avds) · [Android Debug Bridge (adb)](https://developer.android.com/tools/adb)
- [Running your app on a device (Apple)](https://developer.apple.com/documentation/xcode/running-your-app-in-simulator-or-on-a-device) · [Enabling Developer Mode on a device](https://developer.apple.com/documentation/xcode/enabling-developer-mode-on-a-device)
- [Create your Compose Multiplatform app](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-create-first-app.html)
