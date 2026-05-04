package com.todopro.app

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class MonthlyReviewActivity : AppCompatActivity() {

    private lateinit var tvMonthTitle: TextView
    private lateinit var tvTotalCompleted: TextView
    private lateinit var tvBestDay: TextView
    private lateinit var tvActiveDays: TextView
    private lateinit var tvAvgPerDay: TextView
    private lateinit var tvGoalReached: TextView
    private lateinit var tvGoalSuccessRate: TextView
    private lateinit var heatmapGrid: LinearLayout
    private lateinit var btnBack: ImageButton
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton

    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH)

    override fun onCreate(savedInstanceState: Bundle?) {
        val isDark = TodoStorage.isDarkMode(this)
        if (isDark) setTheme(R.style.Theme_TodoPro_Dark)
        else setTheme(R.style.Theme_TodoPro_Light)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monthly_review)

        tvMonthTitle = findViewById(R.id.tvMonthTitle)
        tvTotalCompleted = findViewById(R.id.tvTotalCompleted)
        tvBestDay = findViewById(R.id.tvBestDay)
        tvActiveDays = findViewById(R.id.tvActiveDays)
        tvAvgPerDay = findViewById(R.id.tvAvgPerDay)
        tvGoalReached = findViewById(R.id.tvGoalReached)
        tvGoalSuccessRate = findViewById(R.id.tvGoalSuccessRate)
        heatmapGrid = findViewById(R.id.heatmapGrid)
        btnBack = findViewById(R.id.btnBack)
        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)

        btnBack.setOnClickListener { finish() }

        btnPrevMonth.setOnClickListener {
            currentMonth--
            if (currentMonth < 0) { currentMonth = 11; currentYear-- }
            loadMonthData()
        }

        btnNextMonth.setOnClickListener {
            val now = Calendar.getInstance()
            if (currentYear < now.get(Calendar.YEAR) ||
                (currentYear == now.get(Calendar.YEAR) && currentMonth < now.get(Calendar.MONTH))) {
                currentMonth++
                if (currentMonth > 11) { currentMonth = 0; currentYear++ }
                loadMonthData()
            }
        }

        loadMonthData()
        animateIn()
    }

    private fun animateIn() {
        val root = findViewById<View>(R.id.header)
        root.alpha = 0f
        root.translationY = -60f
        root.animate().alpha(1f).translationY(0f).setDuration(400)
            .setInterpolator(DecelerateInterpolator()).start()
    }

    private fun loadMonthData() {
        val monthNames = arrayOf(
            "Januar", "Februar", "März", "April", "Mai", "Juni",
            "Juli", "August", "September", "Oktober", "November", "Dezember"
        )
        tvMonthTitle.text = "${monthNames[currentMonth]} $currentYear"

        val monthData = TodoStorage.getMonthData(this, currentYear, currentMonth)
        val goalReachedCount = TodoStorage.getGoalReachedCountInMonth(this, currentYear, currentMonth)

        // Statistiken berechnen
        val totalCompleted = monthData.values.sum()
        val activeDays = monthData.size
        val bestDayEntry = monthData.maxByOrNull { it.value }
        val avgPerDay = if (activeDays > 0) totalCompleted.toFloat() / activeDays else 0f

        // Anzeigen
        tvTotalCompleted.text = "$totalCompleted Aufgaben"
        tvActiveDays.text = "$activeDays Tage"
        tvAvgPerDay.text = String.format("%.1f", avgPerDay)

        if (bestDayEntry != null) {
            val cal = Calendar.getInstance()
            cal.set(currentYear, currentMonth, bestDayEntry.key)
            val sdf = SimpleDateFormat("dd.MM.", Locale.GERMAN)
            tvBestDay.text = "${sdf.format(cal.time)} (${bestDayEntry.value} ✅)"
        } else {
            tvBestDay.text = "–"
        }

        // Tagesziel-Statistik
        val goal = TodoStorage.getDailyGoal(this)
        if (goal > 0 && activeDays > 0) {
            tvGoalReached.text = "$goalReachedCount × (Ziel: $goal/Tag)"
            val rate = (goalReachedCount * 100) / activeDays
            tvGoalSuccessRate.text = "$rate%"
        } else {
            tvGoalReached.text = "Kein Tagesziel gesetzt"
            tvGoalSuccessRate.text = "–"
        }

        // Heatmap aufbauen
        buildHeatmap(monthData)

        // Nächster-Monat-Button deaktivieren wenn aktueller Monat
        val now = Calendar.getInstance()
        btnNextMonth.alpha = if (currentYear == now.get(Calendar.YEAR) &&
            currentMonth == now.get(Calendar.MONTH)) 0.3f else 1f
    }

    private fun buildHeatmap(monthData: Map<Int, Int>) {
        heatmapGrid.removeAllViews()

        val cal = Calendar.getInstance()
        cal.set(currentYear, currentMonth, 1)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Wochentag des 1. des Monats (Mo=0, Di=1, ..., So=6)
        var firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 2 // Android: So=1, Mo=2...
        if (firstDayOfWeek < 0) firstDayOfWeek = 6 // Sonntag → 6

        val maxCount = monthData.values.maxOrNull() ?: 1
        val cellSize = 36 // dp
        val cellMargin = 3 // dp
        val density = resources.displayMetrics.density

        var dayCounter = 1
        var weekRow = createWeekRow()

        // Leere Zellen vor dem 1. des Monats
        for (i in 0 until firstDayOfWeek) {
            weekRow.addView(createEmptyCell(cellSize, cellMargin, density))
        }

        var cellsInRow = firstDayOfWeek

        while (dayCounter <= daysInMonth) {
            val count = monthData[dayCounter] ?: 0
            val cell = createDayCell(dayCounter, count, maxCount, cellSize, cellMargin, density)
            weekRow.addView(cell)
            cellsInRow++

            if (cellsInRow == 7) {
                heatmapGrid.addView(weekRow)
                weekRow = createWeekRow()
                cellsInRow = 0
            }
            dayCounter++
        }

        // Letzte Zeile auffüllen
        if (cellsInRow > 0) {
            while (cellsInRow < 7) {
                weekRow.addView(createEmptyCell(cellSize, cellMargin, density))
                cellsInRow++
            }
            heatmapGrid.addView(weekRow)
        }
    }

    private fun createWeekRow(): LinearLayout {
        return LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = (3 * resources.displayMetrics.density).toInt() }
            orientation = LinearLayout.HORIZONTAL
        }
    }

    private fun createEmptyCell(sizeDp: Int, marginDp: Int, density: Float): View {
        val sizePx = (sizeDp * density).toInt()
        val marginPx = (marginDp * density).toInt()
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, sizePx, 1f).also {
                it.marginEnd = marginPx
            }
        }
    }

    private fun createDayCell(
        day: Int, count: Int, maxCount: Int,
        sizeDp: Int, marginDp: Int, density: Float
    ): LinearLayout {
        val sizePx = (sizeDp * density).toInt()
        val marginPx = (marginDp * density).toInt()

        // Farbe basierend auf Aktivität
        val bgColor = when {
            count == 0 -> Color.parseColor("#1AFFFFFF")
            count <= maxCount / 4 -> Color.parseColor("#44CCFF00")
            count <= maxCount / 2 -> Color.parseColor("#88CCFF00")
            count <= maxCount * 3 / 4 -> Color.parseColor("#BBCCFF00")
            else -> Color.parseColor("#FFCCFF00")
        }

        // Heute markieren
        val now = Calendar.getInstance()
        val isToday = currentYear == now.get(Calendar.YEAR) &&
                currentMonth == now.get(Calendar.MONTH) &&
                day == now.get(Calendar.DAY_OF_MONTH)

        return LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, sizePx, 1f).also {
                it.marginEnd = marginPx
            }
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(bgColor)
            if (isToday) {
                // Heute: weißer Rahmen
                setPadding(2, 2, 2, 2)
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(bgColor)
                    setStroke((2 * density).toInt(), Color.WHITE)
                    cornerRadius = 4 * density
                }
            } else {
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(bgColor)
                    cornerRadius = 4 * density
                }
            }

            // Tag-Nummer
            addView(TextView(context).apply {
                text = "$day"
                textSize = 9f
                setTextColor(if (count > 0) Color.BLACK else Color.parseColor("#88FFFFFF"))
                gravity = Gravity.CENTER
            })

            // Anzahl (wenn > 0)
            if (count > 0) {
                addView(TextView(context).apply {
                    text = "$count"
                    textSize = 8f
                    setTextColor(Color.parseColor("#CC000000"))
                    gravity = Gravity.CENTER
                })
            }
        }
    }
}
