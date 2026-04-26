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
            for (todo in todos) {
                val reminderTime = todo.reminderTime
                if (reminderTime != null && reminderTime > now) {
                    AlarmScheduler.scheduleAlarm(context, todo)
                }
            }
        }
    }
}
