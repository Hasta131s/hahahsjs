package com.example.spam

import kotlinx.coroutines.flow.MutableStateFlow

object SpamState {
    val targetX = MutableStateFlow<Float?>(null)
    val targetY = MutableStateFlow<Float?>(null)
    val spamText = MutableStateFlow("Spam Mesajı!")
    val spamCount = MutableStateFlow(10)
    val spamIntervalMs = MutableStateFlow(1000L) // Milisaniye cinsinden hız
    
    val isRunning = MutableStateFlow(false)
    val isMarkingMode = MutableStateFlow(false)
    val countdownSeconds = MutableStateFlow(0)
    
    val isServiceRunning = MutableStateFlow(false)
    val isOverlayVisible = MutableStateFlow(false)
}
