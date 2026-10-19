package com.learn.ndmkmp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.learn.ndmkmp.platform.createAndroidServices
import com.learn.ndmkmp.presentation.AppViewModel

class MainActivity : ComponentActivity() {
    private lateinit var vm: AppViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Before setContent / STARTED: registers the permission + camera launchers (registerForActivityResult rule)
        val services = createAndroidServices(this)
        // `adb shell am start -n com.learn.ndmkmp/.MainActivity --es tab monitor` opens a tab directly (screenshots)
        vm = AppViewModel(services, startTab = intent.getStringExtra("tab"))
        setContent { App(vm) }
    }

    override fun onDestroy() {
        // Same lifetime as the services (they die with this Activity) → stop the ViewModels' coroutines too.
        vm.clear()
        super.onDestroy()
    }
}
