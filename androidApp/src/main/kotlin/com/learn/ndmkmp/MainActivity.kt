package com.learn.ndmkmp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // `adb shell am start -n com.learn.ndmkmp/.MainActivity --es tab monitor` opens a tab directly (screenshots)
            App(startTab = intent.getStringExtra("tab"))
        }
    }
}
