package com.todopro.app

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Empfängt die Knöpfe aus der Benachrichtigung:
 *  - "Erledigt"  → Aufgabe abhaken (wiederkehrende Aufgaben bleiben in der Liste)
 *  - "In 10 Min" → einmalig später nochmal erinnern
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COMPLETE = "com.todopro.app.ACTION_COMPLETE_TODO"
        const val ACTION_SNOOZE = "com.todopro.app.ACTION_SNOOZE_TODO"
        const val EXTRA_TODO_ID = "todo_id"
        const val EXTRA_NOTIF_ID = "notif_id"
        const val SNOOZE_MINUTES = 10
    }

    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getStringExtra(EXTRA_TODO_ID) ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 0)
        val todos = TodoStorage.loadTodos(context)
        val todo = todos.find { it.id == todoId }

        when (intent.action) {
            ACTION_SNOOZE -> {
                if (todo != null) AlarmScheduler.scheduleSnooze(context, todo, SNOOZE_MINUTES)
            }
            ACTION_COMPLETE -> {
                if (todo != null) {
                    if (AlarmScheduler.repeatLabel(todo) != null) {
                        // Wiederkehrend: bleibt in der Liste, nächster Alarm ist schon geplant
                        TodoStorage.recordTodoCompleted(context)
                    } else {
                        AlarmScheduler.cancelAlarm(context, todo)
                        todos.remove(todo)
                        TodoStorage.saveTodos(context, todos)
                        val completedTodos = TodoStorage.loadCompletedTodos(context)
                        completedTodos.add(todo.copy(isCompleted = true, completedAt = System.currentTimeMillis()))
                        TodoStorage.saveCompletedTodos(context, completedTodos)
                        TodoStorage.recordTodoCompleted(context)
                    }
                    TodoStorage.recordDayCount(context, TodoStorage.getCompletedToday(context))
                    TodoStorage.checkAndRecordGoalReached(context)
                }
            }
            else -> return
        }

        // Benachrichtigung schließen
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(notifId)

        // Widgets aktualisieren
        try {
            val awm = android.appwidget.AppWidgetManager.getInstance(context)
            awm.getAppWidgetIds(android.content.ComponentName(context, TodoWidgetSmall::class.java))
                .forEach { TodoWidgetSmall.updateWidget(context, awm, it) }
            awm.getAppWidgetIds(android.content.ComponentName(context, TodoWidgetLarge::class.java))
                .forEach { TodoWidgetLarge.updateWidget(context, awm, it) }
        } catch (_: Exception) { /* ignore */ }
    }
}
