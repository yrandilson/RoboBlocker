package com.roboblocker.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.roboblocker.MainActivity
import com.roboblocker.R

object NotificationHelper {

    private const val CHANNEL_BLOCKED = "roboblocker_blocked"
    private const val CHANNEL_STATUS  = "roboblocker_status"
    private var notifId = 1000

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_BLOCKED, "Chamadas Bloqueadas",
                NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Notificações quando uma chamada de robô é bloqueada"
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_STATUS, "Status do Serviço",
                NotificationManager.IMPORTANCE_LOW).apply {
                description = "Status do serviço RoboBlocker"
            }
        )
    }

    fun showBlockedCallNotification(
        context: Context,
        number: String,
        reason: String,
        isAi: Boolean = false
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("tab", "logs")
        }
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayNumber = if (number.length > 4) number else "Número oculto"
        val aiTag = if (isAi) " 🤖 IA" else ""

        val notification = NotificationCompat.Builder(context, CHANNEL_BLOCKED)
            .setSmallIcon(R.drawable.ic_shield_block)
            .setContentTitle("Chamada bloqueada$aiTag")
            .setContentText("$displayNumber — $reason")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Número: $displayNumber\nMotivo: $reason"))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(notifId++, notification)
    }
}
