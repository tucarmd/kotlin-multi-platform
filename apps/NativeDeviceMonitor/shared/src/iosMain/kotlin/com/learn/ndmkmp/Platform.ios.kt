package com.learn.ndmkmp

import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970   // ObjC category method → Kotlin extension, needs its own import
import platform.UIKit.UIDevice

actual fun platformName(): String =
    UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

actual fun monotonicMs(): Long = (platform.Foundation.NSProcessInfo.processInfo.systemUptime * 1000).toLong()

// en_US_POSIX: without it a phone set to Thai/Arabic numerals or 12-hour time changes the digits / adds "AM".
private fun formatter(pattern: String) = platform.Foundation.NSDateFormatter().apply {
    dateFormat = pattern
    locale = platform.Foundation.NSLocale(localeIdentifier = "en_US_POSIX")
}

private fun nsDate(epochMs: Long) = NSDate.dateWithTimeIntervalSince1970(epochMs / 1000.0)

actual fun clockText(epochMs: Long): String = formatter("HH:mm:ss.SSS").stringFromDate(nsDate(epochMs))

actual fun isoLocalTimestamp(epochMs: Long): String = formatter("yyyy-MM-dd'T'HH:mm:ssxxx").stringFromDate(nsDate(epochMs))
