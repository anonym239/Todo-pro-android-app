package com.todopro.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AlarmScheduler {

    /** Separater Request-Code für "Später erinnern", damit wiederkehrende Alarme erhalten bleiben */
    private fun snoozeCode(todo: TodoItem) = todo.id.hashCode() + 7

    private fun buildIntent(context: Context, todo: TodoItem) =
        Intent(context, AlarmReceiver::class.java).apply {
            action = "com.todopro.app.ALARM_TRIGGER"
            putExtra(AlarmReceiver.EXTRA_TODO_ID, todo.id)
            putExtra(AlarmReceiver.EXTRA_TODO_TEXT, todo.text)
            putExtra(AlarmReceiver.EXTRA_IS_PRIORITY, todo.isPriority)
        }

    private fun setAt(context: Context, todo: TodoItem, time: Long, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, buildIntent(context, todo),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // setAlarmClock ist die zuverlässigste Methode - funktioniert auch im Doze-Modus
        try {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(time, pendingIntent), pendingIntent)
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
        }
    }

    fun scheduleAlarm(context: Context, todo: TodoItem) {
        val reminderTime = todo.reminderTime ?: return
        if (reminderTime <= System.currentTimeMillis()) return
        setAt(context, todo, reminderTime, todo.id.hashCode())
    }

    /** Einmalig in X Minuten nochmal erinnern */
    fun scheduleSnooze(context: Context, todo: TodoItem, minutes: Int) {
        setAt(context, todo, System.currentTimeMillis() + minutes * 60_000L, snoozeCode(todo))
    }

    fun cancelAlarm(context: Context, todo: TodoItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (code in intArrayOf(todo.id.hashCode(), snoozeCode(todo))) {
            val pendingIntent = PendingIntent.getBroadcast(
                context, code,
                Intent(context, AlarmReceiver::class.java).apply { action = "com.todopro.app.ALARM_TRIGGER" },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Für wiederkehrende Aufgaben: nächster Termin in der Zukunft.
     * Gibt null zurück, wenn die Aufgabe nicht wiederholt wird.
     */
    fun nextOccurrence(todo: TodoItem, after: Long = System.currentTimeMillis()): Long? {
        val start = todo.reminderTime ?: return null
        if (!todo.isRecurring || todo.recurringDays <= 0) return null
        val cal = Calendar.getInstance().apply { timeInMillis = start }
        while (cal.timeInMillis <= after) cal.add(Calendar.DAY_OF_YEAR, todo.recurringDays)
        return cal.timeInMillis
    }

    fun repeatLabel(todo: TodoItem): String? {
        if (!todo.isRecurring || todo.recurringDays <= 0) return null
        return when (todo.recurringDays) {
            1 -> "täglich"
            7 -> "wöchentlich"
            else -> "alle ${todo.recurringDays} Tage"
        }
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }
}
