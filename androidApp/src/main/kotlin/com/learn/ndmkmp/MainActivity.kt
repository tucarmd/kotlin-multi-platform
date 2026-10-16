package com.learn.ndmkmp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.learn.ndmkmp.platform.createAndroidServices

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Before setContent / STARTED: registers the permission + camera launchers (registerForActivityResult rule)
        val services = createAndroidServices(this)
        setContent {
            // `adb shell am start -n com.learn.ndmkmp/.MainActivity --es tab monitor` opens a tab directly (screenshots)
            App(startTab = intent.getStringExtra("tab"), services = services)
        }
    }
}
