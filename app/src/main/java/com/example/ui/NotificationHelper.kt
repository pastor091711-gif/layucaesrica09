package com.example.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "stream_rewards_channel"
    private const val CHANNEL_NAME = "StreamRewards Alertas"
    private const val CHANNEL_DESC = "Notificaciones de premios, canjes y rachas de StreamRewards Pro"
    private var isChannelCreated = false

    fun createNotificationChannel(context: Context) {
        if (isChannelCreated) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                lightColor = android.graphics.Color.MAGENTA
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            isChannelCreated = true
            Log.d("NotificationHelper", "Canal de notificaciones creado con éxito.")
        }
    }

    fun showNotification(context: Context, title: String, text: String) {
        try {
            createNotificationChannel(context)

            // Intent to open Main Activity when clicking notification
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )

            // Dynamic modern color for notification elements
            val accentColor = android.graphics.Color.parseColor("#FF2A6D") // Electric Pink

            // Get standard launcher icon as fallback, or use a system-defined icon
            val appIconRes = context.applicationInfo.icon

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(appIconRes)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setColor(accentColor)
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, builder.build())
            Log.d("NotificationHelper", "Notificación nativa enviada: $title - ID: $notificationId")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error enviando notificación: ${e.message}")
        }
    }
}
