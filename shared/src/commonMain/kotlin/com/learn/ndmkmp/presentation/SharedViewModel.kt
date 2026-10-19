package com.learn.ndmkmp.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * R1 — base of the 5 tab ViewModels. MVVM: the View (Compose on Android, SwiftUI on iOS) reads `state` and calls
 * action functions; every `if` about what to show lives in the ViewModel → written once, used by both UIs.
 *
 * Why not `androidx.lifecycle.ViewModel`: the services the ViewModels wrap (permission launchers, BLE client) are
 * built per Activity and die with it, so a ViewModel that outlives the Activity would hold dead services. Here the
 * owner creates the ViewModels next to the services and calls [clear] when it goes away:
 * Android `MainActivity.onDestroy`, iOS `AppModel.deinit` (Swift).
 *
 * `Dispatchers.Main.immediate`: state is written on the main thread → Compose and SwiftUI can read it directly.
 */
abstract class SharedViewModel {
    protected val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Stop every coroutine this ViewModel started. Forgetting it on iOS = loops running forever. */
    open fun clear() {
        scope.cancel()
    }
}
