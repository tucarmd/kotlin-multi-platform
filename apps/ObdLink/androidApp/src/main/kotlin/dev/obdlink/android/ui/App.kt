package dev.obdlink.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.obdlink.android.R
import dev.obdlink.model.ConnectionState
import dev.obdlink.model.LiveData
import dev.obdlink.model.ObdState
import dev.obdlink.model.Reading
import dev.obdlink.obd.model.Dongle
import org.koin.compose.viewmodel.koinViewModel

/** [App 5 · B5] The four tabs, in the same order as the iOS TabView. */
enum class Tab(@androidx.annotation.StringRes val title: Int, val icon: ImageVector) {
    Connect(R.string.tab_connect, Icons.Filled.Search),
    Live(R.string.tab_live, Icons.Filled.PlayArrow),
    Codes(R.string.tab_codes, Icons.Filled.Warning),
    Console(R.string.tab_console, Icons.Filled.Edit),
}

/**
 * [App 5 · B5] Entry composable: gets the ViewModel from Koin and turns its StateFlow into Compose state.
 *
 * Why split Route / App: ObdApp takes plain values and lambdas, so @Preview can draw it without Koin or Bluetooth.
 */
@Composable
fun ObdRoute(viewModel: ObdViewModel = koinViewModel()) {
    // collectAsStateWithLifecycle stops collecting while the app is in the background.
    val state by viewModel.state.collectAsStateWithLifecycle()
    val access = rememberBleAccess()
    ObdApp(
        state = state,
        connect = ConnectActions(
            permissionGranted = access.granted,
            bluetoothOn = access.bluetoothOn,
            onAllow = access::requestPermission,
            onTurnOn = access::requestBluetoothOn,
            // PLATFORM: on Android the scan silently finds nothing without the permission, so ask first.
            onScan = { if (access.granted) viewModel.scan() else access.requestPermission() },
            onStopScan = viewModel::stopScan,
            onConnect = viewModel::connect,
            onDisconnect = viewModel::disconnect,
            onReset = viewModel::reset,
        ),
        onStartLive = viewModel::startLive,
        onStopLive = viewModel::stopLive,
        codes = CodesActions(
            onRead = viewModel::readCodes,
            onRequestClear = viewModel::requestClearCodes,
            onConfirmClear = viewModel::confirmClearCodes,
            onCancelClear = viewModel::cancelClearCodes,
        ),
        onSendConsole = viewModel::sendConsole,
        onClearConsole = viewModel::clearConsole,
    )
}

/** [App 5 · B5] Scaffold with a bottom NavigationBar. Draws [state] and fires actions; computes nothing itself. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObdApp(
    state: ObdState,
    connect: ConnectActions,
    onStartLive: () -> Unit,
    onStopLive: () -> Unit,
    codes: CodesActions,
    onSendConsole: (String) -> Unit,
    onClearConsole: () -> Unit,
) {
    val colors = if (isSystemInDarkTheme()) darkColorScheme(primary = Color(0xFF7FD1AE)) else lightColorScheme(primary = Color(0xFF00695C))
    // rememberSaveable: the selected tab survives rotation.
    var tab by rememberSaveable { mutableStateOf(Tab.Connect) }
    MaterialTheme(colorScheme = colors) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.SemiBold) })
            },
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.title)) },
                        )
                    }
                }
            },
        ) { padding ->
            val modifier = Modifier.padding(padding)
            when (tab) {
                Tab.Connect -> ConnectScreen(state, connect, modifier)
                Tab.Live -> LiveScreen(state, onStartLive, onStopLive, modifier)
                Tab.Codes -> CodesScreen(state, codes, modifier)
                Tab.Console -> ConsoleScreen(state, onSendConsole, onClearConsole, modifier)
            }
        }
    }
}

/** Preview with a dongle polling, so the gauges and the status line are visible without hardware. */
@Preview(showBackground = true)
@Composable
private fun ObdAppPreview() {
    val dongle = Dongle(id = "AA:BB:CC:DD:EE:FF", name = "OBDII", rssi = -58)
    ObdApp(
        state = ObdState(
            connection = ConnectionState.Polling(dongle),
            dongles = listOf(dongle),
            live = LiveData(rpm = Reading(1726), speedKmh = Reading(50), coolantC = Reading(83), refreshHz = 4.2),
            adapterVersion = "ELM327 v1.5",
        ),
        connect = ConnectActions(true, true, {}, {}, {}, {}, {}, {}, {}),
        onStartLive = {},
        onStopLive = {},
        codes = CodesActions({}, {}, {}, {}),
        onSendConsole = {},
        onClearConsole = {},
    )
}
