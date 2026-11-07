package com.todopro.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) {
            // Nach Neustart alle gespeicherten Alarme neu registrieren
            val todos = TodoStorage.loadTodos(context)
            val now = System.currentTimeMillis()
            var changed = false
            for (todo in todos) {
                val reminderTime = todo.reminderTime ?: continue
                if (reminderTime <= now) {
                    // Wiederkehrende Erinnerungen, die während des Ausschaltens fällig waren → nächster Termin
                    val next = AlarmScheduler.nextOccurrence(todo, now) ?: continue
                    todo.reminderTime = next
                    changed = true
                }
                AlarmScheduler.scheduleAlarm(context, todo)
            }
            if (changed) TodoStorage.saveTodos(context, todos)
        }
    }
}
