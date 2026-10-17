package com.nexvoice.app.network

import android.os.Build

/**
 * NetworkConfig.kt
 *
 * Automatically detects whether the app is running on an Android Emulator
 * or a physical phone:
 *
 *   - Emulator      → http://10.0.2.2:5000/
 *   - Physical Phone → http://127.0.0.1:5000/ (via USB reverse proxy)
 */
object NetworkConfig {

    private val isEmulator: Boolean by lazy {
        (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("emulator")
    }

    val BASE_URL: String
        get() = if (isEmulator) {
            "http://10.0.2.2:5000/"
        } else {
            "http://127.0.0.1:5000/"
        }
}
