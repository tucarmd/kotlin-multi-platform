package com.learn.ndmkmp

import androidx.compose.ui.window.ComposeUIViewController

/**
 * 🍎 iOS entry into the shared Compose UI: wraps App() in a UIViewController.
 * Swift sees it as `MainViewControllerKt.MainViewController()` (file name + "Kt"), called from ContentView.swift.
 */
fun MainViewController() = ComposeUIViewController { App() }
