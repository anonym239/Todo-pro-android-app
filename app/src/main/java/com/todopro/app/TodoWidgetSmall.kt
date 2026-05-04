package com.todopro.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Klein-Widget (2x2): Zeigt offene Todo-Anzahl + Streak
 * Tippen öffnet die App
 */
class TodoWidgetSmall : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_small)

            // Daten laden
            val todos = TodoStorage.loadTodos(context)
            val openCount = todos.size
            val streak = TodoStorage.getStreak(context)
            val completedToday = TodoStorage.getCompletedToday(context)

            // Texte setzen
            views.setTextViewText(R.id.widgetTodoCount,
                when (openCount) {
                    0 -> "✅ Alles erledigt!"
                    1 -> "1 offen"
                    else -> "$openCount offen"
                }
            )

            views.setTextViewText(R.id.widgetStreak,
                when {
                    streak >= 7 -> "🔥 $streak Tage"
                    streak >= 3 -> "⭐ $streak Tage"
                    streak >= 1 -> "✨ $streak Tag"
                    else -> "Streak: 0"
                }
            )

            views.setTextViewText(R.id.widgetCompletedToday,
                if (completedToday > 0) "Heute: $completedToday ✓" else "Heute: 0"
            )

            // Nächste Todo anzeigen (erste in der Liste)
            val nextTodo = todos.firstOrNull()
            if (nextTodo != null) {
                val text = if (nextTodo.isPriority && nextTodo.text.startsWith("!"))
                    nextTodo.text.removePrefix("!").trimStart()
                else nextTodo.text
                views.setTextViewText(R.id.widgetNextTodo, "📌 $text")
            } else {
                views.setTextViewText(R.id.widgetNextTodo, "Keine offenen Aufgaben")
            }

            // Klick öffnet die App
            val intent = Intent(context, SplashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
