package com.stel.gemmunch.ui

import androidx.compose.runtime.compositionLocalOf
import com.stel.gemmunch.utils.SessionManager

val LocalSessionManager = compositionLocalOf<SessionManager> {
    error("No SessionManager provided")
}