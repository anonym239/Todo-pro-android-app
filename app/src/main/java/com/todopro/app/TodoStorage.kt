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
    private const val KEY_FIRST_LAUNCH_DONE = "first_launch_done"
    private const val KEY_NOTIFICATION_COUNT = "notification_count"
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
    // Tagesziel
    private const val KEY_DAILY_GOAL = "daily_goal"
    private const val KEY_DAILY_GOAL_DATE = "daily_goal_date"
    // Monatsdaten: Key = "day_YYYY-MM-DD", Value = Anzahl erledigter Todos
    private const val KEY_DAY_PREFIX = "day_"
    // Tagesziel-Erfolge: Key = "goal_reached_YYYY-MM-DD"
    private const val KEY_GOAL_REACHED_PREFIX = "goal_reached_"

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

    /** Erster App-Start? (Onboarding noch nicht gesehen) */
    fun isFirstLaunch(context: Context): Boolean {
        return !getPrefs(context).getBoolean(KEY_FIRST_LAUNCH_DONE, false)
    }

    /** Onboarding als gesehen markieren */
    fun setFirstLaunchDone(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_FIRST_LAUNCH_DONE, true).apply()
    }

    /** Gibt zurück wie viele Benachrichtigungen bisher gesendet wurden */
    fun getNotificationCount(context: Context): Int {
        return getPrefs(context).getInt(KEY_NOTIFICATION_COUNT, 0)
    }

    /** Benachrichtigungs-Zähler erhöhen und neuen Wert zurückgeben */
    fun incrementNotificationCount(context: Context): Int {
        val count = getNotificationCount(context) + 1
        getPrefs(context).edit().putInt(KEY_NOTIFICATION_COUNT, count).apply()
        return count
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

    // ── Tagesziel ──────────────────────────────────────────────────────────

    /** Tagesziel setzen (0 = kein Ziel) */
    fun setDailyGoal(context: Context, goal: Int) {
        val today = getTodayString()
        getPrefs(context).edit()
            .putInt(KEY_DAILY_GOAL, goal)
            .putString(KEY_DAILY_GOAL_DATE, today)
            .apply()
    }

    /** Aktuelles Tagesziel (0 = kein Ziel) */
    fun getDailyGoal(context: Context): Int {
        return getPrefs(context).getInt(KEY_DAILY_GOAL, 0)
    }

    /** Prüft ob das Tagesziel heute schon erreicht wurde und speichert es */
    fun checkAndRecordGoalReached(context: Context) {
        val goal = getDailyGoal(context)
        if (goal <= 0) return
        val completedToday = getCompletedToday(context)
        if (completedToday >= goal) {
            val today = getFullTodayString()
            getPrefs(context).edit()
                .putBoolean("$KEY_GOAL_REACHED_PREFIX$today", true)
                .apply()
        }
    }

    /** Wurde das Tagesziel heute erreicht? */
    fun isGoalReachedToday(context: Context): Boolean {
        val today = getFullTodayString()
        return getPrefs(context).getBoolean("$KEY_GOAL_REACHED_PREFIX$today", false)
    }

    // ── Monatsdaten ────────────────────────────────────────────────────────

    /** Speichert die Anzahl erledigter Todos für heute (wird bei recordTodoCompleted aufgerufen) */
    fun recordDayCount(context: Context, count: Int) {
        val today = getFullTodayString()
        getPrefs(context).edit()
            .putInt("$KEY_DAY_PREFIX$today", count)
            .apply()
    }

    /** Gibt die Anzahl erledigter Todos für einen bestimmten Tag zurück */
    fun getDayCount(context: Context, year: Int, month: Int, day: Int): Int {
        val key = "$KEY_DAY_PREFIX${String.format("%04d-%02d-%02d", year, month + 1, day)}"
        return getPrefs(context).getInt(key, 0)
    }

    /** Gibt alle Tagesdaten für einen Monat zurück: Map<Tag(1-31), Anzahl> */
    fun getMonthData(context: Context, year: Int, month: Int): Map<Int, Int> {
        val prefs = getPrefs(context)
        val result = mutableMapOf<Int, Int>()
        val cal = Calendar.getInstance()
        cal.set(year, month, 1)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..daysInMonth) {
            val key = "$KEY_DAY_PREFIX${String.format("%04d-%02d-%02d", year, month + 1, day)}"
            val count = prefs.getInt(key, 0)
            if (count > 0) result[day] = count
        }
        return result
    }

    /** Gibt zurück wie oft das Tagesziel in einem Monat erreicht wurde */
    fun getGoalReachedCountInMonth(context: Context, year: Int, month: Int): Int {
        val prefs = getPrefs(context)
        val cal = Calendar.getInstance()
        cal.set(year, month, 1)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        var count = 0
        for (day in 1..daysInMonth) {
            val key = "$KEY_GOAL_REACHED_PREFIX${String.format("%04d-%02d-%02d", year, month + 1, day)}"
            if (prefs.getBoolean(key, false)) count++
        }
        return count
    }

    private fun getFullTodayString(): String {
        val cal = Calendar.getInstance()
        return String.format(
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }
}
