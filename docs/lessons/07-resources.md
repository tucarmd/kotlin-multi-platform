# Resources và đa ngôn ngữ

Chữ, ảnh, font của app đặt **một chỗ** trong `composeResources` và dùng chung cho Android lẫn iOS. Plugin Compose sinh
sẵn lớp `Res` để gọi an toàn kiểu. Bài này đưa mọi chữ của Dashboard ra file, thêm icon cho 3 thẻ và định dạng số đo.

## Mục tiêu

- Đưa mọi chữ trên màn ra `strings.xml`, có bản tiếng Anh và tiếng Việt.
- Dùng `stringResource`, `pluralStringResource`, `painterResource`.
- Định dạng phần trăm pin, mét, mili-giây ở một chỗ.

## Thư mục

```text
composeApp/src/commonMain/composeResources/
├── values/strings.xml        # mặc định (tiếng Anh)
├── values-vi/strings.xml     # tiếng Việt — chọn theo ngôn ngữ máy
├── drawable/ic_battery.xml   # vector (Android Vector Drawable), png, svg
└── font/                     # font riêng nếu có
```

Sau khi thêm hoặc sửa file, build lại (hoặc chạy task `generateComposeResClass`) để lớp `Res` cập nhật.

## Chuỗi, tham số và số nhiều

- Tham số theo vị trí: `%1$s`, `%1$d`, `%2$s`… Truyền giá trị qua `stringResource(Res.string.x, a, b)`.
- Số nhiều: thẻ `<plurals>` với `quantity="one"`, `"other"`…; gọi `pluralStringResource(Res.plurals.x, count, count)`
  (đối số thứ hai chọn dạng, các đối số sau điền vào chuỗi). Tiếng Việt chỉ cần `other`.
- Ngoài composable (ViewModel, test) dùng hàm `suspend` `getString(Res.string.x)`.

## Icons

Hai lựa chọn:

1. **Vector drawable** (khuyên dùng): tải icon dạng *Android* XML từ [Material Symbols](https://fonts.google.com/icons),
   bỏ vào `drawable/`, gọi `painterResource(Res.drawable.ic_battery)`. Chỉ mang theo đúng icon cần dùng.
2. Bộ `Icons.Default.*` của thư viện material icons: tiện, nhưng bản *extended* rất nặng và không còn được cập nhật thêm icon mới.

## Làm trên Device Pulse

`values/strings.xml`:

```xml
<resources>
    <string name="app_name">Device Pulse</string>
    <string name="card_battery">Battery</string>
    <string name="card_location">Location</string>
    <string name="card_network">Network</string>
    <string name="charging_charging">Charging</string>
    <string name="charging_discharging">On battery</string>
    <string name="charging_full">Full</string>
    <string name="charging_unknown">Unknown</string>
    <string name="reading_permission_denied">Permission not granted</string>
    <string name="reading_unsupported">Not supported on this device</string>
    <string name="reading_error">Error: %1$s</string>
    <string name="unit_meters">%1$s m</string>
    <string name="unit_kilometers">%1$s km</string>
    <string name="unit_millis">%1$d ms</string>
    <plurals name="updated_seconds_ago">
        <item quantity="one">Updated %1$d second ago</item>
        <item quantity="other">Updated %1$d seconds ago</item>
    </plurals>
</resources>
```

`values-vi/strings.xml` — cùng `name`, chỉ đổi chữ:

```xml
<resources>
    <string name="card_battery">Pin</string>
    <string name="card_location">Vị trí</string>
    <string name="card_network">Mạng</string>
    <string name="charging_charging">Đang sạc</string>
    <string name="charging_discharging">Đang dùng pin</string>
    <string name="charging_full">Đầy</string>
    <string name="charging_unknown">Không rõ</string>
    <string name="reading_permission_denied">Chưa được cấp quyền</string>
    <string name="reading_unsupported">Máy này không hỗ trợ</string>
    <string name="reading_error">Lỗi: %1$s</string>
    <plurals name="updated_seconds_ago">
        <item quantity="other">Cập nhật %1$d giây trước</item>
    </plurals>
</resources>
```

Chuỗi nào thiếu ở `values-vi` sẽ lấy từ `values` (ví dụ `unit_meters`, `app_name`).

Định dạng số đo gom vào một file. `String.format` không có trong `commonMain`, nên làm tròn bằng tay:

```kotlin
// commonMain/kotlin/dev/devicepulse/ui/Format.kt
@Composable
fun ChargingState.label(): String = stringResource(
    when (this) {
        ChargingState.Charging -> Res.string.charging_charging
        ChargingState.Discharging -> Res.string.charging_discharging
        ChargingState.Full -> Res.string.charging_full
        ChargingState.Unknown -> Res.string.charging_unknown
    }
)

fun percent(value: Int): String = "$value%"

@Composable
fun distance(meters: Double): String =
    if (meters < 1000) stringResource(Res.string.unit_meters, meters.roundToInt().toString())
    else stringResource(Res.string.unit_kilometers, ((meters / 100).roundToInt() / 10.0).toString())

@Composable
fun millis(ms: Long): String = stringResource(Res.string.unit_millis, ms)
```

Dùng trong `SensorCard` và `DashboardScreen` của bài 6:

```kotlin
import devicepulse.composeapp.generated.resources.*   // package sinh ra tuỳ project — để IDE gợi ý import
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

SensorCard(stringResource(Res.string.card_battery), state.battery, icon = Res.drawable.ic_battery) { b ->
    Text("${percent(b.levelPercent)} · ${b.charging.label()}")
}

// trong SensorCard: thêm tham số icon: DrawableResource
Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Icon(painterResource(icon), contentDescription = null)
    Text(title, style = MaterialTheme.typography.titleMedium)
}
```

`contentDescription = null` vì icon chỉ trang trí, tiêu đề bên cạnh đã đọc được bằng trình đọc màn hình.

## Kiểm tra

- [ ] Không còn chuỗi chữ cứng nào trong `SensorCard.kt` và `DashboardScreen.kt`.
- [ ] Android: đổi ngôn ngữ máy sang Tiếng Việt → thẻ hiện "Pin · Đang sạc"; về English → "Battery · Charging".
- [ ] iOS: Settings → General → Language & Region thêm Tiếng Việt lên đầu → app đổi chữ theo (Simulator cũng được).
- [ ] Mỗi thẻ có icon riêng trên cả hai nền tảng; `distance(1530.0)` hiện `1.5 km`, `distance(42.0)` hiện `42 m`.

## Lỗi hay gặp

- `Res` không có chuỗi / ảnh mới → chưa build lại để sinh code.
- Import sai package `Res` → để IDE tự gợi ý import, đừng gõ tay.
- iOS không đổi sang tiếng Việt → thêm `vi` vào `CFBundleLocalizations` trong `iosApp/iosApp/Info.plist`. Xem thêm [xử lý sự cố](../reference/troubleshooting.md).

## Tiếp theo?

- [Bài 8: ViewModel và điều hướng](08-viewmodel-navigation.md).
- Đọc thêm: [Regional format](https://kotlinlang.org/docs/multiplatform/compose-regional-format.html) · [RTL languages](https://kotlinlang.org/docs/multiplatform/compose-rtl.html) · [Localization tests](https://kotlinlang.org/docs/multiplatform/compose-localization-tests.html).

## Docs gốc

[Resources overview](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources.html) · [Setup](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-setup.html) · [Usage](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-usage.html) · [Localizing strings](https://kotlinlang.org/docs/multiplatform/compose-localize-strings.html)
