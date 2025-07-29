package com.stel.gemmunch.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.time.Instant

/**
 * A singleton provider for a pre-configured Gson instance.
 * This ensures consistent JSON serialization throughout the app.
 */
object GsonProvider {
    val instance: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
            .create()
    }
}