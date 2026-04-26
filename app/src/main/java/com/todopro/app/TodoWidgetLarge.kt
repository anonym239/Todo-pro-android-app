package com.todopro.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Groß-Widget (4x3): Zeigt die ersten 5 Todos als Liste
 * Tippen auf Todo öffnet die App
 */
class TodoWidgetLarge : AppWidgetProvider() {

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
            val views = RemoteViews(context.packageName, R.layout.widget_large)

            val todos = TodoStorage.loadTodos(context)
            val openCount = todos.size
            val streak = TodoStorage.getStreak(context)

            // Header
            views.setTextViewText(R.id.widgetLargeTitle, "TODOPRO")
            views.setTextViewText(R.id.widgetLargeCount,
                when (openCount) {
                    0 -> "✅ Alles erledigt!"
                    1 -> "1 Aufgabe offen"
                    else -> "$openCount Aufgaben offen"
                }
            )
            views.setTextViewText(R.id.widgetLargeStreak,
                when {
                    streak >= 1 -> "🔥 $streak"
                    else -> ""
                }
            )

            // Bis zu 5 Todos anzeigen
            val todoViews = listOf(
                R.id.widgetTodo1, R.id.widgetTodo2, R.id.widgetTodo3,
                R.id.widgetTodo4, R.id.widgetTodo5
            )
            val todoDots = listOf(
                R.id.widgetDot1, R.id.widgetDot2, R.id.widgetDot3,
                R.id.widgetDot4, R.id.widgetDot5
            )
            val todoRows = listOf(
                R.id.widgetRow1, R.id.widgetRow2, R.id.widgetRow3,
                R.id.widgetRow4, R.id.widgetRow5
            )

            for (i in todoViews.indices) {
                val todo = todos.getOrNull(i)
                if (todo != null) {
                    val text = if (todo.isPriority && todo.text.startsWith("!"))
                        todo.text.removePrefix("!").trimStart()
                    else todo.text

                    views.setTextViewText(todoViews[i], text)
                    // Priorität → roter Punkt, sonst grüner Punkt
                    views.setTextViewText(todoDots[i], if (todo.isPriority) "🔴" else "🟢")
                    views.setInt(todoRows[i], "setVisibility", android.view.View.VISIBLE)
                } else {
                    views.setInt(todoRows[i], "setVisibility", android.view.View.GONE)
                }
            }

            // Mehr-Anzeige wenn > 5 Todos
            if (openCount > 5) {
                views.setTextViewText(R.id.widgetMoreCount, "+${openCount - 5} weitere")
                views.setInt(R.id.widgetMoreCount, "setVisibility", android.view.View.VISIBLE)
            } else {
                views.setInt(R.id.widgetMoreCount, "setVisibility", android.view.View.GONE)
            }

            // Klick öffnet die App
            val intent = Intent(context, SplashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetLargeRoot, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
