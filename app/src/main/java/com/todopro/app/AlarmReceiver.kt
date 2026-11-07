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
        const val CHANNEL_ID = "todopro_reminders_v2"
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

        val notifId = todoId.hashCode()

        // Wiederkehrende Aufgabe → sofort den nächsten Termin planen
        val stored = TodoStorage.loadTodos(context).find { it.id == todoId }
        var bigText = notificationText
        if (stored != null) {
            val next = AlarmScheduler.nextOccurrence(stored)
            if (next != null) {
                stored.reminderTime = next
                TodoStorage.updateTodo(context, stored)
                AlarmScheduler.scheduleAlarm(context, stored)
                val sdf = java.text.SimpleDateFormat("EEEE, HH:mm", java.util.Locale.GERMAN)
                bigText = "$notificationText\n\n🔁 Nächste Erinnerung: ${sdf.format(java.util.Date(next))}"
            }
        }

        fun actionIntent(action: String, code: Int): PendingIntent {
            val i = Intent(context, NotificationActionReceiver::class.java).apply {
                this.action = action
                putExtra(NotificationActionReceiver.EXTRA_TODO_ID, todoId)
                putExtra(NotificationActionReceiver.EXTRA_NOTIF_ID, notifId)
            }
            return PendingIntent.getBroadcast(
                context, code, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSound(soundUri)
            // Starkes Vibrationsmuster wie normale Apps (lang-kurz-lang)
            .setVibrate(longArrayOf(0, 400, 200, 400, 200, 800))
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_LIGHTS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            // Große, klare Knöpfe direkt in der Benachrichtigung
            .addAction(R.drawable.ic_check, "✓ Erledigt",
                actionIntent(NotificationActionReceiver.ACTION_COMPLETE, notifId + 2))
            .addAction(R.drawable.ic_bell, "⏰ In 10 Min nochmal",
                actionIntent(NotificationActionReceiver.ACTION_SNOOZE, notifId + 3))
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
            // Starkes Vibrationsmuster: kurz-lang-kurz-lang-lang
            vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 800)
            setSound(soundUri, audioAttributes)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            enableLights(true)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
