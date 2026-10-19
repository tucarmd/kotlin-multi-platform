package com.learn.ndmkmp.presentation

import com.learn.ndmkmp.AppServices
import com.learn.ndmkmp.core.bluetooth.BluetoothRadioState
import com.learn.ndmkmp.presentation.api.ApiViewModel
import com.learn.ndmkmp.presentation.media.MediaViewModel
import com.learn.ndmkmp.presentation.monitor.MonitorViewModel
import com.learn.ndmkmp.presentation.permission.PermissionViewModel
import com.learn.ndmkmp.presentation.scanner.ScannerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** The 5 tabs, in bottom-bar order. Icons are per platform (Material icon / SF Symbol). */
enum class AppTab(val label: String) {
    PERMISSIONS(AppStrings.TAB_PERMISSIONS),
    SCANNER(AppStrings.TAB_SCANNER),
    MEDIA(AppStrings.TAB_MEDIA),
    API(AppStrings.TAB_API),
    MONITOR(AppStrings.TAB_MONITOR);

    companion object {
        /** `--es tab monitor` (Android) / `-tab monitor` (iOS) → open that tab first (screenshots). Unknown → Permissions. */
        fun fromName(name: String?): AppTab = entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: PERMISSIONS
    }
}

/**
 * The app frame: which tab is open + the 5 tab ViewModels, all built from one [AppServices].
 * One per screen owner — Android `MainActivity`, iOS `AppModel` (Swift) — which calls [clear] when it goes away.
 *
 * ```
 *  AppViewModel
 *   ├─ tab                   selected tab
 *   ├─ permissions ──readiness──► scanner     one "can we scan?" verdict for the whole app
 *   ├─ media · api · monitor
 *   └─ onResume()            back from Settings → re-read permissions
 * ```
 */
class AppViewModel(services: AppServices, startTab: String? = null) : SharedViewModel() {

    private val _tab = MutableStateFlow(AppTab.fromName(startTab))
    val tab: StateFlow<AppTab> = _tab.asStateFlow()

    val permissions = PermissionViewModel(services.permissions)
    val scanner = ScannerViewModel(services.ble, permissions.readiness)
    val media = MediaViewModel(services.media)
    val api = ApiViewModel(services.api)
    val monitor = MonitorViewModel(services.monitor)

    val title: String = AppStrings.APP_TITLE
    val tabs: List<AppTab> = AppTab.entries

    init {
        // Radio switched off mid-session → scan/GATT give no error by themselves, so tell the client to clean up.
        services.permissions.radio.filter { it == BluetoothRadioState.OFF }.onEach { services.ble.onRadioOff() }.launchIn(scope)
    }

    fun selectTab(tab: AppTab) {
        _tab.value = tab
    }

    /** App back in the foreground (Activity onResume / iOS scenePhase .active): no event fires after Settings. */
    fun onResume() = permissions.refresh()

    override fun clear() {
        listOf(permissions, scanner, media, api, monitor).forEach { it.clear() }
        super.clear()
    }
}
