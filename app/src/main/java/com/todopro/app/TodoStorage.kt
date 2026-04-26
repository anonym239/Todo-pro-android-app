package com.todopro.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

object TodoStorage {
    private const val PREFS_NAME = "todopro_prefs"
    private const val KEY_TODOS = "todos"
    private const val KEY_COMPLETED_TODOS = "completed_todos"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_COMPLIMENTS = "compliments_enabled"
    private const val KEY_KONFETTI = "konfetti_enabled"
    private const val KEY_NOTIFICATIONS = "notifications_enabled"
    private const val KEY_STREAK = "streak_count"
    private const val KEY_STREAK_LAST_DATE = "streak_last_date"
    private const val KEY_TOTAL_COMPLETED = "total_completed"
    private const val KEY_DAILY_SUMMARY = "daily_summary_enabled"
    private const val KEY_DAILY_SUMMARY_HOUR = "daily_summary_hour"
    private const val KEY_COMPLETED_TODAY = "completed_today"
    private const val KEY_COMPLETED_TODAY_DATE = "completed_today_date"

    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ── Todos ──────────────────────────────────────────────────────────────

    fun saveTodos(context: Context, todos: List<TodoItem>) {
        val json = gson.toJson(todos)
        getPrefs(context).edit().putString(KEY_TODOS, json).apply()
    }

    fun loadTodos(context: Context): MutableList<TodoItem> {
        val json = getPrefs(context).getString(KEY_TODOS, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<TodoItem>>() {}.type
        return try {
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    // ── Erledigte Todos ────────────────────────────────────────────────────

    fun saveCompletedTodos(context: Context, todos: List<TodoItem>) {
        // Nur die letzten 50 erledigten Todos behalten
        val limited = todos.takeLast(50)
        val json = gson.toJson(limited)
        getPrefs(context).edit().putString(KEY_COMPLETED_TODOS, json).apply()
    }

    fun loadCompletedTodos(context: Context): MutableList<TodoItem> {
        val json = getPrefs(context).getString(KEY_COMPLETED_TODOS, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<TodoItem>>() {}.type
        return try {
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    // ── Streak & Statistik ─────────────────────────────────────────────────

    fun recordTodoCompleted(context: Context) {
        val prefs = getPrefs(context)
        val today = getTodayString()

        // Heute erledigte Todos zählen
        val lastDate = prefs.getString(KEY_COMPLETED_TODAY_DATE, "") ?: ""
        val completedToday = if (lastDate == today) {
            prefs.getInt(KEY_COMPLETED_TODAY, 0) + 1
        } else {
            1
        }
        prefs.edit()
            .putString(KEY_COMPLETED_TODAY_DATE, today)
            .putInt(KEY_COMPLETED_TODAY, completedToday)
            .apply()

        // Gesamt-Counter
        val total = prefs.getInt(KEY_TOTAL_COMPLETED, 0) + 1
        prefs.edit().putInt(KEY_TOTAL_COMPLETED, total).apply()

        // Streak aktualisieren
        updateStreak(context)
    }

    private fun updateStreak(context: Context) {
        val prefs = getPrefs(context)
        val today = getTodayString()
        val yesterday = getYesterdayString()
        val lastDate = prefs.getString(KEY_STREAK_LAST_DATE, "") ?: ""
        val currentStreak = prefs.getInt(KEY_STREAK, 0)

        val newStreak = when (lastDate) {
            today -> currentStreak // Heute schon gezählt
            yesterday -> currentStreak + 1 // Gestern aktiv → Streak verlängern
            "" -> 1 // Erster Tag
            else -> 1 // Streak unterbrochen → neu starten
        }

        prefs.edit()
            .putInt(KEY_STREAK, newStreak)
            .putString(KEY_STREAK_LAST_DATE, today)
            .apply()
    }

    fun getStreak(context: Context): Int {
        val prefs = getPrefs(context)
        val lastDate = prefs.getString(KEY_STREAK_LAST_DATE, "") ?: ""
        val today = getTodayString()
        val yesterday = getYesterdayString()
        // Streak ist nur gültig wenn heute oder gestern aktiv
        return if (lastDate == today || lastDate == yesterday) {
            prefs.getInt(KEY_STREAK, 0)
        } else {
            0
        }
    }

    fun getTotalCompleted(context: Context): Int {
        return getPrefs(context).getInt(KEY_TOTAL_COMPLETED, 0)
    }

    fun getCompletedToday(context: Context): Int {
        val prefs = getPrefs(context)
        val today = getTodayString()
        val lastDate = prefs.getString(KEY_COMPLETED_TODAY_DATE, "") ?: ""
        return if (lastDate == today) prefs.getInt(KEY_COMPLETED_TODAY, 0) else 0
    }

    private fun getTodayString(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }

    private fun getYesterdayString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }

    // ── Einstellungen ──────────────────────────────────────────────────────

    fun isDarkMode(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DARK_MODE, true)
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun isComplimentsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_COMPLIMENTS, false)
    }

    fun setComplimentsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_COMPLIMENTS, enabled).apply()
    }

    fun isKonfettiEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_KONFETTI, true)
    }

    fun setKonfettiEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_KONFETTI, enabled).apply()
    }

    fun isNotificationsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIFICATIONS, true)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }

    fun isDailySummaryEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DAILY_SUMMARY, false)
    }

    fun setDailySummaryEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DAILY_SUMMARY, enabled).apply()
    }

    fun getDailySummaryHour(context: Context): Int {
        return getPrefs(context).getInt(KEY_DAILY_SUMMARY_HOUR, 8)
    }

    fun setDailySummaryHour(context: Context, hour: Int) {
        getPrefs(context).edit().putInt(KEY_DAILY_SUMMARY_HOUR, hour).apply()
    }
}
