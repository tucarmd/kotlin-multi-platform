package com.learn.ndmkmp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.learn.ndmkmp.presentation.AppTab
import com.learn.ndmkmp.presentation.AppViewModel
import com.learn.ndmkmp.ui.api.ApiScreen
import com.learn.ndmkmp.ui.media.MediaScreen
import com.learn.ndmkmp.ui.monitor.MonitorScreen
import com.learn.ndmkmp.ui.permission.PermissionScreen
import com.learn.ndmkmp.ui.scanner.ScannerScreen
import com.learn.ndmkmp.ui.theme.AppColor
import com.learn.ndmkmp.ui.theme.AppTheme

/** Material icon per tab — the icon is a View choice (iOS uses SF Symbols), the tab list comes from [AppTab]. */
private val AppTab.icon: ImageVector
    get() = when (this) {
        AppTab.PERMISSIONS -> Icons.Filled.Lock
        AppTab.SCANNER -> Icons.Filled.Search
        AppTab.MEDIA -> Icons.Filled.Face
        AppTab.API -> Icons.AutoMirrored.Filled.Send
        AppTab.MONITOR -> Icons.Filled.DateRange
    }

/**
 * The app frame. Reads [AppViewModel] (shared) and draws one of the 5 screens.
 *
 * ```
 *  Scaffold
 *   ├─ topBar     CenterAlignedTopAppBar "Native Device Monitor" (CarMD blue)
 *   ├─ content    one of the 5 screens, each with its own ViewModel
 *   └─ bottomBar  NavigationBar: Permissions · Scanner · Media · API · Monitor
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(vm: AppViewModel) {
    val tab by vm.tab.collectAsState()

    // Back from Settings / another app: no event fires for permission changes → re-read on every resume.
    LifecycleResumeEffect(vm) {
        vm.onResume()
        onPauseOrDispose { }
    }

    AppTheme {
        Scaffold(
            containerColor = AppColor.contentBackground,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(vm.title) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColor.primary, titleContentColor = Color.White),
                )
            },
            bottomBar = {
                NavigationBar(containerColor = AppColor.surface) {
                    vm.tabs.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { vm.selectTab(t) },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(t.label, maxLines = 1, softWrap = false, fontSize = 11.sp) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (tab) {
                    AppTab.PERMISSIONS -> PermissionScreen(vm.permissions)
                    AppTab.SCANNER -> ScannerScreen(vm.scanner, onOpenPermissions = { vm.selectTab(AppTab.PERMISSIONS) })
                    AppTab.MEDIA -> MediaScreen(vm.media)
                    AppTab.API -> ApiScreen(vm.api)
                    AppTab.MONITOR -> MonitorScreen(vm.monitor)
                }
            }
        }
    }
}
