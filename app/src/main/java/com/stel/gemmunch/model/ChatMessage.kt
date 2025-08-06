package com.stel.gemmunch.model

import java.time.Instant

data class ChatMessage(
    val id: String = Instant.now().toEpochMilli().toString(),
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Instant = Instant.now(),
    val imagePath: String? = null,
    val isStreaming: Boolean = false
)