# 8. Gradle & build (K351–K400)

Lỗi khi Sync, khi `./gradlew` chạy từ terminal, khi nâng phiên bản Kotlin / AGP / Compose. Người mới gặp ngay ngày đầu
(JDK sai, chưa sync), người đã làm app gặp khi thêm thư viện hoặc nâng cấp.

### K351 · Sync báo "Incompatible Gradle JVM"

`🟢 mới học` · `Gradle`

**Gặp:** Sync dừng ngay, Android Studio báo Gradle JVM không tương thích (Incompatible Gradle JVM), hoặc
`Unsupported class file major version 69`.

**Vì sao:** Gradle đang chạy bằng JDK quá cũ (11) hoặc quá mới (JBR 25 đi kèm Studio). Gradle 8.14 chỉ chạy được tới Java 24,
còn AGP 8.x cần ít nhất Java 17.

**Sửa:** ghim JDK cho Gradle daemon bằng file `gradle/gradle-daemon-jvm.properties`, commit file đó. Gradle tự tìm JDK 21 trên máy,
không còn phụ thuộc setting Gradle JDK của từng người.

```bash
# tạo file gradle/gradle-daemon-jvm.properties
./gradlew updateDaemonJvm --jvm-version=21
cat gradle/gradle-daemon-jvm.properties   # toolchainVersion=21
```

**Gặp ở:** [`DevicePulse/gradle/gradle-daemon-jvm.properties`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/DevicePulse/gradle/gradle-daemon-jvm.properties) ·
Xem thêm: [Lỗi hay gặp](../reference/troubleshooting.md), K352

### K352 · Studio build được, `./gradlew` trong terminal thì không

`🟢 mới học` · `Gradle`

**Gặp:** `Android Gradle plugin requires Java 17 to run. You are currently using Java 11.`

**Vì sao:** Studio dùng JDK riêng, còn terminal dùng `JAVA_HOME` (hoặc `java` đầu tiên trong `PATH`), thường là JDK cũ cài từ lâu.

**Sửa:** trỏ `JAVA_HOME` tới JDK 17–24, hoặc ghim daemon JVM như K351 để cả hai cùng dùng một JDK.

```bash
/usr/libexec/java_home -V                       # xem các JDK trên máy
export JAVA_HOME=$(/usr/libexec/java_home -v 21) # ✅ thêm vào ~/.zshrc
./gradlew --version                             # dòng "Launcher JVM" / "Daemon JVM"
```

### K353 · `permission denied: ./gradlew`

`🟢 mới học` · `Gradle`

**Gặp:** zsh báo `permission denied: ./gradlew` khi chạy lần đầu sau khi tải zip hoặc copy project.

**Vì sao:** file `gradlew` mất quyền thực thi (giải nén zip, copy qua ổ khác, tạo trên Windows).

**Sửa:** cấp lại quyền, và lưu quyền đó vào git để người khác không gặp.

```bash
chmod +x gradlew
git update-index --chmod=+x gradlew
```

### K354 · Gõ `gradle build` thay vì `./gradlew build`

`🟢 mới học` · `Gradle`

**Gặp:** build lỗi lạ (plugin không hợp, DSL không có) trong khi Studio build bình thường.

**Vì sao:** `gradle` là bản Gradle cài trên máy (Homebrew…), phiên bản bất kỳ. `./gradlew` tải và dùng đúng phiên bản ghi trong
`gradle/wrapper/gradle-wrapper.properties` (repo này: 8.14.5).

**Sửa:** luôn chạy `./gradlew` ở thư mục gốc của project.

```bash
gradle assembleDebug      # ❌ Gradle của máy, không biết phiên bản nào
./gradlew assembleDebug   # ✅ Gradle 8.14.5 của project
```

### K355 · Sửa tay `distributionUrl` để nâng Gradle

`🟡 đã làm app` · `Gradle`

**Gặp:** nâng Gradle xong, `gradlew` / `gradle-wrapper.jar` vẫn là bản cũ; máy khác chạy ra hành vi khác.

**Vì sao:** sửa tay chỉ đổi một dòng. Task `wrapper` cập nhật cả file properties, script `gradlew` và file jar.

**Sửa:** nâng bằng task `wrapper`, chạy hai lần (lần sau chạy bằng Gradle mới để sinh lại script), rồi commit cả bốn file.

```bash
./gradlew wrapper --gradle-version 8.14.5
./gradlew wrapper
git add gradlew gradlew.bat gradle/wrapper/
```

### K356 · `libs.kotlinx_coroutines_core` không có

`🟢 mới học` · `Gradle`

**Gặp:** `Unresolved reference` khi gọi alias của version catalog trong `build.gradle.kts`.

**Vì sao:** trong `libs.versions.toml`, dấu `-` `_` `.` trong alias đều thành dấu chấm khi dùng: `kotlinx-coroutines-core` →
`libs.kotlinx.coroutines.core`.

**Sửa:** đổi gạch thành chấm. Plugin nằm dưới `libs.plugins`, phiên bản dưới `libs.versions`.

```kotlin
implementation(libs.kotlinx_coroutines_core)   // ❌
implementation(libs.kotlinx.coroutines.core)   // ✅
alias(libs.plugins.kotlinMultiplatform)        // ✅ plugin
```

### K357 · Thêm dòng vào `libs.versions.toml` mà `libs.xxx` vẫn đỏ

`🟢 mới học` · `Gradle`

**Gặp:** `Unresolved reference` cho alias vừa thêm, dù viết đúng tên.

**Vì sao:** accessor `libs.*` được Gradle sinh ra lúc Sync. Chưa Sync thì IDE chưa biết alias mới.

**Sửa:** bấm **Sync Now** (hoặc chạy `./gradlew help`) sau mỗi lần sửa catalog. Lỗi vẫn còn thì kiểm alias nằm đúng bảng
`[libraries]` / `[plugins]`.

### K358 · `version.ref` trỏ tới key không có

`🟡 đã làm app` · `Gradle`

**Gặp:** Sync lỗi ngay khi đọc catalog, báo một version reference không tồn tại.

**Vì sao:** `version.ref` phải trùng tuyệt đối với một key trong `[versions]`, kể cả dấu gạch. Gõ `kotlin-coroutines` trong khi key là
`kotlinx-coroutines` là sai.

**Sửa:** copy key từ `[versions]` sang, đừng gõ lại.

```toml
[versions]
kotlinx-coroutines = "1.11.0"
[libraries]
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlin-coroutines" }  # ❌
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" } # ✅
```

### K359 · "The Kotlin Gradle plugin was loaded multiple times"

`🟡 đã làm app` · `Gradle`

**Gặp:** cảnh báo `The Kotlin Gradle plugin was loaded multiple times in different subprojects, which is not supported and may
break the build`, sau đó build lỗi khó hiểu giữa các module.

**Vì sao:** mỗi module tự nạp plugin (kèm phiên bản) bằng classloader riêng. Có module dùng bản khác nhau là xung đột.

**Sửa:** khai báo mọi plugin một lần ở `build.gradle.kts` gốc với `apply false`, các module chỉ `alias(...)` không phiên bản.

```kotlin
// build.gradle.kts (gốc)
plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidLibrary) apply false
}
```

**Gặp ở:** [`RateWatch/build.gradle.kts`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/RateWatch/build.gradle.kts)

### K360 · Plugin kotlinx.serialization khác phiên bản Kotlin

`🟡 đã làm app` · `Gradle`

**Gặp:** sau khi nâng Kotlin, build lỗi ở plugin serialization hoặc `@Serializable` không sinh serializer.

**Vì sao:** `org.jetbrains.kotlin.plugin.serialization` là compiler plugin, đi cùng phiên bản Kotlin. Thư viện runtime
`kotlinx-serialization-json` (1.11.0) thì có phiên bản riêng; hai số này khác nhau là bình thường.

**Sửa:** cho plugin dùng `version.ref = "kotlin"`, không đặt số riêng.

```toml
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version = "1.11.0" }      # ❌ số của thư viện
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }  # ✅
```

**Gặp ở:** [`RateWatch/gradle/libs.versions.toml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/RateWatch/gradle/libs.versions.toml)

### K361 · Nâng Kotlin, quên nâng SKIE

`🔴 khó, hiếm nhưng đau` · `Gradle`

**Gặp:** Android build bình thường, nhưng task build framework iOS lỗi ở plugin SKIE.

**Vì sao:** SKIE can thiệp vào compiler Kotlin/Native nên mỗi bản SKIE chỉ hỗ trợ một số phiên bản Kotlin. Repo này dùng SKIE
0.10.15, bản đầu tiên chạy với Kotlin 2.4.20.

**Sửa:** mỗi lần nâng Kotlin, đọc release notes của SKIE và nâng theo. Chưa có bản SKIE hợp thì giữ Kotlin cũ.

```toml
kotlin = "2.4.20"
skie = "0.10.15"   # bản đầu tiên cho Kotlin 2.4.20, đổi Kotlin là phải xem lại dòng này
```

**Gặp ở:** [`TipSplit/gradle/libs.versions.toml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TipSplit/gradle/libs.versions.toml)

### K362 · Kotlin 2.x: "the Compose Compiler Gradle plugin is required"

`🟢 mới học` · `Gradle` · `Compose`

**Gặp:** `Starting in Kotlin 2.0, the Compose Compiler Gradle plugin is required when compose is enabled.`

**Vì sao:** từ Kotlin 2.0, compiler Compose nằm trong repo Kotlin và đi kèm plugin `org.jetbrains.kotlin.plugin.compose`.
Cách cũ `composeOptions { kotlinCompilerExtensionVersion = … }` không còn dùng.

**Sửa:** thêm plugin compose (cùng phiên bản Kotlin) vào module có `buildFeatures { compose = true }`, xoá `composeOptions`.

```kotlin
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)   // ✅ id org.jetbrains.kotlin.plugin.compose, version.ref = "kotlin"
}
```

### K363 · Nâng Compose BOM thì đòi compileSdk cao hơn

`🔴 khó, hiếm nhưng đau` · `Gradle` · `Compose`

**Gặp:** task `checkDebugAarMetadata` lỗi: `Dependency '…' requires libraries and applications that depend on it to compile against
version … or later of the Android APIs.`

**Vì sao:** thư viện AndroidX mới ghi `minCompileSdk` trong AAR. BOM mới kéo theo Compose mới, Compose mới cần compileSdk mới,
compileSdk mới lại cần AGP mới.

**Sửa:** hoặc nâng compileSdk + AGP cùng lúc, hoặc giữ BOM. Repo này giữ BOM 2026.06.01 (UI 1.11.4, Material3 1.4.0), bản mới
nhất còn build với compileSdk 36 + AGP 8.13.

```toml
android-compileSdk = "36"
compose-bom = "2026.06.01"   # bản mới nhất build được với compileSdk 36
```

**Gặp ở:** [`DevicePulse/gradle/libs.versions.toml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/DevicePulse/gradle/libs.versions.toml)

### K364 · Lẫn `compileSdk`, `targetSdk`, `minSdk`

`🟢 mới học` · `Gradle` · `Android`

**Gặp:** nâng `targetSdk` để "dùng được API mới" rồi app đổi hành vi (quyền, foreground service, edge-to-edge…); hoặc gọi API mới
mà vẫn báo không có.

**Vì sao:** `compileSdk` = API nào compiler thấy. `targetSdk` = app hứa đã chạy đúng với luật của phiên bản đó, hệ điều hành bật
hành vi mới theo số này. `minSdk` = máy cũ nhất cài được.

**Sửa:** muốn thấy API mới thì nâng `compileSdk`. Nâng `targetSdk` chỉ khi đã đọc thay đổi hành vi của phiên bản đó và test lại.

```kotlin
compileSdk = 36   // API compiler thấy
minSdk = 24       // máy cũ nhất
targetSdk = 36    // luật hành vi app chấp nhận
```

### K365 · AGP 9: `com.android.library` không dùng chung với KMP được nữa

`🔴 khó, hiếm nhưng đau` · `Gradle` · `KMP`

**Gặp:** nâng AGP lên 9, module `shared` (có `kotlinMultiplatform` + `androidLibrary` + `androidTarget()`) không build được nữa.

**Vì sao:** AGP 9 bỏ cách gắn plugin Android library cũ vào module KMP. Module KMP phải dùng plugin riêng
`com.android.kotlin.multiplatform.library`, cấu hình Android nằm trong khối `kotlin { }`.

**Sửa:** muốn ở lại thì giữ AGP 8.13 (repo này đang làm vậy). Muốn lên AGP 9 thì đổi plugin của `shared/` theo hướng dẫn migrate
chính chủ, đừng chỉ nâng số phiên bản.

```toml
# AGP 8.13 + compileSdk 36 still allow androidTarget() + com.android.library in the shared module.
# Moving to AGP 9 means switching shared/ to com.android.kotlin.multiplatform.library.
agp = "8.13.2"
```

**Gặp ở:** [`DevicePulse/gradle/libs.versions.toml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/DevicePulse/gradle/libs.versions.toml)

### K366 · Android Studio cũ không mở được project dùng AGP mới

`🟡 đã làm app` · `Gradle`

**Gặp:** Sync báo `The project is using an incompatible version (AGP …) of the Android Gradle plugin. Latest supported version is AGP …`

**Vì sao:** mỗi bản Android Studio chỉ hỗ trợ AGP tới một mốc. Project được nâng AGP trên máy khác, máy bạn Studio cũ hơn.

**Sửa:** cập nhật Android Studio. Không được thì hạ `agp` trong catalog về bản Studio hỗ trợ (và Gradle tương ứng).

### K367 · "Minimum supported Gradle version is …"

`🟡 đã làm app` · `Gradle`

**Gặp:** `Minimum supported Gradle version is 8.13. Current version is 8.9.`

**Vì sao:** mỗi AGP đòi Gradle tối thiểu. AGP 8.13 cần Gradle 8.13 trở lên; nâng AGP mà quên nâng wrapper là gặp.

**Sửa:** nâng wrapper bằng task `wrapper` (K355) lên bản lỗi gợi ý hoặc mới hơn.

```bash
./gradlew wrapper --gradle-version 8.14.5
```

### K368 · Module app không thấy class của module con con

`🟢 mới học` · `Gradle`

**Gặp:** `androidApp` báo `Unresolved reference: Dtc` (hoặc "Cannot access class") dù `shared` dùng `Dtc` trong API public của nó.

**Vì sao:** `implementation(project(":core:obd"))` giữ `:core:obd` làm chi tiết bên trong của `shared`. Module dùng `shared` không
thấy nó. Khi kiểu của module con nằm trong API public (property, tham số, kiểu trả về) thì phải dùng `api`.

**Sửa:** đổi sang `api(...)` cho đúng module lộ ra ngoài; còn lại giữ `implementation`.

```kotlin
commonMain.dependencies {
    api(project(":core:obd"))             // ✅ ObdState lộ Dtc, Dongle… ra cả hai UI
    implementation(project(":core:ble"))  // ✅ chỉ dùng bên trong
}
```

**Gặp ở:** [`ObdLink/shared/build.gradle.kts`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/ObdLink/shared/build.gradle.kts)

### K369 · Dùng `api` cho mọi thứ "cho chắc"

`🟡 đã làm app` · `Gradle`

**Gặp:** sửa một dòng trong module thấp nhất mà cả project compile lại; build ngày càng chậm.

**Vì sao:** `api` làm thư viện thành một phần ABI của module. Thư viện đổi thì mọi module phía trên phải compile lại, và module trên
có thể vô tình dùng thứ nó không khai báo.

**Sửa:** mặc định `implementation`. Chỉ `api` khi kiểu của thư viện xuất hiện trong khai báo public (như `koin-core` cho
`initKoin(config: KoinAppDeclaration)`).

### K370 · `Could not find …` dù thư viện có trên Maven

`🟡 đã làm app` · `Gradle`

**Gặp:** `Could not find com.example:lib:1.0.` dù tìm trên Maven Central thấy có.

**Vì sao:** repository khai báo thiếu, hoặc có bộ lọc. Repo này lọc `google()` chỉ cho nhóm `androidx`, `com.android`,
`com.google`; thư viện thuộc nhóm khác chỉ được tìm ở `mavenCentral()`.

**Sửa:** kiểm thư viện nằm ở repository nào rồi thêm repository đó (hoặc thêm nhóm vào bộ lọc) trong `settings.gradle.kts`.

```kotlin
dependencyResolutionManagement {
    repositories {
        google { mavenContent { includeGroupAndSubgroups("androidx") /* … */ } }
        mavenCentral()   // ✅ mọi thứ còn lại
    }
}
```

**Gặp ở:** [`ObdLink/settings.gradle.kts`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/ObdLink/settings.gradle.kts)

### K371 · Thêm `repositories { }` vào module thì build lỗi

`🟢 mới học` · `Gradle`

**Gặp:** `Build was configured to prefer settings repositories over project repositories but repository '…' was added by build file …`

**Vì sao:** project đặt `repositoriesMode` là `FAIL_ON_PROJECT_REPOS` (mẫu Android Studio hay sinh): mọi repository phải khai báo ở
`settings.gradle.kts`.

**Sửa:** xoá khối `repositories` trong `build.gradle.kts` của module, chuyển repository đó vào `dependencyResolutionManagement`.

### K372 · `Could not resolve all files for configuration`

`🟢 mới học` · `Gradle`

**Gặp:** `Could not resolve all files for configuration ':androidApp:debugRuntimeClasspath'.` kèm `Could not find …` bên dưới.

**Vì sao:** dòng đầu chỉ là tóm tắt. Lý do thật ở dòng `Could not find` / `Could not resolve`: gõ sai group/artifact, phiên bản không
tồn tại, thiếu repository hoặc đang offline.

**Sửa:** đọc dòng `Could not find` đầu tiên, so tên với trang Maven của thư viện, kiểm repository (K370) và chế độ offline (K386).

### K373 · Catalog ghi 1.10 mà app chạy bản 1.11

`🟡 đã làm app` · `Gradle`

**Gặp:** đã khai báo `kotlinx-serialization-json` một phiên bản, nhưng lúc chạy lại là bản khác, hoặc có API "không tồn tại".

**Vì sao:** Gradle gom mọi phiên bản được yêu cầu (trực tiếp + qua thư viện khác như Ktor) và chọn bản **cao nhất**. Số trong catalog
chỉ là một lời yêu cầu.

**Sửa:** xem bản thật bằng `dependencyInsight`; chọn phiên bản trùng với bản thư viện lớn đang kéo theo, như RateWatch chọn
kotlinx.serialization 1.11.0 vì Ktor 3.5.1 dùng đúng bản đó.

```bash
./gradlew :androidApp:dependencyInsight --dependency kotlinx-serialization-json --configuration debugRuntimeClasspath
```

**Gặp ở:** [`RateWatch/gradle/libs.versions.toml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/RateWatch/gradle/libs.versions.toml)

### K374 · "Duplicate class … found in modules"

`🔴 khó, hiếm nhưng đau` · `Gradle` · `Android`

**Gặp:** task `checkDebugDuplicateClasses` lỗi: `Duplicate class X found in modules A and B`.

**Vì sao:** cùng một class nằm trong hai artifact khác nhau: thư viện đổi tên artifact giữa các bản, hoặc một bản "all-in-one" đi
cùng bản tách nhỏ.

**Sửa:** tìm ai kéo artifact thừa bằng `./gradlew :androidApp:dependencies`, rồi nâng thư viện đó hoặc `exclude` artifact cũ.

```kotlin
implementation(libs.some.lib) {
    exclude(group = "com.old", module = "old-artifact")   // ✅ bỏ bản trùng
}
```

### K375 · `Could not find androidx.compose.ui:ui:.`

`🟢 mới học` · `Gradle` · `Compose`

**Gặp:** `Could not find androidx.compose.ui:ui:.` (không có số phiên bản sau dấu `:`).

**Vì sao:** thư viện Compose khai báo không phiên bản, chờ BOM cấp. Quên dòng `platform(libs.compose.bom)` thì không ai cấp số.

**Sửa:** thêm BOM bằng `platform(...)`. Mọi thư viện `androidx.compose.*` khác để trống phiên bản.

```kotlin
implementation(platform(libs.compose.bom))   // ✅ cấp phiên bản cho cả nhóm
implementation(libs.compose.ui)
implementation(libs.compose.material3)
```

**Gặp ở:** [`ObdLink/androidApp/build.gradle.kts`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/ObdLink/androidApp/build.gradle.kts)

### K376 · Copy cú pháp Groovy vào `.kts`

`🟢 mới học` · `Gradle`

**Gặp:** `build.gradle.kts` đỏ cả dòng copy từ StackOverflow: `implementation 'a:b:1.0'`.

**Vì sao:** `.kts` là Kotlin: gọi hàm phải có ngoặc, chuỗi phải dùng nháy kép. Groovy thì không cần.

**Sửa:** thêm ngoặc và nháy kép, tốt hơn là đưa vào version catalog.

```kotlin
implementation 'io.ktor:ktor-client-core:3.5.1'    // ❌ Groovy
implementation("io.ktor:ktor-client-core:3.5.1")   // ✅ KTS
implementation(libs.ktor.client.core)              // ✅ tốt hơn
```

### K377 · `minSdk 24`, `minifyEnabled true` trong `.kts`

`🟢 mới học` · `Gradle`

**Gặp:** `Unresolved reference: minifyEnabled` hoặc lỗi cú pháp ở các dòng gán trong khối `android { }`.

**Vì sao:** KTS gán property bằng `=`. Property Boolean thường có tiền tố `is`.

**Sửa:**

```kotlin
minSdk 24                 // ❌ Groovy
minSdk = 24               // ✅
minifyEnabled true        // ❌
isMinifyEnabled = true    // ✅
```

### K378 · `compileSdk = libs.versions.android.compileSdk` báo type mismatch

`🟢 mới học` · `Gradle`

**Gặp:** `Type mismatch: inferred type is Provider<String> but Int was expected`.

**Vì sao:** `libs.versions.x` là một `Provider<String>` (giá trị lười), không phải số.

**Sửa:** lấy giá trị rồi đổi kiểu.

```kotlin
compileSdk = libs.versions.android.compileSdk                    // ❌
compileSdk = libs.versions.android.compileSdk.get().toInt()      // ✅
```

**Gặp ở:** [`ObdLink/androidApp/build.gradle.kts`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/ObdLink/androidApp/build.gradle.kts)

### K379 · "Inconsistent JVM-target compatibility"

`🟡 đã làm app` · `Gradle` · `Android`

**Gặp:** `Inconsistent JVM-target compatibility detected for tasks 'compileDebugJavaWithJavac' (11) and 'compileDebugKotlin' (17).`

**Vì sao:** Java và Kotlin trong cùng module được biên dịch ra hai mức bytecode khác nhau.

**Sửa:** đặt hai chỗ cùng một số. Repo này dùng 11 ở cả `compileOptions` và `compilerOptions`.

```kotlin
android { compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 } }
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }
```

**Gặp ở:** [`ObdLink/androidApp/build.gradle.kts`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/ObdLink/androidApp/build.gradle.kts)

### K380 · Build hết RAM: `OutOfMemoryError`

`🔴 khó, hiếm nhưng đau` · `Gradle`

**Gặp:** `java.lang.OutOfMemoryError: Java heap space` (hoặc `Metaspace`), hay gặp ở bước link framework iOS hoặc lần build đầu.

**Vì sao:** heap mặc định của Gradle daemon và Kotlin daemon nhỏ so với một project KMP có Kotlin/Native.

**Sửa:** tăng bộ nhớ trong `gradle.properties`, rồi `./gradlew --stop` để daemon mới nhận cấu hình.

```properties
org.gradle.jvmargs=-Xmx4096M -Dfile.encoding=UTF-8
kotlin.daemon.jvmargs=-Xmx3072M
```

**Gặp ở:** [`DevicePulse/gradle.properties`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/DevicePulse/gradle.properties)

### K381 · Lần nào build cũng compile lại từ đầu

`🟡 đã làm app` · `Gradle`

**Gặp:** sau `./gradlew clean` hoặc đổi nhánh git, build mất nhiều phút dù code gần như không đổi.

**Vì sao:** build cache tắt mặc định. Không có cache thì kết quả task cũ bị bỏ, task chạy lại hết.

**Sửa:** bật build cache. Gradle lấy lại output của task có cùng input thay vì chạy lại.

```properties
org.gradle.caching=true
```

**Gặp ở:** [`DevicePulse/gradle.properties`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/DevicePulse/gradle.properties)

### K382 · Bật configuration cache rồi build lỗi

`🔴 khó, hiếm nhưng đau` · `Gradle`

**Gặp:** `Configuration cache problems found in this build.` kèm đường dẫn tới file report HTML.

**Vì sao:** configuration cache lưu lại kết quả bước cấu hình. Script hay plugin nào đọc `project` lúc task chạy, hoặc giữ object không
lưu được, sẽ vi phạm.

**Sửa:** mở report để xem dòng nào vi phạm, sửa script của mình. Nếu lỗi nằm trong plugin của bên thứ ba thì tạm tắt
`org.gradle.configuration-cache` tới khi plugin hỗ trợ.

### K383 · Cache hỏng sau khi tải dở

`🔴 khó, hiếm nhưng đau` · `Gradle`

**Gặp:** lỗi đọc file jar/zip trong `~/.gradle/caches` (kiểu `zip END header not found`), lặp lại ở mọi lần build.

**Vì sao:** mạng rớt hoặc tắt máy khi đang tải, file trong cache bị cụt. Gradle tưởng đã có nên không tải lại.

**Sửa:** dừng daemon, xoá đúng thư mục bị hỏng (đường dẫn có trong lỗi) rồi build lại. Bí quá thì thêm `--refresh-dependencies`.

```bash
./gradlew --stop
rm -rf ~/.gradle/caches/modules-2/files-2.1/<group-bị-hỏng>
./gradlew build --refresh-dependencies
```

### K384 · IDE báo đỏ nhưng `./gradlew build` xanh

`🟢 mới học` · `Gradle`

**Gặp:** Android Studio gạch đỏ import / class, nhưng build bằng Gradle chạy bình thường.

**Vì sao:** index của IDE cũ hơn trạng thái Gradle (chưa Sync, Sync bị huỷ giữa chừng, đổi nhánh).

**Sửa:** Sync lại. Vẫn đỏ thì **File › Invalidate Caches…** rồi khởi động lại. Tin kết quả của `./gradlew`, không tin màu đỏ của IDE.

### K385 · "SDK location not found"

`🟢 mới học` · `Gradle` · `Android`

**Gặp:** `SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local properties file`

**Vì sao:** clone project về chưa mở bằng Studio lần nào, nên chưa có `local.properties`. File này không commit vì đường dẫn mỗi máy khác.

**Sửa:** mở project bằng Android Studio một lần (nó tự tạo), hoặc tự viết file.

```properties
# local.properties (không commit)
sdk.dir=/Users/<you>/Library/Android/sdk
```

### K386 · "No cached version … available for offline mode"

`🟢 mới học` · `Gradle`

**Gặp:** `No cached version of … available for offline mode.`

**Vì sao:** Gradle đang ở chế độ offline (nút trong cửa sổ Gradle của Studio, hoặc cờ `--offline`), mà thư viện mới chưa có trong cache.

**Sửa:** tắt offline mode, Sync lại khi có mạng.

### K387 · "N incompatible Daemons could not be reused"

`🔴 khó, hiếm nhưng đau` · `Gradle`

**Gặp:** mỗi lần chạy lại thấy `Starting a Gradle Daemon, 1 incompatible … Daemons could not be reused`, máy nóng, RAM đầy.

**Vì sao:** Studio và terminal chạy Gradle bằng JDK khác nhau hoặc `jvmargs` khác nhau, nên mỗi bên tạo daemon riêng, daemon cũ
không dùng lại được.

**Sửa:** cho mọi nơi dùng cùng JDK (ghim daemon JVM như K351), rồi dọn daemon cũ.

```bash
./gradlew --status   # xem các daemon và lý do không dùng lại
./gradlew --stop
```

### K388 · "Timeout waiting to lock … in use by another Gradle instance"

`🟢 mới học` · `Gradle`

**Gặp:** `Timeout waiting to lock … It is currently in use by another Gradle instance.`

**Vì sao:** Studio đang Sync (hoặc một lệnh `./gradlew` khác đang chạy) và giữ khoá cache.

**Sửa:** chờ Sync xong rồi chạy lại. Nếu không có gì đang chạy mà vẫn kẹt, `./gradlew --stop` để giải phóng khoá.

### K389 · `./gradlew :installDebug` báo không tìm thấy task

`🟢 mới học` · `Gradle`

**Gặp:** `Cannot locate tasks that match ':installDebug' as task 'installDebug' not found in root project …`

**Vì sao:** `:installDebug` nghĩa là task của project gốc, mà project gốc không phải app. Task này nằm ở module `androidApp`.

**Sửa:** ghi đủ đường dẫn module. `./gradlew tasks --all` liệt kê task của từng module.

```bash
./gradlew :installDebug              # ❌ project gốc
./gradlew :androidApp:installDebug   # ✅
```

### K390 · "Project with path ':core:obd' could not be found"

`🟢 mới học` · `Gradle`

**Gặp:** `Project with path ':core:obd' could not be found in project ':shared'.`

**Vì sao:** module chưa được `include` trong `settings.gradle.kts`, hoặc đường dẫn không khớp thư mục (`:core:obd` = thư mục `core/obd`).

**Sửa:** thêm `include`, rồi Sync.

```kotlin
// settings.gradle.kts
include(":core:obd")
include(":core:ble")
include(":shared")
```

**Gặp ở:** [`ObdLink/settings.gradle.kts`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/ObdLink/settings.gradle.kts)

### K391 · "Plugin [id: …] was not found in any of the following sources"

`🟢 mới học` · `Gradle`

**Gặp:** `Plugin [id: 'co.touchlab.skie', version: '0.10.15'] was not found in any of the following sources:`

**Vì sao:** gõ sai id/phiên bản, hoặc `pluginManagement.repositories` thiếu nơi chứa plugin (nhiều plugin chỉ có trên
`gradlePluginPortal()`).

**Sửa:** so id + phiên bản với trang của plugin, và giữ đủ ba repository cho plugin.

```kotlin
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
```

### K392 · "Namespace not specified"

`🟡 đã làm app` · `Gradle` · `Android`

**Gặp:** `Namespace not specified. Specify a namespace in the module's build file.`

**Vì sao:** từ AGP 8, `package` trong `AndroidManifest.xml` không còn dùng; mỗi module Android (kể cả `shared`) phải có `namespace`.

**Sửa:** thêm `namespace` vào khối `android { }` của module, xoá `package` khỏi manifest.

```kotlin
android {
    namespace = "dev.obdlink.shared"
}
```

**Gặp ở:** [`ObdLink/shared/build.gradle.kts`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/ObdLink/shared/build.gradle.kts)

### K393 · `R.string.x` của module khác không còn thấy

`🟡 đã làm app` · `Gradle` · `Android`

**Gặp:** `Unresolved reference` khi dùng `R.string.xxx` khai báo ở một module thư viện, qua class `R` của app.

**Vì sao:** `android.nonTransitiveRClass=true` (mặc định từ AGP 8) làm `R` của mỗi module chỉ chứa resource của chính nó.

**Sửa:** import `R` của module chứa resource, hoặc dùng tên đầy đủ.

```kotlin
import dev.lib.R as LibR
Text(stringResource(LibR.string.title))   // ✅
```

**Gặp ở:** [`DevicePulse/gradle.properties`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/DevicePulse/gradle.properties)

### K394 · `kotlinOptions { jvmTarget = "11" }` không còn dùng được

`🟡 đã làm app` · `Gradle`

**Gặp:** script copy từ hướng dẫn cũ báo `kotlinOptions` đã bị deprecated (ở Kotlin 2.x là lỗi, không chỉ cảnh báo).

**Vì sao:** Kotlin thay `kotlinOptions` bằng DSL `compilerOptions`, giá trị kiểu enum thay vì chuỗi.

**Sửa:**

```kotlin
android { kotlinOptions { jvmTarget = "11" } }              // ❌ cũ
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } } // ✅
```

**Gặp ở:** [`ObdLink/androidApp/build.gradle.kts`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/ObdLink/androidApp/build.gradle.kts)

### K395 · `Unresolved reference: BuildConfig`

`🟡 đã làm app` · `Gradle` · `Android`

**Gặp:** `BuildConfig.DEBUG`, `BuildConfig.VERSION_NAME` đỏ trong module mới tạo.

**Vì sao:** từ AGP 8, class `BuildConfig` không được sinh mặc định nữa.

**Sửa:** bật lại trong module cần nó.

```kotlin
android {
    buildFeatures { buildConfig = true }
}
```

### K396 · Thiếu `android.useAndroidX=true`

`🟢 mới học` · `Gradle` · `Android`

**Gặp:** build báo project dùng thư viện AndroidX nhưng property `android.useAndroidX` chưa bật.

**Vì sao:** `gradle.properties` bị xoá hoặc copy thiếu dòng này. AGP cần nó để biết project đã chuyển sang AndroidX.

**Sửa:** thêm vào `gradle.properties` ở thư mục gốc.

```properties
android.useAndroidX=true
```

**Gặp ở:** [`DevicePulse/gradle.properties`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/DevicePulse/gradle.properties)

### K397 · Lần build iOS đầu tiên rất lâu, hoặc lỗi trong `~/.konan`

`🔴 khó, hiếm nhưng đau` · `Gradle` · `KMP`

**Gặp:** lần đầu build target iOS đứng lâu ở bước tải; hoặc sau khi mạng rớt, mọi lần build iOS đều lỗi trong thư mục `~/.konan`.

**Vì sao:** Kotlin/Native tải toolchain (vài trăm MB tới hơn 1 GB) vào `~/.konan` lần đầu. Tải dở là cache hỏng.

**Sửa:** lần đầu cứ để chạy. Nếu hỏng thì dừng daemon, xoá thư mục bản tải dở trong `~/.konan` (hoặc cả `~/.konan`) rồi build lại.

```bash
./gradlew --stop
rm -rf ~/.konan
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

### K398 · Build iOS chậm vì build mọi target

`🟡 đã làm app` · `Gradle` · `KMP`

**Gặp:** `./gradlew build` mất rất lâu, phần lớn thời gian ở các task `link…Ios…`.

**Vì sao:** `build` link framework cho mọi target iOS × Debug/Release. Khi phát triển chỉ cần một.

**Sửa:** chạy đúng task cần: compile để kiểm lỗi, link simulator Debug để chạy thử.

```bash
./gradlew :shared:compileKotlinIosSimulatorArm64          # kiểm code iOS compile được
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64     # framework cho simulator
```

### K399 · Chỉ đọc dòng cuối của lỗi Gradle

`🟢 mới học` · `Gradle`

**Gặp:** cuối log là `BUILD FAILED` và `Execution failed for task …`, không rõ vì sao.

**Vì sao:** lý do thật nằm trong phần `* What went wrong:` và dòng `e:` (lỗi compiler) đầu tiên, phía trên.

**Sửa:** cuộn lên tìm `What went wrong` và dòng `e:` đầu tiên. Cần thêm thì chạy lại với `--stacktrace` hoặc `--info`.

```bash
./gradlew :shared:compileKotlinIosSimulatorArm64 --stacktrace
```

### K400 · "No matching toolchains found for requested specification"

`🔴 khó, hiếm nhưng đau` · `Gradle`

**Gặp:** `No matching toolchains found for requested specification: {languageVersion=17, …}`

**Vì sao:** script có `kotlin { jvmToolchain(17) }` (hoặc `java.toolchain`), nghĩa là "compile bằng đúng JDK 17", mà máy không có
JDK 17 và Gradle không được phép tự tải.

**Sửa:** cài JDK đó, hoặc thêm plugin cho Gradle tự tải toolchain vào `settings.gradle.kts`. Không cần ghim toolchain thì xoá dòng
`jvmToolchain` và chỉ đặt `jvmTarget` (K379).

```kotlin
// settings.gradle.kts
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "<bản mới nhất>"
}
```

---

**Nhóm trước / sau:** [7. Flow, StateFlow, SharedFlow](07-flow.md) · [9. KMP: source set, expect/actual](09-kmp.md) · [Mục lục 900 lỗi](index.md)
