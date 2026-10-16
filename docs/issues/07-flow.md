# 7. Flow, StateFlow, SharedFlow (K301–K350)

Flow là "dòng giá trị theo thời gian": pin, GPS, danh sách ghi chú, trạng thái màn hình. Lỗi ở đây ít khi crash ngay, mà là
UI không cập nhật, sự kiện mất hoặc lặp, cảm biến chạy mãi không tắt. Hầu hết mục có **▶ Chạy thử** để thấy tận mắt.

### K301 · Tạo `flow { }` rồi mà không có gì chạy

`🟢 mới học` · `Kotlin`

**Gặp:** viết `val prices = flow { … gọi API … }`, thêm `.map { }`, nhưng log trong khối `flow { }` không bao giờ in ra.

**Vì sao:** Flow là **lạnh**: `flow { }` và mọi toán tử `map`, `filter`… chỉ là công thức. Chưa có ai gọi hàm cuối (terminal) như
`collect`, `first`, `toList` thì không dòng nào chạy.

**Sửa:** gọi `collect` (trong coroutine), hoặc `launchIn(scope)` nếu muốn chạy nền.

```kotlin
val prices = flow { emit(api.latest()) }
prices.map { it * 2 }                 // ❌ chưa chạy gì
prices.collect { println(it) }        // ✅ giờ mới gọi API
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMwMTogdOG6oW8gZmxvdyBjaMawYSBsw6BtIGfDrCBj4bqjLiBDaOG7iSBraGkgY29sbGVjdCB0aMOsIGto4buRaSBmbG93IHsgfSBt4bubaSBjaOG6oXkuCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuKgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLmZsb3cuKgoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nPFVuaXQ-IHsKICAgIHZhbCBwcmljZXMgPSBmbG93IHsKICAgICAgICBwcmludGxuKCIgIChn4buNaSBt4bqhbmcgbOG6pXkgZ2nDoSkiKQogICAgICAgIGVtaXQoMTAwKQogICAgfQogICAgcHJpbnRsbigixJHDoyB04bqhbyBmbG93IikgICAgICAgICAgICAvLyDinYwgY2jGsGEgY8OzIGTDsm5nICIoZ-G7jWkgbeG6oW5n4oCmKSIgbsOgbwogICAgcHJpY2VzLm1hcCB7IGl0ICogMiB9ICAgICAgICAgICAgICAvLyDinYwgduG6q24gY2jGsGEgY2jhuqF5OiBtYXAgY8WpbmcgY2jhu4kgbMOgIGPDtG5nIHRo4bupYwogICAgcHJpbnRsbigixJHDoyBtYXAsIHbhuqtuIGNoxrBhIGNo4bqheSIpCiAgICBwcmljZXMuY29sbGVjdCB7IHByaW50bG4oIiAgbmjhuq1uICRpdCIpIH0gLy8g4pyFIGdp4budIG3hu5tpIGNo4bqheQp9Cg) · Xem thêm: [Flow](../concepts/flow.md), K302

### K302 · Hai màn hình cùng collect một flow → gọi API hai lần

`🟢 mới học` · `Kotlin`

**Gặp:** log server thấy cùng một request bị gọi 2–3 lần mỗi khi mở app; hoặc cảm biến bị đăng ký hai lần.

**Vì sao:** flow lạnh chạy lại **từ đầu** cho mỗi lần `collect`. Hai người nghe là hai lần chạy khối `flow { }`.

**Sửa:** biến nó thành flow nóng dùng chung bằng `stateIn` (có giá trị hiện tại) hoặc `shareIn` (chỉ phát sự kiện), tạo **một lần**
trong core / ViewModel.

```kotlin
val rates = repository.rates()                                     // ❌ mỗi màn collect = một lần gọi
val rates = repository.rates()
    .stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)   // ✅ một nguồn, nhiều người đọc
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMwMjogZmxvdyBs4bqhbmggY2jhuqF5IGzhuqFpIHThu6sgxJHhuqd1IGNobyBN4buWSSBuZ8aw4budaSBjb2xsZWN0LgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLioKaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy5mbG93LioKCnZhciBjYWxscyA9IDAKZnVuIHJhdGVzKCk6IEZsb3c8SW50PiA9IGZsb3cgewogICAgY2FsbHMrKwogICAgcHJpbnRsbigiICBn4buNaSBBUEkgbOG6p24gJGNhbGxzIikKICAgIGVtaXQoMjVfMDAwKQp9CgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmc8VW5pdD4gewogICAgdmFsIGZsb3cgPSByYXRlcygpCiAgICBmbG93LmNvbGxlY3QgeyBwcmludGxuKCJtw6BuIEE6ICRpdCIpIH0KICAgIGZsb3cuY29sbGVjdCB7IHByaW50bG4oIm3DoG4gQjogJGl0IikgfSAgICAgICAvLyDinYwgZ-G7jWkgQVBJIGzhuqduIDIKCiAgICAvLyDinIUgc3RhdGVJbjogbeG7mXQgbOG6p24gZ-G7jWksIG5oaeG7gXUgbmfGsOG7nWkgxJHhu41jCiAgICB2YWwgc2hhcmVkID0gcmF0ZXMoKS5zdGF0ZUluKHRoaXMpICAgICAgICAgICAgLy8gYuG6o24gc3VzcGVuZDogY2jhu50gZ2nDoSB0cuG7iyDEkeG6p3UgdGnDqm4KICAgIHByaW50bG4oIkEgxJHhu41jICR7c2hhcmVkLnZhbHVlfSwgQiDEkeG7jWMgJHtzaGFyZWQudmFsdWV9IikKICAgIGNvcm91dGluZUNvbnRleHQuY2FuY2VsQ2hpbGRyZW4oKSAgICAgICAgICAgICAvLyBk4burbmcgY29yb3V0aW5lIGdp4buvIFN0YXRlRmxvdwp9Cg) · Xem thêm: K324, K325

### K303 · Code nằm sau `stateFlow.collect { }` không bao giờ chạy

`🟢 mới học` · `Kotlin`

**Gặp:** dòng ngay sau `state.collect { … }` không chạy; compiler còn cảnh báo `Unreachable code.`

**Vì sao:** `StateFlow` và `SharedFlow` không bao giờ kết thúc. `collect` trên chúng có kiểu trả về `Nothing`: nó treo tới khi
coroutine bị huỷ.

**Sửa:** đặt mỗi `collect` vô hạn trong một `launch` riêng; code muốn chạy tiếp thì để ngoài `launch` đó.

```kotlin
scope.launch {
    state.collect { render(it) }
    println("xong")                              // ❌ không bao giờ in
}
scope.launch { state.collect { render(it) } }    // ✅
println("xong")
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMwMzogY29sbGVjdCBt4buZdCBTdGF0ZUZsb3cga2jDtG5nIGJhbyBnaeG7nSBr4bq_dCB0aMO6YywgZMOybmcgcGjDrWEgc2F1IGtow7RuZyBjaOG6oXkuCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuKgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLmZsb3cuKgoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nPFVuaXQ-IHsKICAgIHZhbCBzdGF0ZSA9IE11dGFibGVTdGF0ZUZsb3coIkxvYWRpbmciKQoKICAgIC8vIOKdjCB2aeG6v3QgdGjhurNuZyBgc3RhdGUuY29sbGVjdCB7IOKApiB9YCDhu58gxJHDonkgdGjDrCAieG9uZyIga2jDtG5nIGJhbyBnaeG7nSBpbiByYS4KICAgIC8vIERlbW8gZMO5bmcgdGltZW91dCDEkeG7gyB0aOG6pXkgbsOzIHRyZW86CiAgICB2YWwgZG9uZSA9IHdpdGhUaW1lb3V0T3JOdWxsKDMwMCkgewogICAgICAgIHN0YXRlLmNvbGxlY3QgeyBwcmludGxuKCIgIHbhur06ICRpdCIpIH0KICAgICAgICAiY29sbGVjdCDEkcOjIHhvbmciCiAgICB9CiAgICBwcmludGxuKCJzYXUgMzAwIG1zOiAke2RvbmUgPzogImNvbGxlY3QgduG6q24gxJFhbmcgY2jhu50sIGLhu4sgdGltZW91dCBj4bqvdCJ9IikKCiAgICAvLyDinIUgY29sbGVjdCB0cm9uZyBsYXVuY2ggcmnDqm5nLCBjb2RlIHBow61hIHNhdSBjaOG6oXkgdGnhur9wCiAgICB2YWwgam9iID0gbGF1bmNoIHsgc3RhdGUuY29sbGVjdCB7IHByaW50bG4oIiAgVUkgdGjhuqV5OiAkaXQiKSB9IH0KICAgIGRlbGF5KDEwMCkKICAgIHN0YXRlLnZhbHVlID0gIlJlYWR5IgogICAgZGVsYXkoMTAwKQogICAgcHJpbnRsbigieG9uZyIpCiAgICBqb2IuY2FuY2VsKCkKfQo) · Xem thêm: K304, K349

### K304 · Hai `collect` nối nhau, cái thứ hai không nhận gì

`🟢 mới học` · `Kotlin`

**Gặp:** trong một `launch` collect `battery` rồi collect `network`: pin cập nhật, mạng thì đứng im.

**Vì sao:** như K303, `battery.collect` không bao giờ trả về, nên dòng `network.collect` không bao giờ được chạy tới.

**Sửa:** mỗi flow một `launch`, hoặc gộp thành một flow bằng `combine` rồi collect một lần.

```kotlin
launch {
    battery.collect { … }
    network.collect { … }                        // ❌ không tới
}
launch { battery.collect { … } }                 // ✅
launch { network.collect { … } }
combine(battery, network, ::Dashboard).collect { … }   // ✅ hoặc gộp
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMwNDogaGFpIGNvbGxlY3QgbGnDqm4gdGnhur9wIHRyb25nIGPDuW5nIGNvcm91dGluZSAtPiBjw6FpIHRo4bupIGhhaSBraMO0bmcgYmFvIGdp4budIGNo4bqheS4KaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuZmxvdy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmc8VW5pdD4gewogICAgdmFsIGJhdHRlcnkgPSBNdXRhYmxlU3RhdGVGbG93KDgwKQogICAgdmFsIG5ldHdvcmsgPSBNdXRhYmxlU3RhdGVGbG93KCJ3aWZpIikKCiAgICBwcmludGxuKCLinYwgaGFpIGNvbGxlY3QgbuG7kWkgbmhhdToiKQogICAgdmFsIHdyb25nID0gbGF1bmNoIHsKICAgICAgICBiYXR0ZXJ5LmNvbGxlY3QgeyBwcmludGxuKCIgIHBpbiAkaXQiKSB9CiAgICAgICAgbmV0d29yay5jb2xsZWN0IHsgcHJpbnRsbigiICBt4bqhbmcgJGl0IikgfSAgIC8vIGtow7RuZyBiYW8gZ2nhu50gdOG7m2kgxJHDonkKICAgIH0KICAgIGRlbGF5KDEwMCkKICAgIHdyb25nLmNhbmNlbCgpCgogICAgcHJpbnRsbigi4pyFIG3hu5dpIGNvbGxlY3QgbeG7mXQgbGF1bmNoOiIpCiAgICB2YWwgcmlnaHQgPSBsYXVuY2ggewogICAgICAgIGxhdW5jaCB7IGJhdHRlcnkuY29sbGVjdCB7IHByaW50bG4oIiAgcGluICRpdCIpIH0gfQogICAgICAgIGxhdW5jaCB7IG5ldHdvcmsuY29sbGVjdCB7IHByaW50bG4oIiAgbeG6oW5nICRpdCIpIH0gfQogICAgfQogICAgZGVsYXkoMTAwKQogICAgcmlnaHQuY2FuY2VsKCkKfQo)

### K305 · `launchIn(scope)` mà không có `onEach`

`🟢 mới học` · `Kotlin`

**Gặp:** viết `flow.map { … }.launchIn(scope)`, flow có chạy nhưng không có gì xảy ra trên UI.

**Vì sao:** `launchIn` chỉ là `scope.launch { flow.collect() }` với collector rỗng. Việc cần làm với từng giá trị phải nằm trong
`onEach` đặt **trước** `launchIn`. Ngoài ra `launchIn` trả về `Job`; bỏ qua nó thì không có cách dừng riêng.

**Sửa:**

```kotlin
ticks.map { "tick $it" }.launchIn(scope)                       // ❌ chạy rồi vứt kết quả
val job = ticks.onEach { label = "tick $it" }.launchIn(scope)  // ✅ giữ job để cancel()
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMwNTogbGF1bmNoSW4gY2jhu4kgY29sbGVjdDsgdmnhu4djIGPhuqduIGzDoG0gcGjhuqNpIG7hurFtIHRyb25nIG9uRWFjaCwgdsOgIG5o4bubIGdp4buvIEpvYiDEkeG7gyBodeG7ty4KaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuZmxvdy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmc8VW5pdD4gewogICAgdmFsIHRpY2tzID0gZmxvdyB7IHJlcGVhdCgzKSB7IGVtaXQoaXQpOyBkZWxheSgyMCkgfSB9CgogICAgdGlja3MubWFwIHsgInRpY2sgJGl0IiB9LmxhdW5jaEluKHRoaXMpICAgICAgICAgICAgICAgIC8vIOKdjCBjaOG6oXkgbmjGsG5nIGNo4bqzbmcgaW4gZ8OsCiAgICB2YWwgam9iID0gdGlja3Mub25FYWNoIHsgcHJpbnRsbigiICDinIUgdGljayAkaXQiKSB9LmxhdW5jaEluKHRoaXMpCiAgICBqb2Iuam9pbigpCiAgICBwcmludGxuKCJ4b25nIikKfQo)

### K306 · Gán cùng giá trị cho `StateFlow` → UI không phản ứng

`🟢 mới học` · `Kotlin`

**Gặp:** bấm Lưu lần hai, toast "Đã lưu" không hiện lại. Đặt `state.value = x` mà collector không chạy.

**Vì sao:** `StateFlow` chỉ phát khi giá trị mới **khác** giá trị cũ, so bằng `equals`. Hai `data class` cùng nội dung là bằng nhau,
nên lần gán thứ hai bị bỏ qua.

**Sửa:** state là "đang hiển thị gì", không dùng cho việc "xảy ra một lần" (K315). Nếu thật sự cần phát lại, đưa vào giá trị một
thứ khác nhau (id, thời điểm) hoặc dùng `Channel`.

```kotlin
toast.value = Toast("Đã lưu")
toast.value = Toast("Đã lưu")      // ❌ bằng nhau → không phát
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMwNjogU3RhdGVGbG93IGNo4buJIHBow6F0IGtoaSBnacOhIHRy4buLIG3hu5tpIEtIw4FDIGdpw6EgdHLhu4sgY8WpIChzbyBi4bqxbmcgPT0pLgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLioKaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy5mbG93LioKCmRhdGEgY2xhc3MgVG9hc3QodmFsIHRleHQ6IFN0cmluZykKCmZ1biBtYWluKCkgPSBydW5CbG9ja2luZzxVbml0PiB7CiAgICB2YWwgdG9hc3QgPSBNdXRhYmxlU3RhdGVGbG93PFRvYXN0Pz4obnVsbCkKICAgIHZhbCB1aSA9IGxhdW5jaCB7IHRvYXN0LmNvbGxlY3QgeyBwcmludGxuKCIgIFVJOiAkaXQiKSB9IH0KICAgIGRlbGF5KDEwMCkKICAgIHRvYXN0LnZhbHVlID0gVG9hc3QoIsSQw6MgbMawdSIpOyBkZWxheSgxMDApCiAgICB0b2FzdC52YWx1ZSA9IFRvYXN0KCLEkMOjIGzGsHUiKTsgZGVsYXkoMTAwKSAgIC8vIOKdjCBi4bqxbmcgbmhhdSB0aGVvIGVxdWFscyAtPiBraMO0bmcgcGjDoXQgbOG6oWkKICAgIHByaW50bG4oImzhuqduIGzGsHUgdGjhu6kgaGFpIGtow7RuZyBoaeG7h24gdG9hc3QiKQogICAgdWkuY2FuY2VsKCkKfQo) · Xem thêm: K315, [Flow](../concepts/flow.md)

### K307 · UI chỉ thấy giá trị cuối, mất các bước ở giữa

`🟡 đã làm app` · `Kotlin`

**Gặp:** cập nhật tiến độ 10%, 20%, … 100% liên tục, thanh tiến độ nhảy thẳng từ 0 lên 100.

**Vì sao:** `StateFlow` được **conflate**: nó chỉ giữ giá trị mới nhất. Collector chậm hơn người gán (hoặc chưa kịp chạy) sẽ bỏ
qua giá trị trung gian. Đây là thiết kế, không phải lỗi: UI chỉ cần trạng thái hiện tại.

**Sửa:** chấp nhận nếu chỉ là hiển thị. Nếu **mọi** giá trị đều quan trọng (log, lưu từng điểm GPS), dùng `Channel` hoặc
`MutableSharedFlow` có buffer, không dùng `StateFlow`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMwNzogU3RhdGVGbG93IGdp4buvIGdpw6EgdHLhu4sgTeG7mkkgTkjhuqRULCBnacOhIHRy4buLIHRydW5nIGdpYW4gY8OzIHRo4buDIGLhu4sgYuG7jyBxdWEuCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuKgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLmZsb3cuKgoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nPFVuaXQ-IHsKICAgIHZhbCBwcm9ncmVzcyA9IE11dGFibGVTdGF0ZUZsb3coMCkKICAgIHZhbCB1aSA9IGxhdW5jaCB7IHByb2dyZXNzLmNvbGxlY3QgeyBwcmludGxuKCIgIFVJIHRo4bqleSAkaXQlIikgfSB9CiAgICBkZWxheSgxMDApCiAgICBmb3IgKHAgaW4gMTAuLjEwMCBzdGVwIDEwKSBwcm9ncmVzcy52YWx1ZSA9IHAgICAgLy8gxJHhurd0IDEwIGzhuqduIGxp4buBbiwga2jDtG5nIG5oxrDhu51uZyB0aHJlYWQKICAgIGRlbGF5KDEwMCkKICAgIHByaW50bG4oIlVJIGtow7RuZyB0aOG6pXkgMTAsIDIwLCDigKYgOTA6IGNo4buJIHRo4bqleSAxMDAiKQogICAgdWkuY2FuY2VsKCkKfQo) · Xem thêm: K321

### K308 · Sửa list bên trong `StateFlow` mà UI không vẽ lại

`🟢 mới học` · `Kotlin`

**Gặp:** `_notes.value.add(note)`: dữ liệu đã đổi (debug thấy), nhưng màn hình vẫn danh sách cũ.

**Vì sao:** vẫn là **cùng một object**. Sửa tại chỗ không báo cho ai; gán lại `value = value` thì `equals` thấy bằng nhau (K306).

**Sửa:** giữ trong `StateFlow` kiểu chỉ đọc (`List`, `data class` với `val`) và luôn tạo object mới.

```kotlin
val notes = MutableStateFlow(mutableListOf<Note>())    // ❌
notes.value.add(note)

val notes = MutableStateFlow(listOf<Note>())           // ✅
notes.update { it + note }
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMwODogc-G7rWEgbGlzdCBiw6puIHRyb25nIFN0YXRlRmxvdyAtPiBjw7luZyBvYmplY3QsIFN0YXRlRmxvdyBraMO0bmcgcGjDoXQuCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuKgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLmZsb3cuKgoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nPFVuaXQ-IHsKICAgIHZhbCBub3RlcyA9IE11dGFibGVTdGF0ZUZsb3cobXV0YWJsZUxpc3RPZigiYSIpKQogICAgdmFsIHVpID0gbGF1bmNoIHsgbm90ZXMuY29sbGVjdCB7IHByaW50bG4oIiAgVUkgduG6vSAkaXQiKSB9IH0KICAgIGRlbGF5KDEwMCkKCiAgICBub3Rlcy52YWx1ZS5hZGQoImIiKSAgICAgICAgICAgICAgICAgICAgICAvLyDinYwgc-G7rWEgdOG6oWkgY2jhu5c6IGtow7RuZyBhaSDEkcaw4bujYyBiw6FvCiAgICBub3Rlcy52YWx1ZSA9IG5vdGVzLnZhbHVlICAgICAgICAgICAgICAgICAvLyDinYwgZ8OhbiBs4bqhaSBjw7luZyBvYmplY3Q6IHbhuqtuIGtow7RuZyBwaMOhdAogICAgZGVsYXkoMTAwKQogICAgcHJpbnRsbigibGlzdCDEkcOjIGzDoCAke25vdGVzLnZhbHVlfSBtw6AgVUkga2jDtG5nIHbhur0gbOG6oWkiKQoKICAgIHZhbCBjbGVhbiA9IE11dGFibGVTdGF0ZUZsb3cobGlzdE9mKCJhIikpCiAgICB2YWwgdWkyID0gbGF1bmNoIHsgY2xlYW4uY29sbGVjdCB7IHByaW50bG4oIiAgVUkyIHbhur0gJGl0IikgfSB9CiAgICBkZWxheSgxMDApCiAgICBjbGVhbi52YWx1ZSA9IGNsZWFuLnZhbHVlICsgImIiICAgICAgICAgICAvLyDinIUgbGlzdCBt4bubaQogICAgZGVsYXkoMTAwKQogICAgdWkuY2FuY2VsKCk7IHVpMi5jYW5jZWwoKQp9Cg) · Xem thêm: K309

### K309 · `value = value + 1` từ nhiều coroutine bị mất lần cập nhật

`🟡 đã làm app` · `Kotlin`

**Gặp:** bộ đếm, giỏ hàng thỉnh thoảng thiếu một món; chỉ xảy ra khi có nhiều việc chạy song song trên `Dispatchers.Default`.

**Vì sao:** `state.value = state.value.copy(…)` là **đọc rồi ghi**, hai bước riêng. Hai thread cùng đọc giá trị cũ thì một lần ghi
đè mất lần kia.

**Sửa:** dùng `update { }`: nó so-và-đổi (compare-and-set), bị chen thì tự chạy lại lambda với giá trị mới nhất. Lambda phải
thuần (không gọi mạng, không gây hiệu ứng phụ) vì có thể chạy nhiều lần.

```kotlin
_state.value = _state.value.copy(count = _state.value.count + 1)   // ❌
_state.update { it.copy(count = it.count + 1) }                    // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMwOTogdmFsdWUgPSB2YWx1ZSArIDEgdOG7qyBuaGnhu4F1IHRocmVhZCBsw6BtIG3huqV0IGzhuqduIGPhu5luZzsgdXBkYXRlIHsgfSB0aMOsIGtow7RuZy4KaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuZmxvdy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmc8VW5pdD4gewogICAgdmFsIGEgPSBNdXRhYmxlU3RhdGVGbG93KDApCiAgICB2YWwgYiA9IE11dGFibGVTdGF0ZUZsb3coMCkKICAgIHdpdGhDb250ZXh0KERpc3BhdGNoZXJzLkRlZmF1bHQpIHsKICAgICAgICByZXBlYXQoMTAwMCkgewogICAgICAgICAgICBsYXVuY2ggewogICAgICAgICAgICAgICAgcmVwZWF0KDEwMCkgewogICAgICAgICAgICAgICAgICAgIGEudmFsdWUgPSBhLnZhbHVlICsgMSAgICAgLy8g4p2MIMSR4buNYyBy4buTaSBnaGk6IGhhaSB0aHJlYWQgY8OzIHRo4buDIMSR4buNYyBjw7luZyBz4buRCiAgICAgICAgICAgICAgICAgICAgYi51cGRhdGUgeyBpdCArIDEgfSAgICAgICAvLyDinIUgY29tcGFyZS1hbmQtc2V0LCB0aOG7rSBs4bqhaSBu4bq_dSBi4buLIGNoZW4KICAgICAgICAgICAgICAgIH0KICAgICAgICAgICAgfQogICAgICAgIH0KICAgIH0KICAgIHByaW50bG4oInZhbHVlID0gdmFsdWUgKyAxIDogJHthLnZhbHVlfSIpICAgLy8gdGjGsOG7nW5nIG5o4buPIGjGoW4gMTAwMDAwCiAgICBwcmludGxuKCJ1cGRhdGUgeyBpdCArIDEgfSA6ICR7Yi52YWx1ZX0iKSAgIC8vIGx1w7RuIDEwMDAwMAp9Cg)

### K310 · Đưa `MutableStateFlow` ra ngoài, UI tự sửa state

`🟢 mới học` · `Kotlin`

**Gặp:** state đổi mà không qua hàm nào của ViewModel; tìm mãi mới thấy một màn hình ép kiểu rồi gán `value`.

**Vì sao:** khai báo `val state: StateFlow<T> = _state` chỉ đổi **kiểu nhìn thấy**, object bên dưới vẫn là `MutableStateFlow`.
Ai ép kiểu `as MutableStateFlow` là sửa được.

**Sửa:** `private val _state`, đưa ra ngoài bằng `asStateFlow()` (object bọc chỉ đọc). Tương tự `asSharedFlow()`.

```kotlin
private val _state = MutableStateFlow(UiState())
val state: StateFlow<UiState> = _state                  // ❌ vẫn ép kiểu ngược được
val state: StateFlow<UiState> = _state.asStateFlow()    // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMxMDogxJHGsGEgTXV0YWJsZVN0YXRlRmxvdyByYSBuZ2_DoGkgZMaw4bubaSBraeG7g3UgU3RhdGVGbG93IHbhuqtuIGLhu4sgw6lwIGtp4buDdSBuZ8aw4bujYyBs4bqhaSDEkcaw4bujYy4KaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy5mbG93LioKCmNsYXNzIENvdW50ZXJDb3JlIHsKICAgIHByaXZhdGUgdmFsIF9jb3VudCA9IE11dGFibGVTdGF0ZUZsb3coMCkKICAgIHZhbCBsZWFreTogU3RhdGVGbG93PEludD4gPSBfY291bnQgICAgICAgICAgICAgICAgIC8vIOKdjCB24bqrbiBsw6AgTXV0YWJsZVN0YXRlRmxvdwogICAgdmFsIGNvdW50OiBTdGF0ZUZsb3c8SW50PiA9IF9jb3VudC5hc1N0YXRlRmxvdygpICAgLy8g4pyFIG9iamVjdCBjaOG7iSDEkeG7jWMKICAgIGZ1biBpbmMoKSA9IF9jb3VudC51cGRhdGUgeyBpdCArIDEgfQp9CgpmdW4gbWFpbigpIHsKICAgIHZhbCBjb3JlID0gQ291bnRlckNvcmUoKQogICAgKGNvcmUubGVha3kgYXMgTXV0YWJsZVN0YXRlRmxvdykudmFsdWUgPSA5OTkgICAgICAgIC8vIFVJIHPhu61hIHRo4bqzbmcgc3RhdGUKICAgIHByaW50bG4oImLhu4sgc-G7rWEgdOG7qyBuZ2_DoGk6ICR7Y29yZS5jb3VudC52YWx1ZX0iKQogICAgdHJ5IHsKICAgICAgICAoY29yZS5jb3VudCBhcyBNdXRhYmxlU3RhdGVGbG93KS52YWx1ZSA9IDAKICAgIH0gY2F0Y2ggKGU6IENsYXNzQ2FzdEV4Y2VwdGlvbikgewogICAgICAgIHByaW50bG4oImFzU3RhdGVGbG93KCk6IMOpcCBraeG7g3UgbMOgIENsYXNzQ2FzdEV4Y2VwdGlvbiIpCiAgICB9Cn0K)

### K311 · `MutableStateFlow` báo thiếu giá trị ban đầu

`🟢 mới học` · `Kotlin`

**Gặp:** `No value passed for parameter 'value'.`

**Vì sao:** `StateFlow` luôn có một giá trị hiện tại, kể cả trước khi dữ liệu tới. Không có giá trị "trống".

**Sửa:** mô tả trạng thái "chưa có" một cách rõ ràng: `null`, list rỗng, hoặc tốt nhất là một case `Loading` trong `sealed`.

```kotlin
val state = MutableStateFlow<List<Note>>()                        // ❌
val state = MutableStateFlow<NotesState>(NotesState.Loading)      // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMxMTogTXV0YWJsZVN0YXRlRmxvdyBi4bqvdCBideG7mWMgY8OzIGdpw6EgdHLhu4sgYmFuIMSR4bqndS4KLy8gU-G7rWE6IHRow6ptIHRy4bqhbmcgdGjDoWkgIsSRYW5nIHThuqNpIiAtPiBNdXRhYmxlU3RhdGVGbG93PFNjcmVlblN0YXRlPihTY3JlZW5TdGF0ZS5Mb2FkaW5nKQppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLmZsb3cuKgoKc2VhbGVkIGludGVyZmFjZSBTY3JlZW5TdGF0ZSB7CiAgICBkYXRhIG9iamVjdCBMb2FkaW5nIDogU2NyZWVuU3RhdGUKICAgIGRhdGEgY2xhc3MgUmVhZHkodmFsIGl0ZW1zOiBMaXN0PFN0cmluZz4pIDogU2NyZWVuU3RhdGUKfQoKZnVuIG1haW4oKSB7CiAgICB2YWwgc3RhdGUgPSBNdXRhYmxlU3RhdGVGbG93PExpc3Q8U3RyaW5nPj4oKSAgICAgLy8g4p2MIE5vIHZhbHVlIHBhc3NlZCBmb3IgcGFyYW1ldGVyICd2YWx1ZScKICAgIHByaW50bG4oc3RhdGUudmFsdWUpCn0K) · Xem thêm: K323

### K312 · `first { }` trên StateFlow trả về ngay, không chờ lần mới

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** viết "chờ tới khi mất kết nối rồi nối lại", nhưng hàm nối lại chạy ngay lập tức, lặp vô hạn.

**Vì sao:** collector mới của `StateFlow` nhận **giá trị hiện tại** trước tiên. Nếu giá trị đó đã khớp điều kiện (ví dụ `Lost` còn
sót từ lần trước), `first { }` trả về luôn.

**Sửa:** chỉ đặt "người canh" khi state đang ở trạng thái ngược lại (đang `Connected`), hoặc bỏ giá trị hiện tại bằng `drop(1)`.

```kotlin
transport.link.first { it == LinkStatus.Lost }            // ❌ nếu link đang Lost thì trả về ngay
transport.link.drop(1).first { it == LinkStatus.Lost }    // ✅ chỉ lần thay đổi kế tiếp
```

**Gặp ở:** [`apps/ObdLink/shared/src/commonMain/kotlin/dev/obdlink/core/ObdCore.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/shared/src/commonMain/kotlin/dev/obdlink/core/ObdCore.kt) (chỉ canh sau khi đã `Connected`) ·
[▶ Chạy thử](../playground/index.html#code=Ly8gSzMxMjogZmlyc3QgeyB9IHRyw6puIFN0YXRlRmxvdyB0cuG6oyB24buBIE5HQVkgbuG6v3UgZ2nDoSB0cuG7iyBoaeG7h24gdOG6oWkgxJHDoyBraOG7m3AuCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuKgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLmZsb3cuKgoKZW51bSBjbGFzcyBMaW5rIHsgQ29ubmVjdGVkLCBMb3N0IH0KCmZ1biBtYWluKCkgPSBydW5CbG9ja2luZzxVbml0PiB7CiAgICB2YWwgbGluayA9IE11dGFibGVTdGF0ZUZsb3coTGluay5Mb3N0KSAgICAgICAgLy8gbOG6p24gcuG7m3QgdHLGsOG7m2MgduG6q24gY8OybiBu4bqxbSDEkcOzCiAgICBsaW5rLnZhbHVlID0gTGluay5Db25uZWN0ZWQgICAgICAgICAgICAgICAgICAgIC8vIG7hu5FpIGzhuqFpIHhvbmcKICAgIGxpbmsudmFsdWUgPSBMaW5rLkxvc3QgICAgICAgICAgICAgICAgICAgICAgICAgLy8gZ2nhuqMgc-G7rSBjaMawYSBhaSDEkeG7lWkgbOG6oWkuLi4KCiAgICB2YWwgd2F0Y2hlciA9IGFzeW5jIHsgbGluay5maXJzdCB7IGl0ID09IExpbmsuTG9zdCB9OyAidGjhuqV5IExvc3QiIH0KICAgIGRlbGF5KDEwMCkKICAgIHByaW50bG4oIuKdjCB3YXRjaGVyOiAke2lmICh3YXRjaGVyLmlzQ29tcGxldGVkKSAieG9uZyBuZ2F5IGzhuq1wIHThu6ljLCBkw7kgY2jGsGEgY8OzIGzhuqduIHLhu5t0IE3hu5pJIiBlbHNlICLEkWFuZyBjaOG7nSJ9IikKCiAgICAvLyDinIUgY2jhu4kgxJHhurd0IHdhdGNoZXIga2hpIGxpbmsgxJFhbmcgQ29ubmVjdGVkLCBob-G6t2MgYuG7jyBnacOhIHRy4buLIGhp4buHbiB04bqhaSBi4bqxbmcgZHJvcCgxKQogICAgbGluay52YWx1ZSA9IExpbmsuQ29ubmVjdGVkCiAgICB2YWwgbmV4dCA9IGFzeW5jIHsgbGluay5kcm9wKDEpLmZpcnN0IHsgaXQgPT0gTGluay5Mb3N0IH07ICJ0aOG6pXkgbOG6p24gcuG7m3QgbeG7m2kiIH0KICAgIGRlbGF5KDEwMCkKICAgIHByaW50bG4oIuKchSDEkWFuZyBjaOG7nTogJHshbmV4dC5pc0NvbXBsZXRlZH0iKQogICAgbGluay52YWx1ZSA9IExpbmsuTG9zdAogICAgcHJpbnRsbigi4pyFICR7bmV4dC5hd2FpdCgpfSIpCn0K)

### K313 · Collector thấy trạng thái cũ trước, tưởng việc đã xong

`🟡 đã làm app` · `Kotlin` · `Android`

**Gặp:** service gọi `recorder.start()` rồi collect `recorder.state`; giá trị đầu tiên là `Idle` nên service hiểu "chuyến đã xong" và
tự `stopSelf()` ngay lúc vừa mở.

**Vì sao:** `start()` chỉ gửi lệnh, core xử lý **bất đồng bộ**. Lúc service bắt đầu collect, `StateFlow` vẫn đang giữ giá trị cũ.

**Sửa:** đừng hiểu giá trị đầu tiên là "kết quả". Chỉ coi `Idle` là "xong" khi trước đó đã thấy `Recording`/`Paused`; hoặc chờ đúng
trạng thái bằng `first { it is Recording }` trước khi canh tiếp.

```kotlin
var seenActive = false
recorder.state.collect { s ->
    if (s is RecordState.Recording) seenActive = true
    if (s == RecordState.Idle && seenActive) stopSelf()   // ✅ Idle SAU Recording
}
```

**Gặp ở:** [`apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/service/TrackingService.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/service/TrackingService.kt) ·
[▶ Chạy thử](../playground/index.html#code=Ly8gSzMxMzogY29sbGVjdG9yIGLhuq90IMSR4bqndSB0aOG6pXkgZ2nDoSB0cuG7iyBDxagsIHbDrCBs4buHbmggc3RhcnQgY2jhuqF5IGLhuqV0IMSR4buTbmcgYuG7mS4KaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuZmxvdy4qCgpzZWFsZWQgaW50ZXJmYWNlIFJlYyB7IGRhdGEgb2JqZWN0IElkbGUgOiBSZWM7IGRhdGEgb2JqZWN0IFJlY29yZGluZyA6IFJlYyB9CgpjbGFzcyBSZWNvcmRlcihzY29wZTogQ29yb3V0aW5lU2NvcGUpIHsKICAgIHByaXZhdGUgdmFsIF9zdGF0ZSA9IE11dGFibGVTdGF0ZUZsb3c8UmVjPihSZWMuSWRsZSkKICAgIHZhbCBzdGF0ZTogU3RhdGVGbG93PFJlYz4gPSBfc3RhdGUKICAgIHByaXZhdGUgdmFsIHNjb3BlID0gc2NvcGUKICAgIGZ1biBzdGFydCgpIHsgc2NvcGUubGF1bmNoIHsgZGVsYXkoMjApOyBfc3RhdGUudmFsdWUgPSBSZWMuUmVjb3JkaW5nIH0gfSAgLy8geOG7rSBsw70gc2F1CiAgICBmdW4gc3RvcCgpIHsgc2NvcGUubGF1bmNoIHsgX3N0YXRlLnZhbHVlID0gUmVjLklkbGUgfSB9Cn0KCmZ1biBtYWluKCkgPSBydW5CbG9ja2luZzxVbml0PiB7CiAgICB2YWwgciA9IFJlY29yZGVyKHRoaXMpCiAgICByLnN0YXJ0KCkKICAgIHZhbCBzZXJ2aWNlID0gbGF1bmNoIHsKICAgICAgICB2YXIgc2VlbkFjdGl2ZSA9IGZhbHNlCiAgICAgICAgci5zdGF0ZS5jb2xsZWN0IHsgcyAtPgogICAgICAgICAgICBwcmludGxuKCIgIHNlcnZpY2UgdGjhuqV5ICRzIikKICAgICAgICAgICAgaWYgKHMgPT0gUmVjLlJlY29yZGluZykgc2VlbkFjdGl2ZSA9IHRydWUKICAgICAgICAgICAgaWYgKHMgPT0gUmVjLklkbGUgJiYgIXNlZW5BY3RpdmUpIHByaW50bG4oIiAg4p2MIG7hur91IGThu6tuZyDhu58gxJHDonk6IHNlcnZpY2UgdOG6r3QgbmdheSBsw7pjIHbhu6thIG3hu58iKQogICAgICAgICAgICBpZiAocyA9PSBSZWMuSWRsZSAmJiBzZWVuQWN0aXZlKSB7IHByaW50bG4oIiAg4pyFIElkbGUgU0FVIFJlY29yZGluZyA9IGNodXnhur9uIHhvbmciKTsgY2FuY2VsKCkgfQogICAgICAgIH0KICAgIH0KICAgIGRlbGF5KDMwMCk7IHIuc3RvcCgpCiAgICBzZXJ2aWNlLmpvaW4oKQp9Cg)

### K314 · `distinctUntilChanged()` trên `StateFlow` báo lỗi deprecated

`🟢 mới học` · `Kotlin`

**Gặp:** `'fun <T> StateFlow<T>.distinctUntilChanged(): Flow<T>' is deprecated. Applying 'distinctUntilChanged' to StateFlow has no effect. See the StateFlow documentation on Operator Fusion.`

**Vì sao:** `StateFlow` vốn đã bỏ giá trị trùng và đã conflate. Thư viện đánh dấu các toán tử vô nghĩa là deprecated mức ERROR để bạn
khỏi tưởng chúng làm gì đó: `distinctUntilChanged`, `conflate` trên `StateFlow`; `flowOn`, `cancellable` trên mọi `SharedFlow`
(kể cả `StateFlow`).

**Sửa:** xoá toán tử đó. Cần lọc theo một phần của state thì `map { it.part }.distinctUntilChanged()` (sau `map` là `Flow` thường).

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMxNDogbeG7mXQgc-G7kSB0b8OhbiB04butIMOhcCBsw6puIFN0YXRlRmxvdyBsw6AgdsO0IG5naMSpYSwgY29tcGlsZXIgY2jhurduIGx1w7RuLgovLyBT4butYTogeG_DoSBkaXN0aW5jdFVudGlsQ2hhbmdlZCgpIOKAlCBTdGF0ZUZsb3cgduG7kW4gxJHDoyBjaOG7iSBwaMOhdCBraGkgZ2nDoSB0cuG7iyDEkeG7lWkuCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuZmxvdy4qCgpmdW4gbWFpbigpIHsKICAgIHZhbCBzdGF0ZSA9IE11dGFibGVTdGF0ZUZsb3coMSkKICAgIHZhbCBhID0gc3RhdGUuZGlzdGluY3RVbnRpbENoYW5nZWQoKSAgIC8vIOKdjCBs4buXaSBjb21waWxlCiAgICBwcmludGxuKGEpCn0K)

### K315 · Dùng `StateFlow` cho sự kiện một lần (toast, điều hướng)

`🟡 đã làm app` · `Kotlin` · `Android`

**Gặp:** xoay máy là màn hình chi tiết mở lại lần nữa, hoặc toast lỗi hiện lại; hoặc ngược lại, lỗi thứ hai giống lỗi đầu không hiện (K306).

**Vì sao:** `StateFlow` luôn trao **giá trị hiện tại** cho người nghe mới. Màn hình tạo lại sau khi xoay là người nghe mới, nên nhận lại
sự kiện cũ.

**Sửa:** cách đơn giản và hợp với Compose: biến sự kiện thành **state** rồi xoá nó khi UI xử lý xong (`onNavigated()` đặt về `null`).
Nếu cần một luồng sự kiện thật, dùng `Channel` + `receiveAsFlow()`: mỗi sự kiện được nhận đúng một lần.

```kotlin
private val _events = Channel<UiEvent>(Channel.BUFFERED)
val events = _events.receiveAsFlow()        // ✅ không phát lại cho người nghe mới
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMxNTogU3RhdGVGbG93IGzDoG0ga8Oqbmggc-G7sSBraeG7h246IG5nxrDhu51pIG5naGUgbeG7m2kgbOG6oWkgbmjhuq1uIGV2ZW50IGPFqS4KaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuY2hhbm5lbHMuQ2hhbm5lbAppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLmZsb3cuKgoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nPFVuaXQ-IHsKICAgIHZhbCBuYXZpZ2F0ZSA9IE11dGFibGVTdGF0ZUZsb3c8U3RyaW5nPz4obnVsbCkKICAgIG5hdmlnYXRlLnZhbHVlID0gImRldGFpbC80MiIKCiAgICB2YWwgc2NyZWVuMSA9IGxhdW5jaCB7IG5hdmlnYXRlLmZpbHRlck5vdE51bGwoKS5jb2xsZWN0IHsgcHJpbnRsbigiICDinYwgbcOgbiBow6xuaCBt4bufICRpdCIpIH0gfQogICAgZGVsYXkoMTAwKTsgc2NyZWVuMS5jYW5jZWwoKSAgICAgICAgICAgICAgICAgICAgICAgICAgLy8geG9heSBtw6F5OiBtw6BuIGjDrG5oIHThuqFvIGzhuqFpCiAgICB2YWwgc2NyZWVuMiA9IGxhdW5jaCB7IG5hdmlnYXRlLmZpbHRlck5vdE51bGwoKS5jb2xsZWN0IHsgcHJpbnRsbigiICDinYwgbcOgbiBow6xuaCAoc2F1IHhvYXkpIGzhuqFpIG3hu58gJGl0IikgfSB9CiAgICBkZWxheSgxMDApOyBzY3JlZW4yLmNhbmNlbCgpCgogICAgLy8g4pyFIENoYW5uZWw6IG3hu5dpIGV2ZW50IMSRxrDhu6NjIG5o4bqtbiDEkcO6bmcgbeG7mXQgbOG6p24KICAgIHZhbCBldmVudHMgPSBDaGFubmVsPFN0cmluZz4oQ2hhbm5lbC5CVUZGRVJFRCkKICAgIHZhbCBmbG93ID0gZXZlbnRzLnJlY2VpdmVBc0Zsb3coKQogICAgZXZlbnRzLnNlbmQoImRldGFpbC80MiIpCiAgICB2YWwgczEgPSBsYXVuY2ggeyBmbG93LmNvbGxlY3QgeyBwcmludGxuKCIgIOKchSBt4bufICRpdCIpIH0gfQogICAgZGVsYXkoMTAwKTsgczEuY2FuY2VsKCkKICAgIHZhbCBzMiA9IGxhdW5jaCB7IGZsb3cuY29sbGVjdCB7IHByaW50bG4oIiAg4pyFIChzYXUgeG9heSkgbeG7nyAkaXQiKSB9IH0KICAgIGRlbGF5KDEwMCk7IHMyLmNhbmNlbCgpCiAgICBwcmludGxuKCJzYXUgeG9heSBraMO0bmcgbeG7nyBs4bqhaSBs4bqnbiBu4buvYSIpCn0K) · Xem thêm: K316, K320, K501

### K316 · `MutableSharedFlow()` nuốt mất sự kiện khi chưa ai nghe

`🟡 đã làm app` · `Kotlin`

**Gặp:** `emit("Đã lưu")` chạy xong không lỗi, nhưng UI không bao giờ nhận; thường là sự kiện phát ngay khi mở màn hình.

**Vì sao:** `MutableSharedFlow()` mặc định `replay = 0`, không buffer. Lúc emit mà `subscriptionCount` là 0 thì giá trị bị **bỏ đi**,
và `emit` cũng không treo chờ ai.

**Sửa:** sự kiện mà người nghe có thể tới muộn thì dùng `Channel` (giữ lại tới khi có người nhận). Cần SharedFlow thì đảm bảo đã có
người nghe trước khi phát (K317).

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMxNjogTXV0YWJsZVNoYXJlZEZsb3coKSBt4bq3YyDEkeG7i25oIHJlcGxheSA9IDA6IGVtaXQga2hpIGNoxrBhIGFpIG5naGUgbMOgIG3huqV0LgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLioKaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy5mbG93LioKCmZ1biBtYWluKCkgPSBydW5CbG9ja2luZzxVbml0PiB7CiAgICB2YWwgZXZlbnRzID0gTXV0YWJsZVNoYXJlZEZsb3c8U3RyaW5nPigpCiAgICBldmVudHMuZW1pdCgixJDDoyBsxrB1IikgICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIGNoxrBhIGPDsyBhaSBuZ2hlIC0-IGLhu48gxJFpLCBlbWl0IGtow7RuZyB0cmVvCiAgICBwcmludGxuKCJzdWJzY3JpcHRpb25Db3VudCA9ICR7ZXZlbnRzLnN1YnNjcmlwdGlvbkNvdW50LnZhbHVlfSIpCiAgICB2YWwgdWkgPSBsYXVuY2ggeyBldmVudHMuY29sbGVjdCB7IHByaW50bG4oIiAgVUkgbmjhuq1uICRpdCIpIH0gfQogICAgZGVsYXkoMTAwKQogICAgZXZlbnRzLmVtaXQoIsSQw6MgeG_DoSIpICAgICAgICAgICAgICAgICAgICAgIC8vIOKchSBjw7MgbmfGsOG7nWkgbmdoZSBy4buTaQogICAgZGVsYXkoMTAwKQogICAgdWkuY2FuY2VsKCkKfQo) · Xem thêm: K315, K321

### K317 · `launch { collect }` rồi phát ngay → mất giá trị đầu

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** mở kết nối BLE, bật notify, ghi lệnh `ATZ` ngay: câu trả lời đầu tiên không bao giờ tới, init timeout không rõ lý do.

**Vì sao:** `launch` chỉ **xếp lịch** coroutine. Dòng ngay sau `launch` chạy trước khi coroutine kia kịp tới `collect`, nên lúc đó
chưa có ai đăng ký. Với BLE còn thêm: việc bật notify cũng bất đồng bộ.

**Sửa:** chờ tín hiệu "đã đăng ký xong" rồi mới phát/ghi: `onSubscription { }` của SharedFlow, hoặc callback tương tự của thư viện,
kèm `CompletableDeferred`.

```kotlin
val subscribed = CompletableDeferred<Unit>()
launch { answers.onSubscription { subscribed.complete(Unit) }.collect { … } }
subscribed.await()            // ✅ chắc chắn đã có người nghe
write("ATZ")
```

**Gặp ở:** [`apps/ObdLink/core/ble/src/commonMain/kotlin/dev/obdlink/ble/KableElmTransport.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/core/ble/src/commonMain/kotlin/dev/obdlink/ble/KableElmTransport.kt) ·
[▶ Chạy thử](../playground/index.html#code=Ly8gSzMxNzogbGF1bmNoIHsgY29sbGVjdCB9IHLhu5NpIGVtaXQgbmdheSAtPiBjb2xsZWN0b3IgY2jGsGEga-G7i3AgxJHEg25nIGvDvSwgbeG6pXQgZXZlbnQuCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuKgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLmZsb3cuKgoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nPFVuaXQ-IHsKICAgIHZhbCBhbnN3ZXJzID0gTXV0YWJsZVNoYXJlZEZsb3c8U3RyaW5nPigpCiAgICB2YWwgYSA9IGxhdW5jaCB7IGFuc3dlcnMuY29sbGVjdCB7IHByaW50bG4oIiAg4p2MIG5o4bqtbiAkaXQiKSB9IH0KICAgIGFuc3dlcnMuZW1pdCgiRUxNMzI3IHYxLjUiKSAgICAgICAgICAgICAgICAgLy8gbGF1bmNoIGNoxrBhIGNo4bqheSB04bubaSBjb2xsZWN0IC0-IG3huqV0CiAgICBkZWxheSgxMDApOyBhLmNhbmNlbCgpCiAgICBwcmludGxuKCLinYwga2jDtG5nIG5o4bqtbiDEkcaw4bujYyBnw6wiKQoKICAgIHZhbCBzdWJzY3JpYmVkID0gQ29tcGxldGFibGVEZWZlcnJlZDxVbml0PigpCiAgICB2YWwgYiA9IGxhdW5jaCB7CiAgICAgICAgYW5zd2Vycy5vblN1YnNjcmlwdGlvbiB7IHN1YnNjcmliZWQuY29tcGxldGUoVW5pdCkgfSAgIC8vIOKchSBiw6FvIGtoaSDEkcOjIMSRxINuZyBrw70geG9uZwogICAgICAgICAgICAuY29sbGVjdCB7IHByaW50bG4oIiAg4pyFIG5o4bqtbiAkaXQiKSB9CiAgICB9CiAgICBzdWJzY3JpYmVkLmF3YWl0KCkKICAgIGFuc3dlcnMuZW1pdCgiRUxNMzI3IHYxLjUiKQogICAgZGVsYXkoMTAwKTsgYi5jYW5jZWwoKQp9Cg)

### K318 · `tryEmit` luôn trả về `false`

`🟡 đã làm app` · `Kotlin`

**Gặp:** gọi `events.tryEmit(x)` từ hàm thường (không suspend) mà không ai nhận; kiểm kết quả thì thấy `false`.

**Vì sao:** `MutableSharedFlow()` không có buffer. `tryEmit` không được treo, nên khi có người nghe mà không còn chỗ chứa, nó
thất bại ngay.

**Sửa:** cho buffer (`extraBufferCapacity`), hoặc gọi `emit` trong coroutine, hoặc dùng `Channel(UNLIMITED).trySend`.

```kotlin
val events = MutableSharedFlow<Event>()                          // ❌ tryEmit = false
val events = MutableSharedFlow<Event>(extraBufferCapacity = 16)  // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMxODogdHJ5RW1pdCB0csOqbiBNdXRhYmxlU2hhcmVkRmxvdygpIGtow7RuZyBidWZmZXIgdHLhuqMgduG7gSBmYWxzZS4KaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuZmxvdy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmc8VW5pdD4gewogICAgdmFsIG5vQnVmZmVyID0gTXV0YWJsZVNoYXJlZEZsb3c8SW50PigpCiAgICB2YWwgdWkgPSBsYXVuY2ggeyBub0J1ZmZlci5jb2xsZWN0IHsgcHJpbnRsbigiICBuaOG6rW4gJGl0IikgfSB9CiAgICBkZWxheSgxMDApCiAgICBwcmludGxuKCLinYwgdHJ5RW1pdCA9ICR7bm9CdWZmZXIudHJ5RW1pdCgxKX0iKSAgICAgICAvLyBmYWxzZToga2jDtG5nIGPDsyBjaOG7lyBjaOG7qWEKICAgIHVpLmNhbmNlbCgpCgogICAgdmFsIHdpdGhCdWZmZXIgPSBNdXRhYmxlU2hhcmVkRmxvdzxJbnQ-KGV4dHJhQnVmZmVyQ2FwYWNpdHkgPSAxNikKICAgIHZhbCB1aTIgPSBsYXVuY2ggeyB3aXRoQnVmZmVyLmNvbGxlY3QgeyBwcmludGxuKCIgIOKchSBuaOG6rW4gJGl0IikgfSB9CiAgICBkZWxheSgxMDApCiAgICBwcmludGxuKCLinIUgdHJ5RW1pdCA9ICR7d2l0aEJ1ZmZlci50cnlFbWl0KDEpfSIpCiAgICBkZWxheSgxMDApOyB1aTIuY2FuY2VsKCkKfQo)

### K319 · `emit` bị treo vì một người nghe chậm

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** nguồn dữ liệu (cảm biến, socket) chậm dần, mỗi `emit` mất bằng thời gian xử lý của người nghe chậm nhất.

**Vì sao:** `SharedFlow` mặc định `BufferOverflow.SUSPEND`: khi buffer đầy, `emit` chờ **người nghe chậm nhất** nhận xong.
Một màn hình vẽ chậm kéo cả nguồn chậm theo.

**Sửa:** nếu chỉ cần giá trị mới nhất, cho buffer và bỏ giá trị cũ.

```kotlin
MutableSharedFlow<Reading>(
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,   // ✅ emit không bao giờ treo
)
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMxOTogU2hhcmVkRmxvdyBjaOG7nSBuZ8aw4budaSBuZ2hlIGNo4bqtbSBuaOG6pXQ6IGVtaXQgYuG7iyB0cmVvIGtoaSBidWZmZXIgxJHhuqd5LgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLioKaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy5jaGFubmVscy5CdWZmZXJPdmVyZmxvdwppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLmZsb3cuKgppbXBvcnQga290bGluLnRpbWUubWVhc3VyZVRpbWUKCmZ1biBtYWluKCkgPSBydW5CbG9ja2luZzxVbml0PiB7CiAgICB2YWwgcmVhZGluZ3MgPSBNdXRhYmxlU2hhcmVkRmxvdzxJbnQ-KCkKICAgIHZhbCBzbG93ID0gbGF1bmNoIHsgcmVhZGluZ3MuY29sbGVjdCB7IGRlbGF5KDEwMCkgfSB9ICAgLy8gbmfGsOG7nWkgbmdoZSBjaOG6rW0gMTAwIG1zL2dpw6EgdHLhu4sKICAgIGRlbGF5KDEwKQogICAgdmFsIHQgPSBtZWFzdXJlVGltZSB7IHJlcGVhdCg1KSB7IHJlYWRpbmdzLmVtaXQoaXQpIH0gfQogICAgcHJpbnRsbigi4p2MIDUgbOG6p24gZW1pdCBt4bqldCAke3QuaW5XaG9sZU1pbGxpc2Vjb25kc30gbXMgKGLhu4sga8OpbyB0aGVvIG5nxrDhu51pIGNo4bqtbSkiKQogICAgc2xvdy5jYW5jZWwoKQoKICAgIHZhbCBsYXRlc3QgPSBNdXRhYmxlU2hhcmVkRmxvdzxJbnQ-KGV4dHJhQnVmZmVyQ2FwYWNpdHkgPSAxLCBvbkJ1ZmZlck92ZXJmbG93ID0gQnVmZmVyT3ZlcmZsb3cuRFJPUF9PTERFU1QpCiAgICB2YWwgc2xvdzIgPSBsYXVuY2ggeyBsYXRlc3QuY29sbGVjdCB7IGRlbGF5KDEwMCkgfSB9CiAgICBkZWxheSgxMCkKICAgIHZhbCB0MiA9IG1lYXN1cmVUaW1lIHsgcmVwZWF0KDUpIHsgbGF0ZXN0LmVtaXQoaXQpIH0gfQogICAgcHJpbnRsbigi4pyFIERST1BfT0xERVNUOiA1IGzhuqduIGVtaXQgbeG6pXQgJHt0Mi5pbldob2xlTWlsbGlzZWNvbmRzfSBtcyIpCiAgICBzbG93Mi5jYW5jZWwoKQp9Cg) · Xem thêm: K346

### K320 · `replay = 1` làm sự kiện cũ hiện lại

`🟡 đã làm app` · `Kotlin`

**Gặp:** để chữa K316 bạn đặt `MutableSharedFlow(replay = 1)`; giờ mỗi lần xoay máy, toast lỗi cũ lại hiện.

**Vì sao:** `replay` là số giá trị cũ gửi lại cho **mỗi** người nghe mới. Có replay thì SharedFlow hành xử gần như StateFlow.

**Sửa:** sự kiện một lần thì `Channel` (K315). Nếu đã dùng replay và cần xoá, gọi `resetReplayCache()` sau khi xử lý.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMyMDogU2hhcmVkRmxvdyByZXBsYXkgPSAxIGzDoG0gZXZlbnQgbeG7mXQgbOG6p24gYuG7iyBwaMOhdCBs4bqhaSBjaG8gbmfGsOG7nWkgbmdoZSBt4bubaS4KaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuZmxvdy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmc8VW5pdD4gewogICAgdmFsIHRvYXN0cyA9IE11dGFibGVTaGFyZWRGbG93PFN0cmluZz4ocmVwbGF5ID0gMSkKICAgIHRvYXN0cy5lbWl0KCJM4buXaSBt4bqhbmciKQogICAgdmFsIHMxID0gbGF1bmNoIHsgdG9hc3RzLmNvbGxlY3QgeyBwcmludGxuKCIgIG3DoG4gaMOsbmg6ICRpdCIpIH0gfQogICAgZGVsYXkoMTAwKTsgczEuY2FuY2VsKCkKICAgIHZhbCBzMiA9IGxhdW5jaCB7IHRvYXN0cy5jb2xsZWN0IHsgcHJpbnRsbigiICDinYwgbcOgbiBow6xuaCBzYXUgeG9heTogJGl0IChs4bqhaSBoaeG7h24pIikgfSB9CiAgICBkZWxheSgxMDApOyBzMi5jYW5jZWwoKQp9Cg)

### K321 · `Channel` có hai người nghe, mỗi người chỉ nhận một phần

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** hai màn hình cùng `collect` `channel.receiveAsFlow()`; mỗi màn thấy lúc có lúc không, cộng lại mới đủ.

**Vì sao:** `Channel` là **hàng đợi**: mỗi phần tử chỉ được **một** người nhận (fan-out). `SharedFlow` mới là **phát thanh**: mọi người
nghe đều nhận.

**Sửa:** chọn theo ý nghĩa. Lệnh cho một bộ xử lý (hàng đợi lệnh của core) → `Channel`. Thông tin cho nhiều người xem → `SharedFlow` /
`StateFlow`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMyMTogQ2hhbm5lbCBDSElBIGdpw6EgdHLhu4sgY2hvIGPDoWMgY29sbGVjdG9yOyBTaGFyZWRGbG93IFBIw4FUIGNobyB04bqldCBj4bqjLgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLioKaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy5jaGFubmVscy5DaGFubmVsCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuZmxvdy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmc8VW5pdD4gewogICAgdmFsIGNoID0gQ2hhbm5lbDxJbnQ-KCkKICAgIHZhbCBmbG93ID0gY2gucmVjZWl2ZUFzRmxvdygpCiAgICB2YWwgYSA9IGxhdW5jaCB7IGZsb3cuY29sbGVjdCB7IHByaW50bG4oIiAgQ2hhbm5lbCAtPiBBIG5o4bqtbiAkaXQiKSB9IH0KICAgIHZhbCBiID0gbGF1bmNoIHsgZmxvdy5jb2xsZWN0IHsgcHJpbnRsbigiICBDaGFubmVsIC0-IEIgbmjhuq1uICRpdCIpIH0gfQogICAgZGVsYXkoMTAwKQogICAgcmVwZWF0KDQpIHsgY2guc2VuZChpdCkgfQogICAgZGVsYXkoMTAwKTsgYS5jYW5jZWwoKTsgYi5jYW5jZWwoKQoKICAgIHZhbCBzZiA9IE11dGFibGVTaGFyZWRGbG93PEludD4oKQogICAgdmFsIGMgPSBsYXVuY2ggeyBzZi5jb2xsZWN0IHsgcHJpbnRsbigiICBTaGFyZWRGbG93IC0-IEMgbmjhuq1uICRpdCIpIH0gfQogICAgdmFsIGQgPSBsYXVuY2ggeyBzZi5jb2xsZWN0IHsgcHJpbnRsbigiICBTaGFyZWRGbG93IC0-IEQgbmjhuq1uICRpdCIpIH0gfQogICAgZGVsYXkoMTAwKQogICAgcmVwZWF0KDIpIHsgc2YuZW1pdChpdCkgfQogICAgZGVsYXkoMTAwKTsgYy5jYW5jZWwoKTsgZC5jYW5jZWwoKQp9Cg) · Xem thêm: K315

### K322 · `trySend` thất bại trong im lặng, mất lệnh

`🟡 đã làm app` · `Kotlin`

**Gặp:** bấm Dừng mà chuyến vẫn ghi; một điểm GPS từ callback biến mất. Không có exception nào.

**Vì sao:** `trySend` không treo; Channel đầy (hoặc đã đóng) thì nó trả về kết quả thất bại. Không ai đọc kết quả thì không ai biết.

**Sửa:** hàng đợi lệnh nhận từ callback / nút bấm thì dùng `Channel.UNLIMITED` để `trySend` không bao giờ thất bại vì đầy;
hoặc kiểm `trySend(x).isSuccess` và log khi thất bại.

```kotlin
private val commands = Channel<Command>(capacity = 2)          // ❌ đầy là mất lệnh
private val commands = Channel<Command>(Channel.UNLIMITED)     // ✅
```

**Gặp ở:** [`apps/TripLog/shared/src/commonMain/kotlin/dev/triplog/core/TripRecorder.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/shared/src/commonMain/kotlin/dev/triplog/core/TripRecorder.kt) ·
[▶ Chạy thử](../playground/index.html#code=Ly8gSzMyMjogdHJ5U2VuZCB2w6BvIENoYW5uZWwgxJHhuqd5IHRow6wgdGjhuqV0IGLhuqFpIHRyb25nIGltIGzhurduZyBu4bq_dSBraMO0bmcgxJHhu41jIGvhur90IHF14bqjLgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLmNoYW5uZWxzLkNoYW5uZWwKCmZ1biBtYWluKCkgewogICAgdmFsIHNtYWxsID0gQ2hhbm5lbDxJbnQ-KGNhcGFjaXR5ID0gMikKICAgIHJlcGVhdCg1KSB7IHNtYWxsLnRyeVNlbmQoaXQpIH0gICAgICAgICAgICAgICAgICAgICAgICAvLyDinYwga2jDtG5nIGtp4buDbSBr4bq_dCBxdeG6owogICAgcHJpbnRsbigi4p2MIGNhcGFjaXR5IDI6IGPDsm4gbOG6oWkgJHtMaXN0KDIpIHsgc21hbGwudHJ5UmVjZWl2ZSgpLmdldE9yTnVsbCgpIH19LCBt4bqldCAyLCAzLCA0IikKCiAgICB2YWwgciA9IENoYW5uZWw8SW50PihjYXBhY2l0eSA9IDEpLmFwcGx5IHsgdHJ5U2VuZCgwKSB9LnRyeVNlbmQoMSkKICAgIHByaW50bG4oImvhur90IHF14bqjIHRyeVNlbmQga2hpIMSR4bqneTogaXNTdWNjZXNzPSR7ci5pc1N1Y2Nlc3N9IikKCiAgICB2YWwgdW5saW1pdGVkID0gQ2hhbm5lbDxJbnQ-KENoYW5uZWwuVU5MSU1JVEVEKSAgICAgICAgICAvLyDinIUga2jDtG5nIGJhbyBnaeG7nSDEkeG6p3kKICAgIHJlcGVhdCg1KSB7IHVubGltaXRlZC50cnlTZW5kKGl0KSB9CiAgICBwcmludGxuKCLinIUgVU5MSU1JVEVEOiAke0xpc3QoNSkgeyB1bmxpbWl0ZWQudHJ5UmVjZWl2ZSgpLmdldE9yTnVsbCgpIH19IikKfQo)

### K323 · `stateIn(scope)` báo chỉ gọi được trong coroutine

`🟢 mới học` · `Kotlin`

**Gặp:** `Suspend function 'suspend fun <T> Flow<T>.stateIn(scope: CoroutineScope): StateFlow<T>' can only be called from a coroutine or another suspend function.`

**Vì sao:** bản `stateIn` một tham số **chờ** giá trị đầu tiên của nguồn, nên nó là hàm `suspend`. Trong khai báo property của
class thì không chờ được.

**Sửa:** dùng bản ba tham số: `scope`, `started`, `initialValue`. Bản này không suspend.

```kotlin
val state = source.stateIn(scope)                                        // ❌
val state = source.stateIn(scope, SharingStarted.Eagerly, NotesState.Loading)   // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMyMzogc3RhdGVJbihzY29wZSkga2jDtG5nIGPDsyBnacOhIHRy4buLIMSR4bqndSBsw6AgaMOgbSBzdXNwZW5kLgovLyBT4butYTogc291cmNlLnN0YXRlSW4oc2NvcGUsIFNoYXJpbmdTdGFydGVkLkVhZ2VybHksIGluaXRpYWxWYWx1ZSA9IDApCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuKgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLmZsb3cuKgoKY2xhc3MgQ29yZShzY29wZTogQ29yb3V0aW5lU2NvcGUsIHNvdXJjZTogRmxvdzxJbnQ-KSB7CiAgICB2YWwgc3RhdGU6IFN0YXRlRmxvdzxJbnQ-ID0gc291cmNlLnN0YXRlSW4oc2NvcGUpICAgICAvLyDinYwgZ-G7jWkgaMOgbSBzdXNwZW5kIG5nb8OgaSBjb3JvdXRpbmUKfQoKZnVuIG1haW4oKSB7fQo) · Xem thêm: K311

### K324 · Chọn sai `SharingStarted`: cảm biến chạy mãi hoặc tắt bật liên tục

`🟡 đã làm app` · `Kotlin`

**Gặp:** rời màn hình rồi mà GPS / cảm biến vẫn chạy (tốn pin); hoặc xoay máy là cảm biến tắt rồi bật lại.

**Vì sao:** `Eagerly` bắt đầu ngay và **không bao giờ dừng** tới khi scope chết. `WhileSubscribed()` (không timeout) dừng ngay khi người
nghe cuối rời đi, kể cả trong một giây xoay máy. `WhileSubscribed(5_000)` chờ 5 giây rồi mới dừng.

**Sửa:** nguồn tốn tài nguyên (cảm biến, GPS, socket) → `WhileSubscribed(5_000)`. Dữ liệu rẻ, cần luôn sẵn (đọc DB, state của core
sống cả app) → `Eagerly` là được.

**Gặp ở:** [`apps/DevicePulse/shared/src/commonMain/kotlin/dev/devicepulse/core/PulseMonitor.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/DevicePulse/shared/src/commonMain/kotlin/dev/devicepulse/core/PulseMonitor.kt) (`WhileSubscribed(5_000)`) ·
[▶ Chạy thử](../playground/index.html#code=Ly8gSzMyNDogV2hpbGVTdWJzY3JpYmVkKHRpbWVvdXQpIHThuq90IG5ndeG7k24ga2hpIGtow7RuZyBjw7JuIGFpIG5naGU7IEVhZ2VybHkgY2jhuqF5IG3Do2kuCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuKgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLmZsb3cuKgoKZnVuIHNlbnNvcihuYW1lOiBTdHJpbmcpID0gZmxvdyB7CiAgICBwcmludGxuKCIgIFskbmFtZV0gYuG6rXQgY-G6o20gYmnhur9uIikKICAgIHRyeSB7IHZhciBpID0gMDsgd2hpbGUgKHRydWUpIHsgZW1pdChpKyspOyBkZWxheSgyMCkgfSB9CiAgICBmaW5hbGx5IHsgcHJpbnRsbigiICBbJG5hbWVdIFThuq5UIGPhuqNtIGJp4bq_biIpIH0KfQoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nPFVuaXQ-IHsKICAgIHZhbCBlYWdlciA9IHNlbnNvcigiRWFnZXJseSIpLnN0YXRlSW4odGhpcywgU2hhcmluZ1N0YXJ0ZWQuRWFnZXJseSwgLTEpCiAgICB2YWwgbGF6eSA9IHNlbnNvcigiV2hpbGVTdWJzY3JpYmVkIikuc3RhdGVJbih0aGlzLCBTaGFyaW5nU3RhcnRlZC5XaGlsZVN1YnNjcmliZWQoMTAwKSwgLTEpCgogICAgdmFsIHNjcmVlbiA9IGxhdW5jaCB7IGxhenkuY29sbGVjdCB7IH0gfSAgICAgICAvLyBtw6BuIGjDrG5oIG3hu58KICAgIGRlbGF5KDUwKQogICAgc2NyZWVuLmNhbmNlbCgpICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8gbcOgbiBow6xuaCDEkcOzbmcKICAgIHByaW50bG4oIm3DoG4gaMOsbmggxJHDs25nLCBjaOG7nSAzMDAgbXPigKYiKQogICAgZGVsYXkoMzAwKQogICAgcHJpbnRsbigiRWFnZXJseSB24bqrbiBjaOG6oXk6IHZhbHVlID0gJHtlYWdlci52YWx1ZX0iKQogICAgY29yb3V0aW5lQ29udGV4dC5jYW5jZWxDaGlsZHJlbigpCn0K) · Xem thêm: K326

### K325 · `stateIn` trong getter: mỗi lần đọc là một nguồn mới

`🟡 đã làm app` · `Kotlin`

**Gặp:** mỗi lần Compose recompose (hoặc mỗi lần gọi `core.state`) lại thấy một request mạng / một listener mới.

**Vì sao:** `val state get() = flow.stateIn(…)` (hoặc `fun state() = …stateIn(…)`) chạy `stateIn` **mỗi lần đọc**, tạo một
StateFlow mới với một coroutine mới collect nguồn.

**Sửa:** tạo một lần, gán vào property thường (`val state = …stateIn(…)`).

```kotlin
val state: StateFlow<UiState> get() = source.stateIn(scope, Eagerly, UiState())   // ❌
val state: StateFlow<UiState> = source.stateIn(scope, Eagerly, UiState())          // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMyNTogc3RhdGVJbiB0cm9uZyBnZXR0ZXIgLT4gbeG7l2kgbOG6p24gxJHhu41jIHThuqFvIG3hu5l0IFN0YXRlRmxvdyBN4buaSSwgbmd14buTbiBjaOG6oXkgbOG6oWkuCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuKgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLmZsb3cuKgoKY2xhc3MgQ29yZShwcml2YXRlIHZhbCBzY29wZTogQ29yb3V0aW5lU2NvcGUpIHsKICAgIHByaXZhdGUgdmFsIHNvdXJjZSA9IGZsb3cgeyBwcmludGxuKCIgIGfhu41pIEFQSSIpOyBlbWl0KCJyYXRlcyIpIH0KICAgIHZhbCBiYWQ6IFN0YXRlRmxvdzxTdHJpbmc-IGdldCgpID0gc291cmNlLnN0YXRlSW4oc2NvcGUsIFNoYXJpbmdTdGFydGVkLkVhZ2VybHksICIiKSAgIC8vIOKdjAogICAgdmFsIGdvb2Q6IFN0YXRlRmxvdzxTdHJpbmc-ID0gc291cmNlLnN0YXRlSW4oc2NvcGUsIFNoYXJpbmdTdGFydGVkLkVhZ2VybHksICIiKSAgICAgICAgLy8g4pyFIHThuqFvIG3hu5l0IGzhuqduCn0KCmZ1biBtYWluKCkgPSBydW5CbG9ja2luZzxVbml0PiB7CiAgICBwcmludGxuKCJ04bqhbyBDb3JlIChnb29kIGNo4bqheSBuZ3Xhu5NuIMSRw7puZyAxIGzhuqduKToiKQogICAgdmFsIGNvcmUgPSBDb3JlKHRoaXMpCiAgICBkZWxheSgxMDApCiAgICBwcmludGxuKCLinIUgxJHhu41jIGdvb2QgMyBs4bqnbjoiKQogICAgcmVwZWF0KDMpIHsgY29yZS5nb29kOyBkZWxheSgxMDApIH0KICAgIHByaW50bG4oIuKdjCDEkeG7jWMgYmFkIDMgbOG6p246IikKICAgIHJlcGVhdCgzKSB7IGNvcmUuYmFkOyBkZWxheSgxMDApIH0KICAgIGNvcm91dGluZUNvbnRleHQuY2FuY2VsQ2hpbGRyZW4oKQp9Cg)

### K326 · Đọc `.value` của `stateIn(WhileSubscribed)` toàn ra giá trị ban đầu

`🟡 đã làm app` · `Kotlin`

**Gặp:** trong hàm xử lý nút bấm đọc `state.value` thì luôn được `Loading` / `-1`, dù màn hình đang hiện số thật ở chỗ khác.

**Vì sao:** với `WhileSubscribed`, nguồn chỉ chạy khi **có người collect**. Đọc `.value` không tính là collect; nếu lúc đó không ai
nghe, giá trị là initial (hoặc giá trị cũ còn lưu).

**Sửa:** cần giá trị mới thì `state.first { it !is Loading }` (có collect thật), hoặc dùng `Eagerly` cho state mà logic cần đọc
bất cứ lúc nào.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMyNjogLnZhbHVlIGPhu6dhIHN0YXRlSW4oV2hpbGVTdWJzY3JpYmVkKSBraGkgY2jGsGEgYWkgY29sbGVjdCA9IGdpw6EgdHLhu4sgYmFuIMSR4bqndSwga2jDtG5nIHBo4bqjaSBk4buvIGxp4buHdSB0aOG6rXQuCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuKgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLmZsb3cuKgoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nPFVuaXQ-IHsKICAgIHZhbCBiYXR0ZXJ5ID0gZmxvdyB7IGVtaXQoODcpIH0uc3RhdGVJbih0aGlzLCBTaGFyaW5nU3RhcnRlZC5XaGlsZVN1YnNjcmliZWQoKSwgaW5pdGlhbFZhbHVlID0gLTEpCiAgICBkZWxheSg1MCkKICAgIHByaW50bG4oIuKdjCBjaMawYSBhaSBjb2xsZWN0OiB2YWx1ZSA9ICR7YmF0dGVyeS52YWx1ZX0iKQogICAgcHJpbnRsbigi4pyFIGZpcnN0IHsgfSBrw61jaCBob-G6oXQgbmd14buTbjogJHtiYXR0ZXJ5LmZpcnN0IHsgaXQgIT0gLTEgfX0iKQogICAgY29yb3V0aW5lQ29udGV4dC5jYW5jZWxDaGlsZHJlbigpCn0K)

### K327 · `.catch { }` không bắt lỗi xảy ra trong `collect { }`

`🟢 mới học` · `Kotlin`

**Gặp:** đã có `.catch { }` mà app vẫn crash, stack trace chỉ vào trong khối `collect`.

**Vì sao:** `catch` chỉ bắt lỗi của các bước **phía trên** nó (upstream). Code trong `collect { }` nằm phía dưới.

**Sửa:** chuyển việc có thể ném lỗi (parse, map) lên trước `catch`; giữ `collect` chỉ để hiển thị.

```kotlin
flow.catch { showError(it) }.collect { show(it.toInt()) }           // ❌ toInt lỗi không bị bắt
flow.map { it.toInt() }.catch { showError(it) }.collect { show(it) } // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMyNzogLmNhdGNoIGNo4buJIGLhuq90IGzhu5dpIFBIw41BIFRSw4pOIG7DszsgbOG7l2kgdHJvbmcgY29sbGVjdCB7IH0gduG6q24gYmF5IHJhLgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLioKaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy5mbG93LioKCmZ1biBtYWluKCkgPSBydW5CbG9ja2luZzxVbml0PiB7CiAgICB2YWwgZiA9IGZsb3dPZigiMTIiLCAieCIsICI3IikKICAgIHRyeSB7CiAgICAgICAgZi5jYXRjaCB7IHByaW50bG4oIiAgY2F0Y2g6ICRpdCIpIH0KICAgICAgICAgICAgLmNvbGxlY3QgeyBwcmludGxuKCIgIHPhu5EgJHtpdC50b0ludCgpfSIpIH0gICAgICAgIC8vIOKdjCBOdW1iZXJGb3JtYXRFeGNlcHRpb24g4bufIMSRw6J5LCBjYXRjaCBraMO0bmcgdGjhuqV5CiAgICB9IGNhdGNoIChlOiBOdW1iZXJGb3JtYXRFeGNlcHRpb24pIHsKICAgICAgICBwcmludGxuKCLinYwgbOG7l2kgbOG7jXQgcmEgbmdvw6BpOiAke2UubWVzc2FnZX0iKQogICAgfQogICAgLy8g4pyFIMSRxrBhIHZp4buHYyBjw7MgdGjhu4MgbOG7l2kgbMOqbiB0csaw4bubYyBjYXRjaAogICAgZi5tYXAgeyBpdC50b0ludCgpIH0KICAgICAgICAuY2F0Y2ggeyBwcmludGxuKCIgIOKchSBjYXRjaCBi4bqvdCDEkcaw4bujYzogJHtpdC5tZXNzYWdlfSIpIH0KICAgICAgICAuY29sbGVjdCB7IHByaW50bG4oIiAgc-G7kSAkaXQiKSB9Cn0K) · Xem thêm: K328, K329

### K328 · `Flow exception transparency is violated`

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** `java.lang.IllegalStateException: Flow exception transparency is violated: Previous 'emit' call has thrown exception …, but then emission attempt of value '-1' has been detected. Emissions from 'catch' blocks are prohibited in order to avoid unspecified behaviour, 'Flow.catch' operator can be used instead.`

**Vì sao:** bạn bọc `emit` trong `try/catch` bên trong `flow { }` rồi emit tiếp trong `catch`. Lỗi bị bắt có thể đến từ
**người collect** (emit chạy luôn code của collector), và flow không được phép giấu lỗi của người khác rồi phát tiếp.

**Sửa:** không `try/catch` quanh `emit`. Bọc riêng phần gọi API, hoặc dùng toán tử `.catch { emit(fallback) }`.

```kotlin
flow {
    try { emit(api.load()) } catch (e: Exception) { emit(fallback) }  // ❌
}
flow { emit(api.load()) }.catch { emit(fallback) }                    // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMyODogdHJ5L2NhdGNoIGLhu41jIGVtaXQgcuG7k2kgZW1pdCB0aeG6v3AgLT4gdmkgcGjhuqFtICJleGNlcHRpb24gdHJhbnNwYXJlbmN5Ii4KaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuZmxvdy4qCgpmdW4gbnVtYmVycygpID0gZmxvdyB7CiAgICB0cnkgewogICAgICAgIGVtaXQoMSkKICAgICAgICBlbWl0KDIpCiAgICB9IGNhdGNoIChlOiBFeGNlcHRpb24pIHsKICAgICAgICBlbWl0KC0xKSAgICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIGVtaXQgdHJvbmcgY2F0Y2gKICAgIH0KfQoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nPFVuaXQ-IHsKICAgIG51bWJlcnMoKS5jb2xsZWN0IHsgY2hlY2soaXQgPCAyKSB7ICJjb2xsZWN0b3Iga2jDtG5nIG5o4bqtbiAkaXQiIH07IHByaW50bG4oIiAgbmjhuq1uICRpdCIpIH0KfQo) · Xem thêm: K327

### K329 · Sau `.catch`, flow dừng hẳn, không nhận giá trị mới nữa

`🟡 đã làm app` · `Kotlin`

**Gặp:** lần đầu mất mạng thì hiện lỗi đúng, nhưng có mạng lại thì danh sách không bao giờ cập nhật nữa.

**Vì sao:** một exception làm flow **kết thúc**. `catch` chỉ cho bạn phát thêm giá trị thay thế rồi đóng; nó không "nối lại" nguồn.

**Sửa:** muốn chạy lại thì `retry` (K330). Hoặc bắt lỗi **bên trong** từng lần tải để flow ngoài không bao giờ chết, và biến lỗi
thành một giá trị state:

```kotlin
refreshClicks.map {
    try { State.Ready(api.load()) } catch (e: IOException) { State.Error(e.message) }   // ✅ lỗi thành state
}
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMyOTogc2F1IGtoaSAuY2F0Y2ggYuG6r3QgbOG7l2ksIGZsb3cgS-G6vlQgVEjDmkMsIGtow7RuZyBwaMOhdCB0aeG6v3AgZ2nDoSB0cuG7iyBzYXUgxJHDsy4KaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuZmxvdy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmc8VW5pdD4gewogICAgZmxvdyB7CiAgICAgICAgZW1pdCgxKQogICAgICAgIHRocm93IElsbGVnYWxTdGF0ZUV4Y2VwdGlvbigibeG6pXQga-G6v3QgbuG7kWkiKQogICAgICAgIGVtaXQoMikgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIGtow7RuZyBiYW8gZ2nhu50gdOG7m2kKICAgIH0KICAgICAgICAuY2F0Y2ggeyBwcmludGxuKCIgIGNhdGNoOiAke2l0Lm1lc3NhZ2V9Iik7IGVtaXQoLTEpIH0gICAvLyBwaMOhdCBnacOhIHRy4buLIHRoYXkgdGjhur8gcuG7k2kgaOG6v3QKICAgICAgICAub25Db21wbGV0aW9uIHsgcHJpbnRsbigiICBmbG93IGvhur90IHRow7pjIikgfQogICAgICAgIC5jb2xsZWN0IHsgcHJpbnRsbigiICBuaOG6rW4gJGl0IikgfQp9Cg) · Xem thêm: K330

### K330 · `retry()` không tham số dội server liên tục

`🟡 đã làm app` · `Kotlin`

**Gặp:** server sập 5 phút, log có hàng chục nghìn request từ một máy; pin tụt nhanh.

**Vì sao:** `retry()` mặc định thử lại `Long.MAX_VALUE` lần, **không chờ** giữa các lần.

**Sửa:** giới hạn số lần, chỉ thử lại lỗi tạm thời, và chờ giữa các lần (lambda là `suspend`). `retryWhen` cho biết đang là lần thứ mấy.

```kotlin
api.retry()                                                      // ❌
api.retryWhen { e, attempt ->                                    // ✅ tối đa 3 lần, chỉ lỗi mạng, chờ 1 s, 2 s, 4 s
    if (e is IOException && attempt < 3) { delay(1_000L shl attempt.toInt()); true } else false
}
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMzMDogcmV0cnkoKSBraMO0bmcgdGhhbSBz4buRIHRo4butIGzhuqFpIHThu5tpIExvbmcuTUFYX1ZBTFVFIGzhuqduLCBraMO0bmcgY2jhu50gZ2nhu69hIGPDoWMgbOG6p24uCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuKgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLmZsb3cuKgoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nPFVuaXQ-IHsKICAgIHZhciBjYWxscyA9IDAKICAgIHZhbCBhcGkgPSBmbG93PEludD4geyBjYWxscysrOyBkZWxheSgxKTsgdGhyb3cgUnVudGltZUV4Y2VwdGlvbigiNTAzIikgfSAgIC8vIGRlbGF5KDEpID0gbeG7mXQgcmVxdWVzdCBy4bqldCBuaGFuaAoKICAgIHdpdGhUaW1lb3V0T3JOdWxsKDIwMCkgeyBhcGkucmV0cnkoKS5jb2xsZWN0KCkgfSAgICAgICAgICAvLyDinYwgZOG7mWkgc2VydmVyIGxpw6puIHThu6VjCiAgICBwcmludGxuKCLinYwgcmV0cnkoKTogJGNhbGxzIGzhuqduIGfhu41pIHRyb25nIDIwMCBtcyIpCgogICAgY2FsbHMgPSAwCiAgICBydW5DYXRjaGluZyB7CiAgICAgICAgYXBpLnJldHJ5KDMpIHsgZSAtPiBkZWxheSgxMEwgKiBjYWxscyk7IGUgaXMgUnVudGltZUV4Y2VwdGlvbiB9ICAgLy8g4pyFIGdp4bubaSBo4bqhbiArIGNo4budIHTEg25nIGThuqduCiAgICAgICAgICAgIC5jb2xsZWN0KCkKICAgIH0ub25GYWlsdXJlIHsgcHJpbnRsbigi4pyFIHJldHJ5KDMpOiAkY2FsbHMgbOG6p24gZ-G7jWkgcuG7k2kgYsOhbyBs4buXaSAke2l0Lm1lc3NhZ2V9IikgfQp9Cg)

### K331 · `Flow invariant is violated` khi emit trong `withContext`

`🟡 đã làm app` · `Kotlin`

**Gặp:** `java.lang.IllegalStateException: Flow invariant is violated: Flow was collected in [...], but emission happened in [...]. Please refer to 'flow' documentation or use 'flowOn' instead`

**Vì sao:** `flow { }` bắt buộc `emit` từ đúng coroutine đã collect nó. `withContext(Dispatchers.IO) { emit(x) }` hay `launch { emit(x) }`
bên trong `flow { }` là vi phạm.

**Sửa:** đổi thread bằng `.flowOn(...)`; cần phát từ nhiều coroutine / callback thì dùng `channelFlow { }` hoặc `callbackFlow { }` với `send`.

```kotlin
flow { withContext(Dispatchers.IO) { emit(db.load()) } }        // ❌
flow { emit(db.load()) }.flowOn(Dispatchers.IO)                 // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMzMTogZW1pdCB04burIGNvcm91dGluZS90aHJlYWQga2jDoWMgYsOqbiB0cm9uZyBmbG93IHsgfSAtPiBJbGxlZ2FsU3RhdGVFeGNlcHRpb24uCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuKgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLmZsb3cuKgoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nPFVuaXQ-IHsKICAgIGZsb3cgewogICAgICAgIHdpdGhDb250ZXh0KERpc3BhdGNoZXJzLkRlZmF1bHQpIHsKICAgICAgICAgICAgZW1pdCgxKSAgICAgICAgICAgICAgICAgICAgIC8vIOKdjCBlbWl0IHRyb25nIHdpdGhDb250ZXh0CiAgICAgICAgfQogICAgfS5jb2xsZWN0IHsgcHJpbnRsbihpdCkgfQp9Cg) · Xem thêm: K332

### K332 · `flowOn` đặt sai chỗ, việc nặng vẫn chạy trên main

`🟡 đã làm app` · `Kotlin`

**Gặp:** đã có `.flowOn(Dispatchers.Default)` mà UI vẫn giật khi lọc danh sách lớn.

**Vì sao:** `flowOn` chỉ đổi dispatcher cho các bước **phía trên** nó. Bước nằm **sau** `flowOn` và `collect` vẫn chạy trên thread
của người collect (main).

**Sửa:** đặt `flowOn` ngay **sau** bước nặng cuối cùng.

```kotlin
source.flowOn(Dispatchers.Default).map { heavyFilter(it) }.collect { … }   // ❌ map chạy trên main
source.map { heavyFilter(it) }.flowOn(Dispatchers.Default).collect { … }   // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMzMjogZmxvd09uIMSR4buVaSBkaXNwYXRjaGVyIGNobyBjw6FjIGLGsOG7m2MgUEjDjUEgVFLDik4gbsOzOyBjb2xsZWN0IHbhuqtuIOG7nyB0aHJlYWQgY-G7p2EgbmfGsOG7nWkgZ-G7jWkuCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuKgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLmZsb3cuKgoKZnVuIHQoKSA9IFRocmVhZC5jdXJyZW50VGhyZWFkKCkubmFtZS5zdWJzdHJpbmdCZWZvcmUoIiBAIikKCmZ1biBtYWluKCkgPSBydW5CbG9ja2luZzxVbml0PiB7CiAgICBmbG93IHsgcHJpbnRsbigiICBlbWl0IHRyw6puICR7dCgpfSIpOyBlbWl0KDEpIH0KICAgICAgICAubWFwIHsgcHJpbnRsbigiICBtYXAgMSB0csOqbiAke3QoKX0iKTsgaXQgfQogICAgICAgIC5mbG93T24oRGlzcGF0Y2hlcnMuRGVmYXVsdCkgICAgICAgICAgICAgICAgIC8vIMOhcCBjaG8gZmxvdyB7IH0gdsOgIG1hcCAxCiAgICAgICAgLm1hcCB7IHByaW50bG4oIiAgbWFwIDIgdHLDqm4gJHt0KCl9Iik7IGl0IH0gIC8vIOKdjCBtYXAgMiBLSMOUTkcgbuG6sW0gdHLDqm4gRGVmYXVsdAogICAgICAgIC5jb2xsZWN0IHsgcHJpbnRsbigiICBjb2xsZWN0IHRyw6puICR7dCgpfSIpIH0KfQo) · Xem thêm: [Dispatcher](../concepts/dispatchers.md), K333

### K333 · Query SQLDelight dạng Flow chạy trên main thread

`🟡 đã làm app` · `Kotlin` · `KMP`

**Gặp:** danh sách ghi chú lớn mở chậm, Android StrictMode (nếu bật) báo đọc đĩa trên main; trên iOS UI khựng khi DB thay đổi.

**Vì sao:** `query.asFlow()` chỉ báo "bảng đã đổi"; việc chạy câu SQL nằm ở `mapToList(context)`. Không có dispatcher thì câu SQL chạy
trên thread của người collect, tức là main ở cả hai nền tảng.

**Sửa:** luôn truyền dispatcher nền (inject vào repository để test thay được).

```kotlin
queries.selectAll().asFlow().mapToList(dispatcher)   // ✅ ví dụ Dispatchers.Default / IO
```

**Gặp ở:** [`apps/PocketNotes/shared/src/commonMain/kotlin/dev/pocketnotes/data/NoteRepository.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/shared/src/commonMain/kotlin/dev/pocketnotes/data/NoteRepository.kt)

### K334 · iOS: `callbackFlow` bọc `CLLocationManager` không bao giờ nhận vị trí

`🔴 khó, hiếm nhưng đau` · `iOS` · `KMP`

**Gặp:** Android ghi chuyến bình thường; iOS không lỗi, không crash, chỉ là delegate không bao giờ được gọi.

**Vì sao:** `CLLocationManager` chỉ gọi delegate trên thread có **run loop**. Flow được collect trên `Dispatchers.Default` (thread nền
không có run loop), nên manager tạo trong `callbackFlow` cũng nằm trên thread đó.

**Sửa:** cho phần tạo và dùng manager chạy trên main bằng `.flowOn(Dispatchers.Main)` ngay sau `callbackFlow`.

```kotlin
override fun points(): Flow<TrackPoint> = callbackFlow {
    val manager = CLLocationManager()
    …
    awaitClose { manager.stopUpdatingLocation() }
}.flowOn(Dispatchers.Main)    // ✅ run loop của main
```

**Gặp ở:** [`apps/TripLog/shared/src/iosMain/kotlin/dev/triplog/platform/IosLocationStream.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/shared/src/iosMain/kotlin/dev/triplog/platform/IosLocationStream.kt)

### K335 · `'awaitClose { yourCallbackOrListener.cancel() }' should be used in the end of callbackFlow block`

`🟢 mới học` · `Kotlin`

**Gặp:** `java.lang.IllegalStateException: 'awaitClose { yourCallbackOrListener.cancel() }' should be used in the end of callbackFlow block. Otherwise, a callback/listener may leak in case of external cancellation.`

**Vì sao:** khối `callbackFlow { }` chạy xong ngay sau khi đăng ký listener. Không có `awaitClose`, flow kết thúc mà listener vẫn còn
đó, nên thư viện ném lỗi để bạn khỏi rò listener.

**Sửa:** dòng cuối của `callbackFlow` luôn là `awaitClose { gỡ listener }`. Nếu thoát sớm (không có quyền…), gọi `close(lỗi)` trước
khi `return@callbackFlow`.

```kotlin
fun Sensor.values() = callbackFlow {
    listener = Listener { trySend(it) }
    awaitClose { listener = null }      // ✅
}
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMzNTogY2FsbGJhY2tGbG93IGvhur90IHRow7pjIGto4buRaSBtw6Aga2jDtG5nIGPDsyBhd2FpdENsb3NlIC0-IElsbGVnYWxTdGF0ZUV4Y2VwdGlvbi4KaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuZmxvdy4qCgpmdW4gaW50ZXJmYWNlIExpc3RlbmVyIHsgZnVuIG9uVmFsdWUodjogSW50KSB9CmNsYXNzIFNlbnNvciB7IHZhciBsaXN0ZW5lcjogTGlzdGVuZXI_ID0gbnVsbCB9CgpmdW4gU2Vuc29yLnZhbHVlcygpOiBGbG93PEludD4gPSBjYWxsYmFja0Zsb3cgewogICAgbGlzdGVuZXIgPSBMaXN0ZW5lciB7IHRyeVNlbmQoaXQpIH0KICAgIC8vIOKdjCBxdcOqbiBhd2FpdENsb3NlIHsgbGlzdGVuZXIgPSBudWxsIH0KfQoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nPFVuaXQ-IHsKICAgIFNlbnNvcigpLnZhbHVlcygpLmNvbGxlY3QgeyBwcmludGxuKGl0KSB9Cn0K) · Xem thêm: K336

### K336 · Quên gỡ listener trong `awaitClose` → GPS chạy mãi

`🟡 đã làm app` · `Kotlin` · `Android` · `iOS`

**Gặp:** dừng chuyến rồi mà biểu tượng định vị vẫn sáng; pin tụt dù app ở nền.

**Vì sao:** `awaitClose { }` là chỗ **duy nhất** chạy khi người collect dừng (cancel) hoặc flow đóng. Để trống `awaitClose()` thì
listener của hệ thống vẫn sống tới khi process chết.

**Sửa:** gỡ đúng cái đã đăng ký: `removeUpdates`, `unregisterReceiver`, `stopUpdatingLocation`, `nw_path_monitor_cancel`…

```kotlin
awaitClose { manager.removeUpdates(listener) }   // ✅
```

**Gặp ở:** [`apps/TripLog/shared/src/androidMain/kotlin/dev/triplog/platform/AndroidLocationStream.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/shared/src/androidMain/kotlin/dev/triplog/platform/AndroidLocationStream.kt) ·
[▶ Chạy thử](../playground/index.html#code=Ly8gSzMzNjogYXdhaXRDbG9zZSB7IH0gbMOgIGNo4buXIERVWSBOSOG6pFQgZ-G7oSBsaXN0ZW5lciBraGkgbmfGsOG7nWkgbmdoZSBk4burbmcuCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuKgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLmNoYW5uZWxzLmF3YWl0Q2xvc2UKaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy5mbG93LioKCmZ1biBpbnRlcmZhY2UgTGlzdGVuZXIgeyBmdW4gb25GaXgobWV0ZXJzOiBJbnQpIH0KY2xhc3MgR3BzIHsKICAgIHByaXZhdGUgdmFsIGxpc3RlbmVycyA9IG11dGFibGVMaXN0T2Y8TGlzdGVuZXI-KCkKICAgIGZ1biBhZGQobDogTGlzdGVuZXIpIHsgbGlzdGVuZXJzICs9IGw7IHByaW50bG4oIiAgR1BTIGLhuq10ICgke2xpc3RlbmVycy5zaXplfSBsaXN0ZW5lcikiKSB9CiAgICBmdW4gcmVtb3ZlKGw6IExpc3RlbmVyKSB7IGxpc3RlbmVycyAtPSBsOyBwcmludGxuKCIgIEdQUzogY8OybiAke2xpc3RlbmVycy5zaXplfSBsaXN0ZW5lciIpIH0KICAgIGZ1biB0aWNrKG06IEludCkgPSBsaXN0ZW5lcnMuZm9yRWFjaCB7IGl0Lm9uRml4KG0pIH0KfQoKZnVuIEdwcy5maXhlcygpOiBGbG93PEludD4gPSBjYWxsYmFja0Zsb3cgewogICAgdmFsIGwgPSBMaXN0ZW5lciB7IHRyeVNlbmQoaXQpIH0KICAgIGFkZChsKQogICAgYXdhaXRDbG9zZSB7IHJlbW92ZShsKSB9ICAgICAgICAgIC8vIOKchSBjaOG6oXkga2hpIGNvbGxlY3QgYuG7iyBodeG7tyBob-G6t2MgZmxvdyDEkcOzbmcKfQoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nPFVuaXQ-IHsKICAgIHZhbCBncHMgPSBHcHMoKQogICAgdmFsIHRyaXAgPSBsYXVuY2ggeyBncHMuZml4ZXMoKS5jb2xsZWN0IHsgcHJpbnRsbigiICBuaOG6rW4gxJFp4buDbSBjw6FjaCAkaXQgbSIpIH0gfQogICAgZGVsYXkoMTAwKTsgZ3BzLnRpY2soNSk7IGRlbGF5KDEwMCkKICAgIHRyaXAuY2FuY2VsKCk7IHRyaXAuam9pbigpICAgICAgICAvLyBk4burbmcgY2h1eeG6v24gLT4gYXdhaXRDbG9zZSBn4buhIGxpc3RlbmVyCiAgICBwcmludGxuKCJjaHV54bq_biDEkcOjIGThu6tuZyIpCn0K)

### K337 · `send` trong callback báo "Suspension functions can only be called within coroutine body"

`🟢 mới học` · `Kotlin`

**Gặp:** `Suspension functions can only be called within coroutine body.` ở dòng `send(it)` bên trong listener.

**Vì sao:** `send` là hàm `suspend`. Lambda của listener là hàm thường do hệ thống gọi, không phải coroutine.

**Sửa:** dùng `trySend(it)` (không treo). Nếu sợ đầy buffer thì xem K322.

```kotlin
listener = Listener { send(it) }       // ❌
listener = Listener { trySend(it) }    // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMzNzogc2VuZCBsw6Agc3VzcGVuZCwga2jDtG5nIGfhu41pIMSRxrDhu6NjIHRyb25nIGNhbGxiYWNrIHRoxrDhu51uZy4KLy8gU-G7rWE6IExpc3RlbmVyIHsgdHJ5U2VuZChpdCkgfQppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLmNoYW5uZWxzLmF3YWl0Q2xvc2UKaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy5mbG93LioKCmZ1biBpbnRlcmZhY2UgTGlzdGVuZXIgeyBmdW4gb25WYWx1ZSh2OiBJbnQpIH0KY2xhc3MgU2Vuc29yIHsgdmFyIGxpc3RlbmVyOiBMaXN0ZW5lcj8gPSBudWxsIH0KCmZ1biBTZW5zb3IudmFsdWVzKCk6IEZsb3c8SW50PiA9IGNhbGxiYWNrRmxvdyB7CiAgICBsaXN0ZW5lciA9IExpc3RlbmVyIHsgc2VuZChpdCkgfSAgICAgIC8vIOKdjAogICAgYXdhaXRDbG9zZSB7IGxpc3RlbmVyID0gbnVsbCB9Cn0KCmZ1biBtYWluKCkge30K) · Xem thêm: K322

### K338 · Callback báo cùng một giá trị liên tục, UI vẽ lại vô ích

`🟡 đã làm app` · `Kotlin` · `Android`

**Gặp:** `ACTION_BATTERY_CHANGED` tới liên tục (đổi nhiệt độ, điện áp) dù phần trăm pin không đổi; màn hình nháy, log ngập.

**Vì sao:** `callbackFlow` phát mọi thứ callback đưa vào. Khác `StateFlow`, Flow thường không tự bỏ giá trị trùng.

**Sửa:** chỉ map ra phần app cần, rồi `.distinctUntilChanged()`.

```kotlin
fun observe() = callbackFlow { … }.distinctUntilChanged()   // ✅
```

**Gặp ở:** [`apps/DevicePulse/shared/src/androidMain/kotlin/dev/devicepulse/sensor/AndroidBatterySensor.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/DevicePulse/shared/src/androidMain/kotlin/dev/devicepulse/sensor/AndroidBatterySensor.kt) ·
[▶ Chạy thử](../playground/index.html#code=Ly8gSzMzODogY2FsbGJhY2sgaOG7hyB0aOG7kW5nIGLDoW8gY8O5bmcgbeG7mXQgZ2nDoSB0cuG7iyBuaGnhu4F1IGzhuqduIC0-IGRpc3RpbmN0VW50aWxDaGFuZ2VkLgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLioKaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy5mbG93LioKCmZ1biBtYWluKCkgPSBydW5CbG9ja2luZzxVbml0PiB7CiAgICB2YWwgYnJvYWRjYXN0cyA9IGZsb3dPZig4MCwgODAsIDgwLCA3OSwgNzksIDc4KSAgIC8vIHBpbjogaOG7hyB0aOG7kW5nIGLDoW8gY-G6oyBraGkgbmhp4buHdCDEkeG7mS_EkWnhu4duIMOhcCDEkeG7lWkKICAgIHByaW50KCLinYwga2jDtG5nIGzhu41jOiIpCiAgICBicm9hZGNhc3RzLmNvbGxlY3QgeyBwcmludCgiICRpdCIpIH0KICAgIHByaW50KCJcbuKchSBkaXN0aW5jdFVudGlsQ2hhbmdlZDoiKQogICAgYnJvYWRjYXN0cy5kaXN0aW5jdFVudGlsQ2hhbmdlZCgpLmNvbGxlY3QgeyBwcmludCgiICRpdCIpIH0KICAgIHByaW50bG4oKQp9Cg)

### K339 · `combine` không phát gì cả

`🟢 mới học` · `Kotlin`

**Gặp:** màn hình kẹt ở Loading. Từng nguồn riêng lẻ đều có dữ liệu, chỉ `combine` của chúng là im.

**Vì sao:** `combine` chỉ phát khi **mọi** nguồn đã phát ít nhất một giá trị. Một nguồn chưa từng phát (chờ mạng, chờ quyền) chặn cả cụm.

**Sửa:** cho nguồn chậm một giá trị đầu: `onStart { emit(Unknown) }`, hoặc làm nó thành `StateFlow` có initial value.

```kotlin
combine(battery, network.onStart { emit(NetworkStatus.Unknown) }) { b, n -> … }   // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzMzOTogY29tYmluZSBjaOG7iSBwaMOhdCBraGkgTeG7jEkgbmd14buTbiDEkcOjIGPDsyDDrXQgbmjhuqV0IG3hu5l0IGdpw6EgdHLhu4suCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuKgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLmZsb3cuKgoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nPFVuaXQ-IHsKICAgIHZhbCBiYXR0ZXJ5ID0gZmxvd09mKDgwKQogICAgdmFsIG5ldHdvcmsgPSBmbG93PFN0cmluZz4geyBkZWxheSgxMF8wMDApOyBlbWl0KCJ3aWZpIikgfSAgIC8vIHbDrSBk4bulOiBjaMawYSB04burbmcgcGjDoXQKCiAgICB2YWwgciA9IHdpdGhUaW1lb3V0T3JOdWxsKDIwMCkgeyBjb21iaW5lKGJhdHRlcnksIG5ldHdvcmspIHsgYiwgbiAtPiAiJGIlICRuIiB9LmZpcnN0KCkgfQogICAgcHJpbnRsbigi4p2MIGNvbWJpbmUgc2F1IDIwMCBtczogJHIiKQoKICAgIHZhbCBmaXhlZCA9IGNvbWJpbmUoYmF0dGVyeSwgbmV0d29yay5vblN0YXJ0IHsgZW1pdCgixJFhbmcga2nhu4NtIHRyYeKApiIpIH0pIHsgYiwgbiAtPiAiJGIlICRuIiB9LmZpcnN0KCkKICAgIHByaW50bG4oIuKchSBvblN0YXJ0IGNobyBnacOhIHRy4buLIMSR4bqndTogJGZpeGVkIikKfQo)

### K340 · Nhầm `zip` với `combine`

`🟢 mới học` · `Kotlin`

**Gặp:** dùng `zip(price, currency)`: đổi tiền tệ mà giá không cập nhật cho tới khi giá đổi.

**Vì sao:** `zip` ghép theo **cặp**: phần tử thứ n bên này với phần tử thứ n bên kia, và chờ nhau. `combine` lấy giá trị **mới nhất**
của mỗi bên mỗi khi một bên đổi.

**Sửa:** gộp state cho UI thì gần như luôn là `combine`. `zip` chỉ khi hai nguồn thật sự đi theo cặp (request n ↔ response n).

[▶ Chạy thử](../playground/index.html#code=Ly8gSzM0MDogemlwIGdow6lwIHRoZW8gQ-G6tlAgKDEtMSwgY2jhu50gbmhhdSk7IGNvbWJpbmUgZ2jDqXAgZ2nDoSB0cuG7iyBN4buaSSBOSOG6pFQgY-G7p2EgbeG7l2kgYsOqbi4KaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuZmxvdy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmc8VW5pdD4gewogICAgdmFsIHByaWNlcyA9IGZsb3cgeyBlbWl0KDEwMCk7IGRlbGF5KDMwKTsgZW1pdCgxMTApOyBkZWxheSgzMCk7IGVtaXQoMTIwKSB9CiAgICB2YWwgY3VycmVuY3kgPSBmbG93IHsgZW1pdCgiVVNEIik7IGRlbGF5KDQ1KTsgZW1pdCgiRVVSIikgfQoKICAgIHByaW50KCJ6aXAgICAgOiIpOyBwcmljZXMuemlwKGN1cnJlbmN5KSB7IHAsIGMgLT4gIiRwICRjIiB9LmNvbGxlY3QgeyBwcmludCgiIFskaXRdIikgfQogICAgcHJpbnQoIlxuY29tYmluZToiKTsgY29tYmluZShwcmljZXMsIGN1cnJlbmN5KSB7IHAsIGMgLT4gIiRwICRjIiB9LmNvbGxlY3QgeyBwcmludCgiIFskaXRdIikgfQogICAgcHJpbnRsbigpCn0K)

### K341 · Đổi khoá, `flatMapLatest` còn đưa dữ liệu của khoá cũ một nhịp

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** đổi đồng gốc từ USD sang EUR, trong chốc lát màn hình hiện tỷ giá của USD dưới tên EUR.

**Vì sao:** `combine(base, base.flatMapLatest { observe(it) })`: `base` đổi trước, `combine` phát ngay với bảng **cũ** còn giữ ở nhánh
thứ hai, rồi mới có bảng mới.

**Sửa:** để dữ liệu mang theo khoá của nó và lọc khi ghép.

```kotlin
combine(base, base.flatMapLatest { repository.observe(it) }) { b, table ->
    val current = table?.takeIf { it.base == b }    // ✅ bỏ bảng không khớp
    …
}
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/core/RatesCore.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/core/RatesCore.kt) ·
[▶ Chạy thử](../playground/index.html#code=Ly8gSzM0MTogxJHhu5VpIGtob8OhIC0-IGNvbWJpbmUoa2V5LCBrZXkuZmxhdE1hcExhdGVzdCB7IOKApiB9KSBjw7MgbeG7mXQgbmjhu4twIGTDuW5nIGThu68gbGnhu4d1IGPhu6dhIGtob8OhIEPFqC4KaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuZmxvdy4qCgpkYXRhIGNsYXNzIFRhYmxlKHZhbCBiYXNlOiBTdHJpbmcsIHZhbCByYXRlOiBEb3VibGUpCgpmdW4gb2JzZXJ2ZShiYXNlOiBTdHJpbmcpOiBGbG93PFRhYmxlPiA9IGZsb3cgewogICAgZGVsYXkoMTApICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyDEkeG7jWMgREIgLyBt4bqhbmcKICAgIGVtaXQoVGFibGUoYmFzZSwgaWYgKGJhc2UgPT0gIlVTRCIpIDI1XzAwMC4wIGVsc2UgMjdfMDAwLjApKQp9CgpAT3B0SW4oRXhwZXJpbWVudGFsQ29yb3V0aW5lc0FwaTo6Y2xhc3MpCmZ1biBtYWluKCkgPSBydW5CbG9ja2luZzxVbml0PiB7CiAgICB2YWwgYmFzZSA9IE11dGFibGVTdGF0ZUZsb3coIlVTRCIpCiAgICB2YWwgam9iID0gbGF1bmNoIHsKICAgICAgICBjb21iaW5lKGJhc2UsIGJhc2UuZmxhdE1hcExhdGVzdCB7IG9ic2VydmUoaXQpIH0pIHsgYiwgdCAtPgogICAgICAgICAgICB2YWwgb2sgPSBpZiAodC5iYXNlID09IGIpICLinIUiIGVsc2UgIuKdjCBi4bqjbmcgY-G7p2EgJHt0LmJhc2V9ISIKICAgICAgICAgICAgImJhc2U9JGIgcmF0ZT0ke3QucmF0ZX0gJG9rIgogICAgICAgIH0uY29sbGVjdCB7IHByaW50bG4oIiAgJGl0IikgfQogICAgfQogICAgZGVsYXkoMzAwKQogICAgYmFzZS52YWx1ZSA9ICJFVVIiCiAgICBkZWxheSgzMDApCiAgICBqb2IuY2FuY2VsKCkKfQo)

### K342 · Ô tìm kiếm hiện kết quả cũ chồng lên kết quả mới

`🟡 đã làm app` · `Kotlin`

**Gặp:** gõ "han", danh sách lần lượt nhảy qua kết quả của "h", "ha" rồi mới tới "han"; đôi khi kết quả "ha" tới sau và đè lên.

**Vì sao:** `flatMapConcat` chạy **hết** từng truy vấn theo thứ tự; `flatMapMerge` chạy song song, kết quả về theo thứ tự nào cũng có.

**Sửa:** `flatMapLatest`: chuỗi mới tới thì **huỷ** truy vấn cũ.

```kotlin
query.flatMapLatest { q -> repository.search(q) }   // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzM0Mjogw7QgdMOsbSBraeG6v206IGZsYXRNYXBDb25jYXQgY2jhuqF5IGjhur90IHThu6tuZyB0cnV5IHbhuqVuIGPFqTsgZmxhdE1hcExhdGVzdCBodeG7tyB0cnV5IHbhuqVuIGPFqS4KaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuZmxvdy4qCgpmdW4gc2VhcmNoKHE6IFN0cmluZykgPSBmbG93IHsgZGVsYXkoMTAwKTsgZW1pdCgia-G6v3QgcXXhuqMgY2hvIFwiJHFcIiIpIH0KCmZ1biBtYWluKCkgPSBydW5CbG9ja2luZzxVbml0PiB7CiAgICB2YWwgdHlwaW5nID0gZmxvdyB7IGZvciAocSBpbiBsaXN0T2YoImgiLCAiaGEiLCAiaGFuIikpIHsgZW1pdChxKTsgZGVsYXkoMzApIH0gfQoKICAgIHByaW50bG4oIuKdjCBmbGF0TWFwQ29uY2F0OiIpCiAgICB0eXBpbmcuZmxhdE1hcENvbmNhdCB7IHNlYXJjaChpdCkgfS5jb2xsZWN0IHsgcHJpbnRsbigiICAkaXQiKSB9CiAgICBwcmludGxuKCLinIUgZmxhdE1hcExhdGVzdDoiKQogICAgdHlwaW5nLmZsYXRNYXBMYXRlc3QgeyBzZWFyY2goaXQpIH0uY29sbGVjdCB7IHByaW50bG4oIiAgJGl0IikgfQp9Cg) · Xem thêm: K343, K344

### K343 · `debounce` báo cảnh báo "in a preview state"

`🟢 mới học` · `Kotlin`

**Gặp:** cảnh báo `This declaration is in a preview state and can be changed in a backwards-incompatible manner with a best-effort migration. Its usage should be marked with '@kotlinx.coroutines.FlowPreview' or '@OptIn(kotlinx.coroutines.FlowPreview::class)'`

**Vì sao:** `debounce` và `sample` vẫn được kotlinx.coroutines đánh dấu `@FlowPreview`. Chạy được, chỉ là API có thể đổi.

**Sửa:** thêm `@OptIn(FlowPreview::class)` ở hàm/class dùng nó (hoặc `optIn.add("kotlinx.coroutines.FlowPreview")` trong
`compilerOptions`). Ô tìm kiếm thường là `debounce(300).flatMapLatest { search(it) }`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzM0MzogZGVib3VuY2UgY2jhu50gbmfGsOG7nWkgZMO5bmcgbmfhu6tuZyBnw7UgcuG7k2kgbeG7m2kgcGjDoXQuCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuKgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLmZsb3cuKgoKQE9wdEluKEZsb3dQcmV2aWV3OjpjbGFzcykKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nPFVuaXQ-IHsKICAgIHZhbCB0eXBpbmcgPSBmbG93IHsKICAgICAgICBmb3IgKHEgaW4gbGlzdE9mKCJoIiwgImhhIiwgImhhbiIpKSB7IGVtaXQocSk7IGRlbGF5KDUwKSB9ICAgLy8gZ8O1IG5oYW5oCiAgICAgICAgZGVsYXkoNDAwKQogICAgICAgIGVtaXQoImhhbm9pIikgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8gZ8O1IHRow6ptIHNhdSBt4buZdCBsw7pjCiAgICB9CiAgICB0eXBpbmcuZGVib3VuY2UoMzAwKS5jb2xsZWN0IHsgcHJpbnRsbigiICB0w6xtIFwiJGl0XCIiKSB9Cn0K)

### K344 · `flatMapLatest`, `mapLatest` báo "This declaration needs opt-in"

`🟢 mới học` · `Kotlin` · `Gradle`

**Gặp:** cảnh báo `This declaration needs opt-in. Its usage should be marked with '@kotlinx.coroutines.ExperimentalCoroutinesApi' or '@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)'`

**Vì sao:** `flatMapLatest`, `flatMapConcat`, `flatMapMerge`, `mapLatest` còn mang `@ExperimentalCoroutinesApi`. Dự án bật
"cảnh báo là lỗi" (`allWarningsAsErrors`) thì build đỏ.

**Sửa:** `@OptIn(ExperimentalCoroutinesApi::class)` tại chỗ dùng, hoặc bật một lần cho cả module.

```kotlin
kotlin {
    compilerOptions {
        optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
    }
}
```

**Gặp ở:** [`apps/RateWatch/shared/build.gradle.kts`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/build.gradle.kts)

### K345 · `collectLatest` không lưu đủ dữ liệu

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** ghi điểm GPS bằng `collectLatest { save(it) }`: chuyến lưu xong thiếu gần hết điểm, chỉ còn vài điểm cuối.

**Vì sao:** `collectLatest` **huỷ** khối đang chạy khi giá trị mới tới. Khối chậm hơn nhịp dữ liệu thì phần sau điểm treo (`save`) gần
như không bao giờ chạy xong.

**Sửa:** `collectLatest` chỉ cho việc mà kết quả cũ vô nghĩa (vẽ lại, tìm kiếm). Việc phải làm cho **mọi** giá trị thì `collect`,
và nếu chậm thì gom lô (`chunked` tự viết / buffer rồi ghi một transaction).

[▶ Chạy thử](../playground/index.html#code=Ly8gSzM0NTogY29sbGVjdExhdGVzdCBIVeG7tiBraOG7kWkgY8WpIGtoaSBjw7MgZ2nDoSB0cuG7iyBt4bubaTsgcGjhuqduIHNhdSDEkWnhu4NtIHRyZW8gY8OzIHRo4buDIGtow7RuZyBiYW8gZ2nhu50gY2jhuqF5LgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLioKaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy5mbG93LioKCmZ1biBtYWluKCkgPSBydW5CbG9ja2luZzxVbml0PiB7CiAgICB2YWwgZml4ZXMgPSBmbG93IHsgcmVwZWF0KDQpIHsgZW1pdChpdCk7IGRlbGF5KDIwKSB9IH0KICAgIGZpeGVzLmNvbGxlY3RMYXRlc3QgewogICAgICAgIHByaW50bG4oIiAgYuG6r3QgxJHhuqd1IGzGsHUgxJFp4buDbSAkaXQiKQogICAgICAgIGRlbGF5KDUwKSAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIGdoaSBEQiBjaOG6rW0gaMahbiB04buRYyDEkeG7mSDEkWnhu4NtIHThu5tpCiAgICAgICAgcHJpbnRsbigiICDinIUgxJHDoyBsxrB1IMSRaeG7g20gJGl0IikgICAgICAgICAvLyBjaOG7iSDEkWnhu4NtIGN14buRaSB04bubaSDEkcaw4bujYyBkw7JuZyBuw6B5CiAgICB9Cn0K) · Xem thêm: K346

### K346 · Nguồn và người vẽ chờ nhau, tổng thời gian cộng dồn

`🟡 đã làm app` · `Kotlin`

**Gặp:** đọc cảm biến mất 100 ms, vẽ mất 100 ms, nhưng mỗi giá trị cách nhau 200 ms chứ không phải 100.

**Vì sao:** mặc định nguồn và `collect` chạy **lần lượt** trong một coroutine: `emit` chờ collector xử lý xong mới quay lại nguồn.

**Sửa:** `buffer()` để nguồn chạy tiếp trong coroutine riêng; `conflate()` nếu chỉ cần giá trị mới nhất (bỏ giá trị cũ khi collector chậm).

[▶ Chạy thử](../playground/index.html#code=Ly8gSzM0Njoga2jDtG5nIGPDsyBidWZmZXIsIGVtaXQgdsOgIGNvbGxlY3QgY2jhuqF5IGzhuqduIGzGsOG7o3Q7IGJ1ZmZlcigpIGNobyBjaMO6bmcgY2jhuqF5IHNvbmcgc29uZy4KaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuZmxvdy4qCmltcG9ydCBrb3RsaW4udGltZS5tZWFzdXJlVGltZQoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nPFVuaXQ-IHsKICAgIHZhbCByZWFkaW5ncyA9IGZsb3cgeyByZXBlYXQoNSkgeyBkZWxheSgxMDApOyBlbWl0KGl0KSB9IH0gICAvLyDEkeG7jWMgY-G6o20gYmnhur9uIDEwMCBtcwogICAgdmFsIHQxID0gbWVhc3VyZVRpbWUgeyByZWFkaW5ncy5jb2xsZWN0IHsgZGVsYXkoMTAwKSB9IH0gICAgICAgLy8gduG6vSAxMDAgbXMKICAgIHByaW50bG4oImtow7RuZyBidWZmZXI6ICR7dDEuaW5XaG9sZU1pbGxpc2Vjb25kc30gbXMiKQogICAgdmFsIHQyID0gbWVhc3VyZVRpbWUgeyByZWFkaW5ncy5idWZmZXIoKS5jb2xsZWN0IHsgZGVsYXkoMTAwKSB9IH0KICAgIHByaW50bG4oImJ1ZmZlcigpICAgIDogJHt0Mi5pbldob2xlTWlsbGlzZWNvbmRzfSBtcyIpCiAgICB2YWwgdDMgPSBtZWFzdXJlVGltZSB7IHJlYWRpbmdzLmNvbmZsYXRlKCkuY29sbGVjdCB7IGRlbGF5KDMwMCkgfSB9CiAgICBwcmludGxuKCJjb25mbGF0ZSgpIHbhu5tpIG5nxrDhu51pIHbhur0gcuG6pXQgY2jhuq1tOiAke3QzLmluV2hvbGVNaWxsaXNlY29uZHN9IG1zIChi4buPIGdpw6EgdHLhu4sgY8WpKSIpCn0K) · Xem thêm: K319, K345

### K347 · Huỷ rồi mà `asFlow()` vẫn chạy hết danh sách

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** bấm Huỷ khi đang xử lý `items.asFlow()`, vòng xử lý vẫn chạy tới phần tử cuối rồi mới dừng.

**Vì sao:** để nhanh, `asFlow()` trên list/range và phần lớn toán tử **không tự kiểm** lệnh huỷ giữa các phần tử. Chỉ builder `flow { }`
kiểm ở mỗi `emit`.

**Sửa:** thêm `.cancellable()`, hoặc gọi `ensureActive()` trong vòng xử lý.

```kotlin
items.asFlow().cancellable().collect { process(it) }   // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzM0NzogYXNGbG93KCkgdHLDqm4gbGlzdC9yYW5nZSBraMO0bmcgdOG7sSBraeG7g20gbOG7h25oIGh14bu3OyBmbG93IHsgfSB2w6AgLmNhbmNlbGxhYmxlKCkgdGjDrCBjw7MuCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuKgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLmZsb3cuKgoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nPFVuaXQ-IHsKICAgIGxhdW5jaCB7CiAgICAgICAgKDEuLjUpLmFzRmxvdygpLmNvbGxlY3QgeyB2IC0-IGlmICh2ID09IDMpIGNhbmNlbCgpOyBwcmludGxuKCIgIOKdjCBhc0Zsb3cgbmjhuq1uICR2IikgfQogICAgfS5qb2luKCkKICAgIGxhdW5jaCB7CiAgICAgICAgKDEuLjUpLmFzRmxvdygpLmNhbmNlbGxhYmxlKCkuY29sbGVjdCB7IHYgLT4gaWYgKHYgPT0gMykgY2FuY2VsKCk7IHByaW50bG4oIiAg4pyFIGNhbmNlbGxhYmxlIG5o4bqtbiAkdiIpIH0KICAgIH0uam9pbigpCn0K)

### K348 · `first()` / `single()` ném lỗi

`🟢 mới học` · `Kotlin`

**Gặp:** `java.util.NoSuchElementException: Expected at least one element` khi gọi `first()`; hoặc
`java.lang.IllegalArgumentException: Flow has more than one element` khi gọi `single()`.

**Vì sao:** `first()` cần ít nhất một phần tử, flow rỗng (query không có dòng nào) là lỗi. `single()` cần **đúng một**.

**Sửa:** không chắc có dữ liệu thì `firstOrNull()` / `singleOrNull()`.

```kotlin
val note = repository.observe(id).first()          // ❌ flow rỗng là crash
val note = repository.observe(id).firstOrNull()    // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzM0ODogZmlyc3QoKSB0csOqbiBmbG93IHLhu5duZyBuw6ltIE5vU3VjaEVsZW1lbnRFeGNlcHRpb247IHNpbmdsZSgpIG7DqW0ga2hpIGPDsyBoxqFuIDEgcGjhuqduIHThu60uCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuKgppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLmZsb3cuKgoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nPFVuaXQ-IHsKICAgIHZhbCBub25lID0gZW1wdHlGbG93PFN0cmluZz4oKQogICAgcHJpbnRsbigiZmlyc3RPck51bGw6ICR7bm9uZS5maXJzdE9yTnVsbCgpfSIpICAgICAgICAgICAgICAgICAgICAgICAgLy8g4pyFCiAgICBydW5DYXRjaGluZyB7IG5vbmUuZmlyc3QoKSB9Lm9uRmFpbHVyZSB7IHByaW50bG4oIuKdjCBmaXJzdCgpOiAke2l0OjpjbGFzcy5zaW1wbGVOYW1lfTogJHtpdC5tZXNzYWdlfSIpIH0KICAgIHJ1bkNhdGNoaW5nIHsgZmxvd09mKDEsIDIpLnNpbmdsZSgpIH0ub25GYWlsdXJlIHsgcHJpbnRsbigi4p2MIHNpbmdsZSgpOiAke2l0OjpjbGFzcy5zaW1wbGVOYW1lfTogJHtpdC5tZXNzYWdlfSIpIH0KfQo)

### K349 · `stateFlow.toList()` trong test treo mãi

`🟡 đã làm app` · `Kotlin`

**Gặp:** test đứng tới khi `runTest` báo timeout; IDE cảnh báo `'suspend fun <T> SharedFlow<T>.toList(): List<T>' is deprecated. SharedFlow never completes, so this terminal operation never completes.`

**Vì sao:** `toList()` chờ flow **kết thúc**. `StateFlow` / `SharedFlow` không bao giờ kết thúc.

**Sửa:** lấy đủ số giá trị cần rồi dừng: `take(n).toList()`, `first { … }`, hoặc dùng Turbine (`test { awaitItem() }`).

```kotlin
val all = core.state.toList()                 // ❌ treo
val two = core.state.take(2).toList()         // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzM0OTogdG9MaXN0KCkgdHLDqm4gU3RhdGVGbG93IGtow7RuZyBiYW8gZ2nhu50geG9uZzsgZMO5bmcgdGFrZShuKSBob-G6t2MgZmlyc3QoKS4KaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuZmxvdy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmc8VW5pdD4gewogICAgdmFsIHN0YXRlID0gTXV0YWJsZVN0YXRlRmxvdygxKQogICAgdmFsIHIgPSB3aXRoVGltZW91dE9yTnVsbCgyMDApIHsgc3RhdGUudG9MaXN0KCkgfSAgICAgICAgICAvLyDinYwgdHJlbwogICAgcHJpbnRsbigi4p2MIHRvTGlzdCgpOiAke3IgPzogInbhuqtuIHRyZW8gc2F1IDIwMCBtcyJ9IikKCiAgICB2YWwgZ290ID0gYXN5bmMgeyBzdGF0ZS50YWtlKDIpLnRvTGlzdCgpIH0gICAgICAgICAgICAgICAgICAvLyDinIUgxJHhu6cgMiBnacOhIHRy4buLIHRow6wgZOG7q25nCiAgICBkZWxheSgxMDApOyBzdGF0ZS52YWx1ZSA9IDIKICAgIHByaW50bG4oIuKchSB0YWtlKDIpLnRvTGlzdCgpOiAke2dvdC5hd2FpdCgpfSIpCn0K) · Xem thêm: K303

### K350 · Android: collect trong `lifecycleScope.launch` vẫn chạy khi app ở nền

`🟡 đã làm app` · `Android` · `Compose`

**Gặp:** về màn hình chính rồi mà GPS / cảm biến vẫn chạy, vì `WhileSubscribed` thấy vẫn còn người nghe; đôi khi crash vì cập nhật
View đã bị huỷ.

**Vì sao:** `lifecycleScope.launch { flow.collect { } }` chỉ dừng khi Activity **bị huỷ**, không dừng khi app vào nền (`onStop`).
Trong Compose, `collectAsState()` cũng collect tới khi composable rời màn hình, kể cả khi app ở nền.

**Sửa:** View: `repeatOnLifecycle(Lifecycle.State.STARTED) { flow.collect { } }`. Compose: `collectAsStateWithLifecycle()`
(thư viện `lifecycle-runtime-compose`).

```kotlin
val state by viewModel.state.collectAsState()                  // ❌ vẫn nghe khi ở nền
val state by viewModel.state.collectAsStateWithLifecycle()     // ✅ dừng ở onStop, nối lại ở onStart
```

Xem thêm: K324, K501
