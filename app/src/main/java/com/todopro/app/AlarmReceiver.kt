package com.todopro.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "todopro_reminders"
        const val EXTRA_TODO_ID = "todo_id"
        const val EXTRA_TODO_TEXT = "todo_text"
        const val EXTRA_IS_PRIORITY = "is_priority"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getStringExtra(EXTRA_TODO_ID) ?: return
        val todoText = intent.getStringExtra(EXTRA_TODO_TEXT) ?: "Erinnerung"
        val isPriority = intent.getBooleanExtra(EXTRA_IS_PRIORITY, false)

        if (!TodoStorage.isNotificationsEnabled(context)) return

        createNotificationChannel(context)

        // Tippen auf Notification → App öffnen
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_TODO_ID, todoId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, todoId.hashCode(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Text bereinigen: ! am Anfang entfernen für Anzeige
        val cleanText = if (todoText.startsWith("!")) todoText.removePrefix("!").trimStart() else todoText

        // Für Prio-Todos: 🚨Text🚨 in der Benachrichtigung
        val notificationText = if (isPriority) "🚨$cleanText🚨" else cleanText
        val notificationTitle = if (isPriority) "🚨 Wichtige Erinnerung!" else "⏰ TodoPro Erinnerung"

        // Zähler erhöhen – Warnung nur bei 1. und 2. Benachrichtigung
        val notifCount = TodoStorage.incrementNotificationCount(context)
        val bigText = if (notifCount <= 2) {
            "$notificationText\n\n⚠️ Achtung: Nach dem Wegwischen wird diese Todo automatisch als erledigt markiert!"
        } else {
            notificationText
        }

        val notifId = todoId.hashCode()

        // Wegwischen der Notification → Todo automatisch als erledigt markieren (deleteIntent)
        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_COMPLETE
            putExtra(NotificationActionReceiver.EXTRA_TODO_ID, todoId)
            putExtra(NotificationActionReceiver.EXTRA_NOTIF_ID, notifId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, notifId + 2, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            // Wenn Notification weggewischt wird → Todo erledigen
            .setDeleteIntent(dismissPendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notifId, notification)
    }

    private fun createNotificationChannel(context: Context) {
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID,
            "TodoPro Erinnerungen",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Benachrichtigungen für TodoPro Aufgaben-Erinnerungen"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500)
            setSound(soundUri, audioAttributes)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
