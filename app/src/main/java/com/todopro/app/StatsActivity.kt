package com.todopro.app

import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Theme anwenden
        if (TodoStorage.isDarkMode(this)) {
            setTheme(R.style.Theme_TodoPro_Dark)
        } else {
            setTheme(R.style.Theme_TodoPro_Light)
        }

        setContentView(R.layout.activity_stats)

        val btnClose = findViewById<ImageButton>(R.id.btnClose)
        val tvStreak = findViewById<TextView>(R.id.tvStreak)
        val tvStreakLabel = findViewById<TextView>(R.id.tvStreakLabel)
        val tvTotalCompleted = findViewById<TextView>(R.id.tvTotalCompleted)
        val tvCompletedToday = findViewById<TextView>(R.id.tvCompletedToday)
        val tvOpenTodos = findViewById<TextView>(R.id.tvOpenTodos)
        val tvStreakEmoji = findViewById<TextView>(R.id.tvStreakEmoji)
        val tvCompletedList = findViewById<TextView>(R.id.tvCompletedList)

        btnClose.setOnClickListener { finish() }

        // Daten laden
        val streak = TodoStorage.getStreak(this)
        val total = TodoStorage.getTotalCompleted(this)
        val today = TodoStorage.getCompletedToday(this)
        val openTodos = TodoStorage.loadTodos(this).size
        val completedTodos = TodoStorage.loadCompletedTodos(this)

        // Streak anzeigen
        tvStreak.text = streak.toString()
        tvStreakLabel.text = if (streak == 1) "Tag Streak" else "Tage Streak"
        tvStreakEmoji.text = when {
            streak >= 30 -> "🏆"
            streak >= 14 -> "🔥"
            streak >= 7  -> "⭐"
            streak >= 3  -> "💪"
            streak >= 1  -> "✨"
            else         -> "😴"
        }

        tvTotalCompleted.text = total.toString()
        tvCompletedToday.text = today.toString()
        tvOpenTodos.text = openTodos.toString()

        // Erledigte Todos Liste (letzte 10)
        if (completedTodos.isEmpty()) {
            tvCompletedList.text = "Noch keine erledigten Aufgaben"
        } else {
            val sdf = SimpleDateFormat("dd.MM.yy HH:mm", Locale.GERMAN)
            val sb = StringBuilder()
            completedTodos.reversed().take(10).forEach { todo ->
                val displayText = if (todo.isPriority && todo.text.startsWith("!")) {
                    todo.text.removePrefix("!").trimStart()
                } else todo.text
                val dateStr = todo.completedAt?.let { sdf.format(Date(it)) } ?: ""
                sb.append("✅ $displayText")
                if (dateStr.isNotEmpty()) sb.append("\n   $dateStr")
                sb.append("\n\n")
            }
            tvCompletedList.text = sb.toString().trimEnd()
        }

        // Animationen
        animateIn()
    }

    private fun animateIn() {
        val views = listOf(
            findViewById<android.view.View>(R.id.cardStreak),
            findViewById<android.view.View>(R.id.cardStats),
            findViewById<android.view.View>(R.id.cardCompleted)
        )

        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 60f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay((index * 120).toLong())
                .setInterpolator(OvershootInterpolator(1.2f))
                .start()
        }
    }
}
