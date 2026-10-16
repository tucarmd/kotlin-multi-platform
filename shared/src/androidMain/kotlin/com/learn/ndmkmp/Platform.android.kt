package com.learn.ndmkmp

import android.os.Build

actual fun platformName(): String = "Android ${Build.VERSION.RELEASE}"
