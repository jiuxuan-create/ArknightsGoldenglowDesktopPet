package com.floatingpet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/** 开机自启：如果用户之前开了桌宠，开机后自动恢复 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED && Settings.isShown(context)) {
            val svc = Intent(context, FloatingPetService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(svc)
            } else {
                context.startService(svc)
            }
        }
    }
}
