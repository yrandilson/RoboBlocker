package com.roboblocker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i("RoboBlocker", "Boot complete – service will be registered via CallScreeningService")
            // CallScreeningService is bound by the system automatically.
            // No manual start needed. Just ensure settings are preserved.
        }
    }
}
