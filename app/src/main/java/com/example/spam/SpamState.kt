package com.example.spam

import kotlinx.coroutines.flow.MutableStateFlow

object SpamState {
    val targetX1 = MutableStateFlow<Float?>(null)
    val targetY1 = MutableStateFlow<Float?>(null)
    val targetX2 = MutableStateFlow<Float?>(null)
    val targetY2 = MutableStateFlow<Float?>(null)
    val targetX3 = MutableStateFlow<Float?>(null)
    val targetY3 = MutableStateFlow<Float?>(null)
    val activePointsCount = MutableStateFlow(1) // 1, 2, veya 3 noktalı dizilim
    val currentMarkingPointIndex = MutableStateFlow(1) // Şu an işaretlenen nokta

    // Deprecated backward-compatibility aliases for targetX/Y (points to target1)
    val targetX = targetX1
    val targetY = targetY1

    val spamText = MutableStateFlow("Spam Mesajı!")
    val spamTextList = MutableStateFlow(listOf("Spam Mesajı 1!", "Farklı bir spam varyasyonu!", "Sistem otomatik gönderiyor."))
    val isRandomMode = MutableStateFlow(false)

    val spamCount = MutableStateFlow(10)
    val spamIntervalMs = MutableStateFlow(1000L) // Milisaniye cinsinden hız
    
    val isRunning = MutableStateFlow(false)
    val isMarkingMode = MutableStateFlow(false)
    val countdownSeconds = MutableStateFlow(0)
    
    val isServiceRunning = MutableStateFlow(false)
    val isOverlayVisible = MutableStateFlow(false)
    val isMinimized = MutableStateFlow(false) // Küçültülmüş yüzen balon modu
}
