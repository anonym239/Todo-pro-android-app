package com.todopro.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import java.util.Calendar

class DailySummaryReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "todopro_daily"
        const val NOTIF_ID = 9999

        fun schedule(context: Context) {
            if (!TodoStorage.isDailySummaryEnabled(context)) return

            val hour = TodoStorage.getDailySummaryHour(context)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, DailySummaryReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, NOTIF_ID, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }

        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, DailySummaryReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, NOTIF_ID, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!TodoStorage.isDailySummaryEnabled(context)) return
        if (!TodoStorage.isNotificationsEnabled(context)) return

        val todos = TodoStorage.loadTodos(context)
        val openCount = todos.size
        val streak = TodoStorage.getStreak(context)
        val completedToday = TodoStorage.getCompletedToday(context)

        createChannel(context)

        val title = when {
            streak >= 7 -> "🔥 $streak Tage Streak! Weiter so!"
            streak >= 3 -> "⭐ $streak Tage am Stück aktiv!"
            else -> "📋 Dein täglicher Überblick"
        }

        val body = buildString {
            if (openCount > 0) append("📌 $openCount offene Aufgabe${if (openCount != 1) "n" else ""}")
            if (completedToday > 0) {
                if (isNotEmpty()) append(" • ")
                append("✅ $completedToday heute erledigt")
            }
            if (isEmpty()) append("Keine offenen Aufgaben – super gemacht! 🎉")
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, notification)
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Tägliche Zusammenfassung",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Tägliche Übersicht deiner Aufgaben"
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }
}
