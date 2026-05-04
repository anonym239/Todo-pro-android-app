package com.todopro.app

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Empfängt die "Erledigt"-Action aus der Benachrichtigung.
 * Markiert die Todo als erledigt und schließt die Notification – ohne App zu öffnen.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COMPLETE = "com.todopro.app.ACTION_COMPLETE_TODO"
        const val EXTRA_TODO_ID = "todo_id"
        const val EXTRA_NOTIF_ID = "notif_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_COMPLETE) return

        val todoId = intent.getStringExtra(EXTRA_TODO_ID) ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 0)

        // Todo aus der Liste laden, als erledigt markieren und speichern
        val todos = TodoStorage.loadTodos(context)
        val todo = todos.find { it.id == todoId }

        if (todo != null) {
            // Alarm canceln
            AlarmScheduler.cancelAlarm(context, todo)

            // Aus offenen Todos entfernen
            todos.remove(todo)
            TodoStorage.saveTodos(context, todos)

            // In erledigte Todos verschieben
            val completedTodos = TodoStorage.loadCompletedTodos(context)
            val completedTodo = todo.copy(
                isCompleted = true,
                completedAt = System.currentTimeMillis()
            )
            completedTodos.add(completedTodo)
            TodoStorage.saveCompletedTodos(context, completedTodos)

            // Statistiken aktualisieren
            TodoStorage.recordTodoCompleted(context)
            TodoStorage.recordDayCount(context, TodoStorage.getCompletedToday(context))
            TodoStorage.checkAndRecordGoalReached(context)
        }

        // Benachrichtigung schließen
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(notifId)

        // Widget aktualisieren
        try {
            val awm = android.appwidget.AppWidgetManager.getInstance(context)
            val smallIds = awm.getAppWidgetIds(
                android.content.ComponentName(context, TodoWidgetSmall::class.java)
            )
            for (id in smallIds) TodoWidgetSmall.updateWidget(context, awm, id)
            val largeIds = awm.getAppWidgetIds(
                android.content.ComponentName(context, TodoWidgetLarge::class.java)
            )
            for (id in largeIds) TodoWidgetLarge.updateWidget(context, awm, id)
        } catch (e: Exception) { /* ignore */ }
    }
}
