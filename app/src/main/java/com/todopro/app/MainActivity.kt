package com.todopro.app

import android.Manifest
import android.animation.ObjectAnimator
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import nl.dionsegijn.konfetti.xml.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    // ── Views ───────────────────────────────────────────────────────────────
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TodoAdapter
    private lateinit var editInput: EditText
    private lateinit var btnAdd: ImageButton
    private lateinit var btnMenu: ImageButton
    private lateinit var btnSearch: ImageButton
    private lateinit var btnClearSearch: ImageButton
    private lateinit var editSearch: EditText
    private lateinit var searchBar: LinearLayout
    private lateinit var filterChipsScroll: HorizontalScrollView
    private lateinit var filterChipsContainer: LinearLayout
    private lateinit var emptyView: LinearLayout
    private lateinit var emptyTitle: TextView
    private lateinit var emptySubtitle: TextView
    private lateinit var konfettiView: KonfettiView
    private lateinit var tvSnackbar: TextView
    private lateinit var snackbarLayout: LinearLayout
    private lateinit var header: LinearLayout
    private lateinit var inputRow: LinearLayout
    private lateinit var tvTodoCount: TextView
    private lateinit var tvProgressPercent: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStreakHeader: TextView

    // Goal card
    private lateinit var goalCard: LinearLayout
    private lateinit var tvGoalLabel: TextView
    private lateinit var tvGoalCount: TextView
    private lateinit var goalProgressBar: ProgressBar

    // Sidebar
    private lateinit var sidebarPanel: LinearLayout
    private lateinit var sidebarOverlay: View
    private lateinit var btnCloseSidebar: ImageButton
    private lateinit var menuStats: LinearLayout
    private lateinit var menuArchive: LinearLayout
    private lateinit var menuTheme: LinearLayout
    private lateinit var menuNotifications: LinearLayout
    private lateinit var menuGoal: LinearLayout
    private lateinit var menuMonthlyReview: LinearLayout
    private lateinit var menuHelp: LinearLayout

    // State
    private val todos = mutableListOf<TodoItem>()
    private val completedTodos = mutableListOf<TodoItem>()
    private var isDarkMode = true
    private var isSidebarOpen = false
    private var isSearchOpen = false
    private var activeFilter = TodoCategory.NONE
    private var snackbarRunnable: Runnable? = null
    private var lastDeletedTodo: TodoItem? = null
    private val goalHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var goalHideRunnable: Runnable? = null

    companion object {
        private const val NOTIF_PERMISSION_REQUEST = 1001
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isDarkMode = TodoStorage.isDarkMode(this)
        applyTheme()
        setContentView(R.layout.activity_main)

        initViews()
        loadTodos()
        setupRecyclerView()
        setupSwipeGestures()
        setupListeners()
        setupButtonAnimations()
        requestNotificationPermission()
        checkExactAlarmPermission()
        updateEmptyView()
        updateTodoCount()
        updateGoalCard()
        updateStreakHeader()
        buildFilterChips()
        scheduleDailySummaryIfNeeded()
        animateHeaderIn()

        // Update-Check nach 2s
        val ver = try { packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0" } catch (_: Exception) { "1.0" }
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ UpdateChecker.checkForUpdate(this, ver) }, 2000)
    }

    private fun applyTheme() {
        if (isDarkMode) setTheme(R.style.Theme_TodoPro_Dark)
        else setTheme(R.style.Theme_TodoPro_Light)
    }

    // ── Init ─────────────────────────────────────────────────────────────────
    private fun initViews() {
        recyclerView         = findViewById(R.id.recyclerView)
        editInput            = findViewById(R.id.editInput)
        btnAdd               = findViewById(R.id.btnAdd)
        btnMenu              = findViewById(R.id.btnMenu)
        btnSearch            = findViewById(R.id.btnSearch)
        btnClearSearch       = findViewById(R.id.btnClearSearch)
        editSearch           = findViewById(R.id.editSearch)
        searchBar            = findViewById(R.id.searchBar)
        filterChipsScroll    = findViewById(R.id.filterChipsScroll)
        filterChipsContainer = findViewById(R.id.filterChipsContainer)
        emptyView            = findViewById(R.id.emptyView)
        emptyTitle           = findViewById(R.id.emptyTitle)
        emptySubtitle        = findViewById(R.id.emptySubtitle)
        konfettiView         = findViewById(R.id.konfettiView)
        tvSnackbar           = findViewById(R.id.tvSnackbar)
        snackbarLayout       = findViewById(R.id.snackbarLayout)
        header               = findViewById(R.id.header)
        inputRow             = findViewById(R.id.inputRow)
        tvTodoCount          = findViewById(R.id.tvTodoCount)
        tvProgressPercent    = findViewById(R.id.tvProgressPercent)
        progressBar          = findViewById(R.id.progressBar)
        tvStreakHeader       = findViewById(R.id.tvStreakHeader)
        goalCard             = findViewById(R.id.goalCard)
        tvGoalLabel          = findViewById(R.id.tvGoalLabel)
        tvGoalCount          = findViewById(R.id.tvGoalCount)
        goalProgressBar      = findViewById(R.id.goalProgressBar)
        sidebarPanel         = findViewById(R.id.sidebarPanel)
        sidebarOverlay       = findViewById(R.id.sidebarOverlay)
        btnCloseSidebar      = findViewById(R.id.btnCloseSidebar)
        menuStats            = findViewById(R.id.menuStats)
        menuArchive          = findViewById(R.id.menuArchive)
        menuTheme            = findViewById(R.id.menuTheme)
        menuNotifications    = findViewById(R.id.menuNotifications)
        menuGoal             = findViewById(R.id.menuGoal)
        menuMonthlyReview    = findViewById(R.id.menuMonthlyReview)
        menuHelp             = findViewById(R.id.menuHelp)
    }

    // ── Header Animation ─────────────────────────────────────────────────────
    private fun animateHeaderIn() {
        header.translationY = -120f; header.alpha = 0f
        header.animate().translationY(0f).alpha(1f).setDuration(460).setInterpolator(DecelerateInterpolator(1.5f)).start()
        inputRow.alpha = 0f; inputRow.translationY = 30f
        inputRow.animate().alpha(1f).translationY(0f).setDuration(400).setStartDelay(160).setInterpolator(DecelerateInterpolator()).start()
        recyclerView.alpha = 0f; recyclerView.translationY = 24f
        recyclerView.animate().alpha(1f).translationY(0f).setDuration(380).setStartDelay(280).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun setupButtonAnimations() {
        listOf(btnAdd, btnMenu, btnSearch).forEach { btn ->
            btn.setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(90).start()
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL ->
                        v.animate().scaleX(1f).scaleY(1f).setDuration(160).setInterpolator(OvershootInterpolator(2f)).start()
                }
                false
            }
        }
    }

    // ── Filter Chips ─────────────────────────────────────────────────────────
    private fun buildFilterChips() {
        filterChipsContainer.removeAllViews()
        val density = resources.displayMetrics.density
        val categories = listOf(null) + TodoCategory.values().filter { it != TodoCategory.NONE }
        var hasFilters = false

        for (cat in categories) {
            val chip = TextView(this)
            val label = if (cat == null) "Alle" else "${cat.emoji} ${cat.label}"
            chip.text = label
            chip.textSize = 12f
            chip.setTextColor(if (isDarkMode) Color.parseColor("#CCFF00") else Color.parseColor("#333333"))
            chip.setPadding((14 * density).toInt(), (7 * density).toInt(), (14 * density).toInt(), (7 * density).toInt())
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, (8 * density).toInt(), 0)
            chip.layoutParams = params

            val isSelected = cat == activeFilter || (cat == null && activeFilter == TodoCategory.NONE)
            chip.background = if (isSelected)
                ContextCompat.getDrawable(this, R.drawable.chip_bg_selected)
            else
                ContextCompat.getDrawable(this, R.drawable.chip_bg)

            chip.setOnClickListener {
                activeFilter = cat ?: TodoCategory.NONE
                adapter.setFilter(activeFilter)
                buildFilterChips()
                updateEmptyView()
                updateTodoCount()
            }

            filterChipsContainer.addView(chip)
            if (cat != null) hasFilters = true
        }

        // Chips nur anzeigen wenn es Todos gibt
        filterChipsScroll.visibility = if (todos.isNotEmpty()) View.VISIBLE else View.GONE
    }

    // ── Search ───────────────────────────────────────────────────────────────
    private fun toggleSearch() {
        isSearchOpen = !isSearchOpen
        if (isSearchOpen) {
            searchBar.visibility = View.VISIBLE
            searchBar.alpha = 0f
            searchBar.translationY = -20f
            searchBar.animate().alpha(1f).translationY(0f).setDuration(280)
                .setInterpolator(DecelerateInterpolator()).start()
            editSearch.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(editSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        } else {
            searchBar.animate().alpha(0f).translationY(-16f).setDuration(220)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction { searchBar.visibility = View.GONE }.start()
            editSearch.setText("")
            adapter.setSearchQuery("")
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(editSearch.windowToken, 0)
        }
    }

    // ── Sidebar ──────────────────────────────────────────────────────────────
    private fun openSidebar() {
        if (isSidebarOpen) return
        isSidebarOpen = true
        sidebarPanel.visibility = View.VISIBLE
        sidebarOverlay.visibility = View.VISIBLE
        sidebarOverlay.alpha = 0f
        sidebarPanel.translationX = 290f.dpToPx()
        sidebarPanel.animate().translationX(0f).setDuration(300).setInterpolator(DecelerateInterpolator(1.5f)).start()
        sidebarOverlay.animate().alpha(1f).setDuration(300).start()
    }

    private fun closeSidebar() {
        if (!isSidebarOpen) return
        isSidebarOpen = false
        sidebarPanel.animate().translationX(290f.dpToPx()).setDuration(260).setInterpolator(DecelerateInterpolator())
            .withEndAction { sidebarPanel.visibility = View.GONE }.start()
        sidebarOverlay.animate().alpha(0f).setDuration(260)
            .withEndAction { sidebarOverlay.visibility = View.GONE }.start()
    }

    private fun Float.dpToPx() = this * resources.displayMetrics.density

    // ── Data ─────────────────────────────────────────────────────────────────
    private fun loadTodos() {
        todos.clear(); todos.addAll(TodoStorage.loadTodos(this))
        completedTodos.clear(); completedTodos.addAll(TodoStorage.loadCompletedTodos(this))
        sortTodos()
    }

    private fun sortTodos() {
        todos.sortWith(compareByDescending<TodoItem> { it.isPriority }.thenByDescending { it.createdAt })
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────
    private fun setupRecyclerView() {
        adapter = TodoAdapter(
            todos,
            onDelete  = { todo -> deleteTodo(todo) },
            onComplete = { todo -> completeTodo(todo) },
            onReminderClick = { todo -> showReminderDialog(todo) },
            onTextChanged = { todo, newText ->
                todo.text = newText
                todo.isPriority = newText.startsWith("!")
                saveTodos()
            },
            onReminderSuggestion = { todo, suggestedTime ->
                todo.reminderTime = suggestedTime; saveTodos()
                AlarmScheduler.scheduleAlarm(this, todo)
                adapter.notifyDataSetChanged()
                val sdf = SimpleDateFormat("dd.MM. HH:mm", Locale.GERMAN)
                showSnackbar("⏰ Erinnerung gesetzt: ${sdf.format(Date(suggestedTime))}")
            },
            onCategoryChanged = { todo ->
                saveTodos()
                val cat = try { TodoCategory.valueOf(todo.category) } catch (_: Exception) { TodoCategory.NONE }
                if (cat != TodoCategory.NONE) showSnackbar("${cat.emoji} Kategorie: ${cat.label}")
                buildFilterChips()
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator().apply {
            addDuration = 260; removeDuration = 200; moveDuration = 160; changeDuration = 120
        }
        recyclerView.setOnTouchListener { _, _ ->
            if (editInput.hasFocus()) {
                editInput.clearFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(editInput.windowToken, 0)
            }
            false
        }
    }

    // ── Swipe Gesten (links = löschen, rechts = erledigen) ───────────────────
    private fun setupSwipeGestures() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                // Hole das Todo aus der displayList des Adapters – wir nehmen direkt todos[pos] wenn gleich
                if (pos < 0 || pos >= todos.size) return
                val todo = todos[pos]

                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        // Löschen
                        lastDeletedTodo = todo.copy()
                        deleteTodo(todo)
                        vibrate(50)
                        showSnackbarWithUndo("🗑️ Aufgabe gelöscht") {
                            lastDeletedTodo?.let { restored ->
                                todos.add(0, restored); sortTodos()
                                adapter.notifyDataSetChanged()
                                saveTodos(); updateEmptyView(); updateTodoCount()
                            }
                        }
                    }
                    ItemTouchHelper.RIGHT -> {
                        // Erledigen
                        completeTodo(todo)
                        vibrate(60)
                    }
                }
            }

            override fun onChildDraw(
                c: android.graphics.Canvas, rv: RecyclerView,
                vh: RecyclerView.ViewHolder, dX: Float, dY: Float,
                actionState: Int, isCurrentlyActive: Boolean
            ) {
                val item = vh.itemView
                val radius = 18f * resources.displayMetrics.density

                if (dX < 0) {
                    // Links: roter Lösch-Hintergrund
                    val paint = android.graphics.Paint().apply {
                        color = Color.parseColor("#CC2D3436")
                        isAntiAlias = true
                    }
                    val rect = android.graphics.RectF(item.right + dX, item.top.toFloat(), item.right.toFloat(), item.bottom.toFloat())
                    c.drawRoundRect(rect, radius, radius, paint)
                    // Trash icon Text
                    val textPaint = android.graphics.Paint().apply {
                        color = Color.WHITE; textSize = 42f; isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    val iconX = item.right - 60f
                    val iconY = (item.top + item.bottom) / 2f + 14f
                    c.drawText("🗑", iconX, iconY, textPaint)
                } else if (dX > 0) {
                    // Rechts: grüner Erledigt-Hintergrund
                    val paint = android.graphics.Paint().apply {
                        color = Color.parseColor("#CC00B894")
                        isAntiAlias = true
                    }
                    val rect = android.graphics.RectF(item.left.toFloat(), item.top.toFloat(), item.left + dX, item.bottom.toFloat())
                    c.drawRoundRect(rect, radius, radius, paint)
                    val textPaint = android.graphics.Paint().apply {
                        color = Color.WHITE; textSize = 42f; isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    val iconX = item.left + 60f
                    val iconY = (item.top + item.bottom) / 2f + 14f
                    c.drawText("✓", iconX, iconY, textPaint)
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView)
    }

    // ── Listeners ────────────────────────────────────────────────────────────
    private fun setupListeners() {
        btnAdd.setOnClickListener { addTodo() }
        editInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { addTodo(); true } else false
        }
        editInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) editInput.animate().scaleX(1.015f).scaleY(1.015f).setDuration(120).start()
            else editInput.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        }

        // Search
        btnSearch.setOnClickListener { toggleSearch() }
        btnClearSearch.setOnClickListener { toggleSearch() }
        editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.setSearchQuery(s?.toString() ?: "")
                updateEmptyView()
                updateTodoCount()
            }
        })

        btnMenu.setOnClickListener { openSidebar() }
        sidebarOverlay.setOnClickListener { closeSidebar() }
        btnCloseSidebar.setOnClickListener { closeSidebar() }

        fun afterClose(block: () -> Unit) {
            closeSidebar()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(block, 220)
        }

        menuStats.setOnClickListener { afterClose {
            startActivity(Intent(this, StatsActivity::class.java))
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out)
        }}
        menuArchive.setOnClickListener { afterClose {
            startActivity(Intent(this, ArchiveActivity::class.java))
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out)
        }}
        menuMonthlyReview.setOnClickListener { afterClose {
            startActivity(Intent(this, MonthlyReviewActivity::class.java))
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out)
        }}
        menuTheme.setOnClickListener { afterClose {
            isDarkMode = !isDarkMode; TodoStorage.setDarkMode(this, isDarkMode); recreate()
        }}
        menuNotifications.setOnClickListener { afterClose { showNotificationSettings() }}
        menuGoal.setOnClickListener       { afterClose { showDailyGoalDialog() }}
        menuHelp.setOnClickListener       { afterClose { showHelpDialog() }}
    }

    override fun onBackPressed() {
        when {
            isSearchOpen  -> toggleSearch()
            isSidebarOpen -> closeSidebar()
            else          -> super.onBackPressed()
        }
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────
    private fun addTodo() {
        val text = editInput.text.toString().trim()
        if (text.isEmpty()) {
            editInput.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake_input))
            return
        }

        val todo = TodoItem(text = text, isPriority = text.startsWith("!"))

        // Auto-Kategorisierung beim Hinzufügen
        if (AutoCategory.detect(text) != TodoCategory.NONE) {
            todo.category = AutoCategory.detect(text).name
        }

        todos.add(0, todo); sortTodos()
        val insertPos = todos.indexOf(todo)
        adapter.notifyItemInserted(insertPos)
        saveTodos()
        editInput.setText("")
        updateEmptyView(); updateTodoCount()
        buildFilterChips()
        vibrate(30)

        btnAdd.animate().scaleX(1.25f).scaleY(1.25f).setDuration(100)
            .withEndAction {
                btnAdd.animate().scaleX(1f).scaleY(1f).setDuration(200)
                    .setInterpolator(OvershootInterpolator(3f)).start()
            }.start()
    }

    private fun deleteTodo(todo: TodoItem) {
        AlarmScheduler.cancelAlarm(this, todo)
        val index = todos.indexOf(todo)
        if (index >= 0) {
            todos.removeAt(index); adapter.notifyItemRemoved(index)
            saveTodos(); updateEmptyView(); updateTodoCount(); buildFilterChips()
        }
    }

    private fun completeTodo(todo: TodoItem) {
        AlarmScheduler.cancelAlarm(this, todo)
        val index = todos.indexOf(todo)
        if (index >= 0) {
            todos.removeAt(index); adapter.notifyItemRemoved(index)
            val done = todo.copy(isCompleted = true, completedAt = System.currentTimeMillis())
            completedTodos.add(done)
            TodoStorage.saveCompletedTodos(this, completedTodos)
            TodoStorage.recordTodoCompleted(this)
            TodoStorage.recordDayCount(this, TodoStorage.getCompletedToday(this))
            TodoStorage.checkAndRecordGoalReached(this)
            saveTodos(); updateEmptyView(); updateTodoCount(); updateGoalCard(); updateStreakHeader()
            vibrate(80)
            if (TodoStorage.isKonfettiEnabled(this)) launchKonfetti()
            if (TodoStorage.isComplimentsEnabled(this)) showCompliment()
            val streak = TodoStorage.getStreak(this)
            if (streak > 1 && streak % 5 == 0) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    showSnackbar("🔥 $streak Tage Streak! Unglaublich!")
                }, 1500)
            }
        }
    }

    // ── Vibration ────────────────────────────────────────────────────────────
    private fun vibrate(ms: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                    .defaultVibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                else @Suppress("DEPRECATION") v.vibrate(ms)
            }
        } catch (_: Exception) {}
    }

    // ── Streak ────────────────────────────────────────────────────────────────
    private fun updateStreakHeader() {
        val streak = TodoStorage.getStreak(this)
        tvStreakHeader.text = when {
            streak >= 30 -> "🏆 $streak Tage Streak – Legende!"
            streak >= 14 -> "🔥 $streak Tage Streak!"
            streak >= 7  -> "⭐ $streak Tage Streak"
            streak >= 3  -> "✨ $streak Tage Streak"
            streak >= 1  -> "✦ $streak Tag Streak"
            else         -> ""
        }
    }

    // ── Konfetti ──────────────────────────────────────────────────────────────
    private fun launchKonfetti() {
        val colors = listOf(0xFFCCFF00.toInt(), 0xFF00E676.toInt(), 0xFFFFD700.toInt(),
            0xFFFF6B6B.toInt(), 0xFF4ECDC4.toInt(), 0xFFFFFFFF.toInt(),
            0xFFFF9F43.toInt(), 0xFF54A0FF.toInt(), 0xFFB388FF.toInt())
        konfettiView.start(Party(speed=0f, maxSpeed=38f, damping=0.88f, spread=360, colors=colors,
            emitter=Emitter(duration=140, TimeUnit.MILLISECONDS).max(140),
            position=Position.Relative(0.5, 0.3)))
    }

    private fun showCompliment() {
        val c = listOf("🎉 Großartig! Weiter so!","💪 Du rockst das!","⭐ Klasse!",
            "🚀 Produktivitäts-Monster!","✨ Perfekt gemacht!","🏆 Champion!",
            "🔥 On fire heute!","💥 Erledigt!","🎯 Volltreffer!","⚡ Blitzschnell!")
        showSnackbar(c.random())
    }

    // ── Snackbar ──────────────────────────────────────────────────────────────
    private fun showSnackbar(message: String) {
        tvSnackbar.text = message
        snackbarLayout.visibility = View.VISIBLE
        snackbarLayout.alpha = 0f; snackbarLayout.translationY = 60f
        snackbarLayout.animate().alpha(1f).translationY(0f).setDuration(300)
            .setInterpolator(OvershootInterpolator(1.5f)).start()
        snackbarRunnable?.let { snackbarLayout.removeCallbacks(it) }
        snackbarRunnable = Runnable {
            snackbarLayout.animate().alpha(0f).translationY(30f).setDuration(260)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction { snackbarLayout.visibility = View.GONE; snackbarLayout.translationY = 0f }.start()
        }
        snackbarLayout.postDelayed(snackbarRunnable!!, 3200)
        snackbarLayout.setOnClickListener(null)
    }

    private fun showSnackbarWithUndo(message: String, onUndo: () -> Unit) {
        tvSnackbar.text = "$message  ↩ Rückgängig"
        snackbarLayout.visibility = View.VISIBLE
        snackbarLayout.alpha = 0f; snackbarLayout.translationY = 60f
        snackbarLayout.animate().alpha(1f).translationY(0f).setDuration(300)
            .setInterpolator(OvershootInterpolator(1.5f)).start()
        snackbarRunnable?.let { snackbarLayout.removeCallbacks(it) }
        snackbarRunnable = Runnable {
            snackbarLayout.animate().alpha(0f).translationY(30f).setDuration(260)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction { snackbarLayout.visibility = View.GONE; snackbarLayout.translationY = 0f }.start()
        }
        snackbarLayout.postDelayed(snackbarRunnable!!, 4000)
        snackbarLayout.setOnClickListener {
            snackbarRunnable?.let { r -> snackbarLayout.removeCallbacks(r) }
            snackbarLayout.visibility = View.GONE; snackbarLayout.setOnClickListener(null)
            onUndo()
        }
    }

    // ── Todo Count & Progress ──────────────────────────────────────────────
    private fun updateTodoCount() {
        val count = todos.size
        val completedToday = TodoStorage.getCompletedToday(this)
        val total = count + completedToday
        tvTodoCount.text = when (count) {
            0    -> if (completedToday > 0) "Alles erledigt heute! 🎉" else ""
            1    -> "1 Aufgabe offen"
            else -> "$count Aufgaben offen"
        }
        tvTodoCount.alpha = 0f; tvTodoCount.animate().alpha(1f).setDuration(280).start()
        if (total > 0 && completedToday > 0) {
            val pct = (completedToday * 100) / total
            progressBar.visibility = View.VISIBLE
            ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, pct).setDuration(500).start()
            tvProgressPercent.text = "$pct%"
        } else {
            progressBar.visibility = View.GONE; tvProgressPercent.text = ""
        }
    }

    // ── Empty View ───────────────────────────────────────────────────────────
    private fun updateEmptyView() {
        val hasItems = todos.isNotEmpty()
        val searchActive = editSearch.text?.isNotBlank() == true

        if (todos.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            emptyTitle.text = if (searchActive) "Keine Treffer" else "Alles erledigt!"
            emptySubtitle.text = if (searchActive) "Versuche einen anderen Suchbegriff." else "Füge oben eine neue Aufgabe hinzu."
            emptyView.alpha = 0f; emptyView.scaleX = 0.92f; emptyView.scaleY = 0.92f
            emptyView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(380)
                .setInterpolator(OvershootInterpolator(1.2f)).start()
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    // ── Tagesziel ─────────────────────────────────────────────────────────────
    private fun updateGoalCard() {
        val goal = TodoStorage.getDailyGoal(this)
        if (goal <= 0 || TodoStorage.isGoalReachedToday(this)) {
            goalCard.visibility = View.GONE; return
        }
        val done = TodoStorage.getCompletedToday(this)
        val pct = minOf((done * 100) / goal, 100)
        val isReached = done >= goal
        goalCard.visibility = View.VISIBLE
        tvGoalLabel.text = if (isReached) "🏆 Tagesziel erreicht!" else "🎯 Tagesziel: $goal Aufgaben"
        tvGoalCount.text = "$done / $goal"
        val anim = ObjectAnimator.ofInt(goalProgressBar, "progress", goalProgressBar.progress, pct)
        anim.duration = 500
        anim.interpolator = DecelerateInterpolator()
        anim.start()
        if (goalCard.alpha == 0f) {
            goalCard.alpha = 0f; goalCard.translationY = -20f
            goalCard.animate().alpha(1f).translationY(0f).setDuration(340).setInterpolator(OvershootInterpolator(1.2f)).start()
        }
        if (isReached) {
            goalHandler.postDelayed({ showSnackbar("🏆 Tagesziel erreicht! Fantastisch!"); launchKonfetti() }, 600)
            goalHideRunnable?.let { goalHandler.removeCallbacks(it) }
            goalHideRunnable = Runnable {
                goalCard.animate().alpha(0f).translationY(-16f).setDuration(380).setInterpolator(DecelerateInterpolator())
                    .withEndAction { goalCard.visibility = View.GONE; goalCard.translationY = 0f; goalCard.alpha = 1f }.start()
            }
            goalHandler.postDelayed(goalHideRunnable!!, 3000)
        } else {
            goalHideRunnable?.let { goalHandler.removeCallbacks(it) }; goalHideRunnable = null
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────
    private fun showReminderDialog(todo: TodoItem) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reminder, null)
        val tvTodoText       = dialogView.findViewById<TextView>(R.id.tvTodoText)
        val tvCurrentReminder= dialogView.findViewById<TextView>(R.id.tvCurrentReminder)
        val btn15min         = dialogView.findViewById<LinearLayout>(R.id.btn15min)
        val btn30min         = dialogView.findViewById<LinearLayout>(R.id.btn30min)
        val btn1hour         = dialogView.findViewById<LinearLayout>(R.id.btn1hour)
        val btnTomorrow      = dialogView.findViewById<LinearLayout>(R.id.btnTomorrow)
        val editDate         = dialogView.findViewById<EditText>(R.id.editDate)
        val editTime         = dialogView.findViewById<EditText>(R.id.editTime)
        val btnOk            = dialogView.findViewById<Button>(R.id.btnOk)
        val btnClose         = dialogView.findViewById<ImageButton>(R.id.btnClose)

        tvTodoText.text = if (todo.isPriority && todo.text.startsWith("!"))
            todo.text.removePrefix("!").trimStart() else todo.text

        val sdf     = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN)
        val sdfDate = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
        val sdfTime = SimpleDateFormat("HH:mm", Locale.GERMAN)

        tvCurrentReminder.text = if (todo.reminderTime != null)
            "⏰ Erinnerung: ${sdf.format(Date(todo.reminderTime!!))}" else "Keine Erinnerung gesetzt"
        tvCurrentReminder.visibility = View.VISIBLE

        val cal = Calendar.getInstance()
        todo.reminderTime?.let { cal.timeInMillis = it }
        editDate.setText(sdfDate.format(cal.time))
        editTime.setText(sdfTime.format(cal.time))

        val dialog = AlertDialog.Builder(this, R.style.ReminderDialogTheme)
            .setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        fun setReminder(timeMs: Long) {
            todo.reminderTime = timeMs; saveTodos()
            AlarmScheduler.scheduleAlarm(this, todo)
            adapter.notifyDataSetChanged()
            showSnackbar("⏰ ${sdf.format(Date(timeMs))}")
            dialog.dismiss()
        }

        btn15min.setOnClickListener  { setReminder(System.currentTimeMillis() + 15 * 60_000) }
        btn30min.setOnClickListener  { setReminder(System.currentTimeMillis() + 30 * 60_000) }
        btn1hour.setOnClickListener  { setReminder(System.currentTimeMillis() + 60 * 60_000) }
        btnTomorrow.setOnClickListener {
            val t = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
            }
            setReminder(t.timeInMillis)
        }
        editDate.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                editDate.setText(sdfDate.format(Calendar.getInstance().apply { set(y, m, d) }.time))
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }
        editTime.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(this, { _, h, min ->
                editTime.setText(String.format("%02d:%02d", h, min))
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }
        btnOk.setOnClickListener {
            try {
                val combined = sdf.parse("${editDate.text} ${editTime.text}")
                if (combined != null) {
                    if (combined.time > System.currentTimeMillis()) setReminder(combined.time)
                    else Toast.makeText(this, "Zeitpunkt liegt in der Vergangenheit!", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) { Toast.makeText(this, "Ungültiges Datum/Uhrzeit", Toast.LENGTH_SHORT).show() }
        }
        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showNotificationSettings() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)
        val switchCompliments   = dialogView.findViewById<Switch>(R.id.switchCompliments)
        val switchKonfetti      = dialogView.findViewById<Switch>(R.id.switchKonfetti)
        val switchNotifications = dialogView.findViewById<Switch>(R.id.switchNotifications)
        val switchDailySummary  = dialogView.findViewById<Switch>(R.id.switchDailySummary)
        val btnClose            = dialogView.findViewById<ImageButton>(R.id.btnClose)

        switchCompliments.isChecked   = TodoStorage.isComplimentsEnabled(this)
        switchKonfetti.isChecked      = TodoStorage.isKonfettiEnabled(this)
        switchNotifications.isChecked = TodoStorage.isNotificationsEnabled(this)
        switchDailySummary?.isChecked = TodoStorage.isDailySummaryEnabled(this)

        switchCompliments.setOnCheckedChangeListener   { _, c -> TodoStorage.setComplimentsEnabled(this, c) }
        switchKonfetti.setOnCheckedChangeListener      { _, c -> TodoStorage.setKonfettiEnabled(this, c) }
        switchNotifications.setOnCheckedChangeListener { _, c -> TodoStorage.setNotificationsEnabled(this, c) }
        switchDailySummary?.setOnCheckedChangeListener { _, c ->
            TodoStorage.setDailySummaryEnabled(this, c)
            if (c) DailySummaryReceiver.schedule(this) else DailySummaryReceiver.cancel(this)
        }

        val dialog = AlertDialog.Builder(this, R.style.ReminderDialogTheme).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showHelpDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_help, null)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)
        val dialog = AlertDialog.Builder(this, R.style.ReminderDialogTheme).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showDailyGoalDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_daily_goal, null)
        val btnGoal1      = dialogView.findViewById<LinearLayout>(R.id.btnGoal1)
        val btnGoal3      = dialogView.findViewById<LinearLayout>(R.id.btnGoal3)
        val btnGoal5      = dialogView.findViewById<LinearLayout>(R.id.btnGoal5)
        val btnGoal10     = dialogView.findViewById<LinearLayout>(R.id.btnGoal10)
        val editGoalNumber= dialogView.findViewById<EditText>(R.id.editGoalNumber)
        val btnSetGoal    = dialogView.findViewById<Button>(R.id.btnSetGoal)
        val btnClearGoal  = dialogView.findViewById<TextView>(R.id.btnClearGoal)
        val btnClose      = dialogView.findViewById<ImageButton>(R.id.btnClose)

        val currentGoal = TodoStorage.getDailyGoal(this)
        if (currentGoal > 0) editGoalNumber.setText("$currentGoal")

        val dialog = AlertDialog.Builder(this, R.style.ReminderDialogTheme).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        fun setGoal(goal: Int) {
            TodoStorage.setDailyGoal(this, goal); updateGoalCard()
            showSnackbar("🎯 Tagesziel: $goal Aufgaben"); dialog.dismiss()
        }

        btnGoal1.setOnClickListener  { setGoal(1) }
        btnGoal3.setOnClickListener  { setGoal(3) }
        btnGoal5.setOnClickListener  { setGoal(5) }
        btnGoal10.setOnClickListener { setGoal(10) }
        btnSetGoal.setOnClickListener {
            val num = editGoalNumber.text.toString().trim().toIntOrNull()
            if (num != null && num > 0) setGoal(num)
            else Toast.makeText(this, "Bitte eine gültige Zahl eingeben", Toast.LENGTH_SHORT).show()
        }
        btnClearGoal.setOnClickListener {
            TodoStorage.setDailyGoal(this, 0); updateGoalCard()
            showSnackbar("Tagesziel entfernt"); dialog.dismiss()
        }
        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // ── Save & Widgets ────────────────────────────────────────────────────────
    private fun saveTodos() {
        TodoStorage.saveTodos(this, todos)
        updateWidgets()
    }

    private fun updateWidgets() {
        try {
            val awm = AppWidgetManager.getInstance(this)
            awm.getAppWidgetIds(ComponentName(this, TodoWidgetSmall::class.java)).forEach { TodoWidgetSmall.updateWidget(this, awm, it) }
            awm.getAppWidgetIds(ComponentName(this, TodoWidgetLarge::class.java)).forEach { TodoWidgetLarge.updateWidget(this, awm, it) }
        } catch (_: Exception) {}
    }

    // ── Permissions ───────────────────────────────────────────────────────────
    private fun scheduleDailySummaryIfNeeded() {
        if (TodoStorage.isDailySummaryEnabled(this)) DailySummaryReceiver.schedule(this)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIF_PERMISSION_REQUEST)
        }
    }

    private fun checkExactAlarmPermission() {
        if (!AlarmScheduler.canScheduleExactAlarms(this)) {
            AlertDialog.Builder(this)
                .setTitle("Exakte Alarme erlauben")
                .setMessage("Damit Erinnerungen zuverlässig funktionieren, muss TodoPro exakte Alarme setzen dürfen.")
                .setPositiveButton("Einstellungen öffnen") { _, _ ->
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply { data = Uri.parse("package:$packageName") })
                }
                .setNegativeButton("Später", null).show()
        }
    }
}
