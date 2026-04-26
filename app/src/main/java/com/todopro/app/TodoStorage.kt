package com.todopro.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object TodoStorage {
    private const val PREFS_NAME = "todopro_prefs"
    private const val KEY_TODOS = "todos"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_COMPLIMENTS = "compliments_enabled"
    private const val KEY_KONFETTI = "konfetti_enabled"
    private const val KEY_NOTIFICATIONS = "notifications_enabled"

    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

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

    fun isDarkMode(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DARK_MODE, true)
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun isComplimentsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_COMPLIMENTS, true)
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
}
