package com.example.smartswine.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("smartswine_settings", Context.MODE_PRIVATE)
            val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
            if (notificationsEnabled) {
                NotificationWorker.schedule(context)
            }
        }
    }
}
