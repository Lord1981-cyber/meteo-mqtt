package com.example.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    private const val CHANNEL_ID = "meteo_notifications"
    private const val CHANNEL_NAME = "Données Météo & Bulletins"
    private const val CHANNEL_DESC = "Notifications d'alertes météo et rapports de bulletins"

    fun showNotification(context: Context, title: String, message: String, notificationId: Int = 1) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Build the notification with nice defaults compatibility
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            // Use Android's system drawables to guarantee compile-time and runtime safety
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Erreur lors de l'envoi de la notification", e)
        }
    }
}
