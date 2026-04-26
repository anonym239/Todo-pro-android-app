package com.todopro.app

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
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

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TodoAdapter
    private lateinit var editInput: EditText
    private lateinit var btnAdd: ImageButton
    private lateinit var btnTheme: ImageButton
    private lateinit var btnNotifications: ImageButton
    private lateinit var btnHelp: ImageButton
    private lateinit var btnStats: ImageButton
    private lateinit var btnArchive: ImageButton
    private lateinit var emptyView: LinearLayout
    private lateinit var konfettiView: KonfettiView
    private lateinit var tvSnackbar: TextView
    private lateinit var snackbarLayout: LinearLayout
    private lateinit var header: LinearLayout
    private lateinit var inputRow: LinearLayout
    private lateinit var tvTodoCount: TextView
    private lateinit var tvProgressPercent: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStreakHeader: TextView

    private val todos = mutableListOf<TodoItem>()
    private val completedTodos = mutableListOf<TodoItem>()
    private var isDarkMode = true
    private var snackbarRunnable: Runnable? = null
    private var lastDeletedTodo: TodoItem? = null

    companion object {
        private const val NOTIF_PERMISSION_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isDarkMode = TodoStorage.isDarkMode(this)
        applyTheme()

        setContentView(R.layout.activity_main)

        initViews()
        loadTodos()
        setupRecyclerView()
        setupSwipeToDelete()
        setupListeners()
        setupButtonAnimations()
        requestNotificationPermission()
        checkExactAlarmPermission()
        updateEmptyView()
        updateTodoCount()
        updateStreakHeader()
        scheduleDailySummaryIfNeeded()

        animateHeaderIn()

        val currentVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) { "1.0" }
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            UpdateChecker.checkForUpdate(this, currentVersion)
        }, 2000)
    }

    private fun applyTheme() {
        if (isDarkMode) setTheme(R.style.Theme_TodoPro_Dark)
        else setTheme(R.style.Theme_TodoPro_Light)
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        editInput = findViewById(R.id.editInput)
        btnAdd = findViewById(R.id.btnAdd)
        btnTheme = findViewById(R.id.btnTheme)
        btnNotifications = findViewById(R.id.btnNotifications)
        btnHelp = findViewById(R.id.btnHelp)
        btnStats = findViewById(R.id.btnStats)
        btnArchive = findViewById(R.id.btnArchive)
        emptyView = findViewById(R.id.emptyView)
        konfettiView = findViewById(R.id.konfettiView)
        tvSnackbar = findViewById(R.id.tvSnackbar)
        snackbarLayout = findViewById(R.id.snackbarLayout)
        header = findViewById(R.id.header)
        inputRow = findViewById(R.id.inputRow)
        tvTodoCount = findViewById(R.id.tvTodoCount)
        tvProgressPercent = findViewById(R.id.tvProgressPercent)
        progressBar = findViewById(R.id.progressBar)
        tvStreakHeader = findViewById(R.id.tvStreakHeader)
    }

    private fun animateHeaderIn() {
        header.translationY = -120f
        header.alpha = 0f
        header.animate().translationY(0f).alpha(1f).setDuration(450)
            .setInterpolator(DecelerateInterpolator(1.5f)).start()

        inputRow.alpha = 0f
        inputRow.translationY = 40f
        inputRow.animate().alpha(1f).translationY(0f).setDuration(400)
            .setStartDelay(180).setInterpolator(DecelerateInterpolator()).start()

        recyclerView.alpha = 0f
        recyclerView.translationY = 30f
        recyclerView.animate().alpha(1f).translationY(0f).setDuration(400)
            .setStartDelay(300).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun setupButtonAnimations() {
        listOf(btnAdd, btnTheme, btnNotifications, btnHelp, btnStats, btnArchive).forEach { btn ->
            btn.setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN ->
                        v.animate().scaleX(0.86f).scaleY(0.86f).setDuration(90).start()
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL ->
                        v.animate().scaleX(1f).scaleY(1f).setDuration(160)
                            .setInterpolator(OvershootInterpolator(2f)).start()
                }
                false
            }
        }
    }

    private fun loadTodos() {
        todos.clear()
        todos.addAll(TodoStorage.loadTodos(this))
        completedTodos.clear()
        completedTodos.addAll(TodoStorage.loadCompletedTodos(this))
        sortTodos()
    }

    private fun sortTodos() {
        todos.sortWith(compareByDescending<TodoItem> { it.isPriority }.thenByDescending { it.createdAt })
    }

    private fun setupRecyclerView() {
        adapter = TodoAdapter(
            todos,
            onDelete = { todo -> deleteTodo(todo) },
            onComplete = { todo -> completeTodo(todo) },
            onReminderClick = { todo -> showReminderDialog(todo) },
            onTextChanged = { todo, newText ->
                todo.text = newText
                todo.isPriority = newText.startsWith("!")
                saveTodos()
            },
            onReminderSuggestion = { todo, suggestedTime ->
                // Erinnerungsvorschlag direkt setzen
                todo.reminderTime = suggestedTime
                saveTodos()
                AlarmScheduler.scheduleAlarm(this, todo)
                adapter.notifyDataSetChanged()
                val sdf = java.text.SimpleDateFormat("dd.MM. HH:mm", java.util.Locale.GERMAN)
                showSnackbar("⏰ Erinnerung gesetzt: ${sdf.format(java.util.Date(suggestedTime))}")
            },
            onCategoryChanged = { todo ->
                saveTodos()
                val cat = try { TodoCategory.valueOf(todo.category) } catch (e: Exception) { TodoCategory.NONE }
                if (cat != TodoCategory.NONE) {
                    showSnackbar("${cat.emoji} Kategorie: ${cat.label}")
                }
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator().apply {
            addDuration = 280; removeDuration = 220; moveDuration = 180; changeDuration = 150
        }
    }

    // Swipe-to-Delete: nach links wischen löscht die Todo
    private fun setupSwipeToDelete() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position < 0 || position >= todos.size) return
                val todo = todos[position]
                lastDeletedTodo = todo.copy()
                deleteTodo(todo)
                vibrate(50)
                showSnackbarWithUndo("🗑️ Aufgabe gelöscht") {
                    // Undo: Todo wiederherstellen
                    lastDeletedTodo?.let { restored ->
                        todos.add(0, restored)
                        sortTodos()
                        adapter.notifyDataSetChanged()
                        saveTodos()
                        updateEmptyView()
                        updateTodoCount()
                    }
                }
            }

            override fun onChildDraw(
                c: android.graphics.Canvas, recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float,
                actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val paint = android.graphics.Paint().apply { color = Color.parseColor("#FF4444") }
                val icon = ContextCompat.getDrawable(this@MainActivity, android.R.drawable.ic_menu_delete)

                if (dX < 0) {
                    // Roter Hintergrund beim Wischen
                    c.drawRect(
                        itemView.right + dX, itemView.top.toFloat(),
                        itemView.right.toFloat(), itemView.bottom.toFloat(), paint
                    )
                    // Lösch-Icon
                    icon?.let {
                        val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = iconTop + it.intrinsicHeight
                        val iconLeft = itemView.right - iconMargin - it.intrinsicWidth
                        val iconRight = itemView.right - iconMargin
                        it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        it.setTint(Color.WHITE)
                        it.draw(c)
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView)
    }

    private fun setupListeners() {
        btnAdd.setOnClickListener { addTodo() }

        editInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { addTodo(); true } else false
        }

        editInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) editInput.animate().scaleX(1.02f).scaleY(1.02f).setDuration(150).start()
            else editInput.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
        }

        btnTheme.setOnClickListener {
            isDarkMode = !isDarkMode
            TodoStorage.setDarkMode(this, isDarkMode)
            recreate()
        }

        btnNotifications.setOnClickListener { showNotificationSettings() }
        btnHelp.setOnClickListener { showHelpDialog() }
        btnStats.setOnClickListener {
            val intent = Intent(this, StatsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out)
        }

        btnArchive.setOnClickListener {
            val intent = Intent(this, ArchiveActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out)
        }
    }

    private fun addTodo() {
        val text = editInput.text.toString().trim()
        if (text.isEmpty()) {
            editInput.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake_input))
            return
        }

        val todo = TodoItem(text = text, isPriority = text.startsWith("!"))
        todos.add(0, todo)
        sortTodos()
        val insertPos = todos.indexOf(todo)
        adapter.notifyItemInserted(insertPos)
        saveTodos()
        editInput.setText("")
        updateEmptyView()
        updateTodoCount()
        vibrate(30)

        btnAdd.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100)
            .withEndAction {
                btnAdd.animate().scaleX(1f).scaleY(1f).setDuration(180)
                    .setInterpolator(OvershootInterpolator(3f)).start()
            }.start()
    }

    private fun deleteTodo(todo: TodoItem) {
        AlarmScheduler.cancelAlarm(this, todo)
        val index = todos.indexOf(todo)
        if (index >= 0) {
            todos.removeAt(index)
            adapter.notifyItemRemoved(index)
            saveTodos()
            updateEmptyView()
            updateTodoCount()
        }
    }

    private fun completeTodo(todo: TodoItem) {
        AlarmScheduler.cancelAlarm(this, todo)
        val index = todos.indexOf(todo)
        if (index >= 0) {
            todos.removeAt(index)
            adapter.notifyItemRemoved(index)

            // Als erledigt speichern
            val completedTodo = todo.copy(isCompleted = true, completedAt = System.currentTimeMillis())
            completedTodos.add(completedTodo)
            TodoStorage.saveCompletedTodos(this, completedTodos)
            TodoStorage.recordTodoCompleted(this)

            saveTodos()
            updateEmptyView()
            updateTodoCount()
            updateStreakHeader()

            // Haptic Feedback
            vibrate(80)

            if (TodoStorage.isKonfettiEnabled(this)) launchKonfetti()
            if (TodoStorage.isComplimentsEnabled(this)) showCompliment()

            // Streak-Meldung
            val streak = TodoStorage.getStreak(this)
            if (streak > 1 && streak % 5 == 0) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    showSnackbar("🔥 $streak Tage Streak! Unglaublich!")
                }, 1500)
            }
        }
    }

    // Haptic Feedback
    private fun vibrate(ms: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(ms)
                }
            }
        } catch (e: Exception) { /* ignore */ }
    }

    private fun updateStreakHeader() {
        val streak = TodoStorage.getStreak(this)
        tvStreakHeader.text = when {
            streak >= 7 -> "🔥 $streak Tage Streak!"
            streak >= 3 -> "⭐ $streak Tage Streak"
            streak >= 1 -> "✨ $streak Tag Streak"
            else -> ""
        }
    }

    private fun launchKonfetti() {
        val colors = listOf(
            0xFFCCFF00.toInt(), 0xFF00FF88.toInt(), 0xFFFFD700.toInt(),
            0xFFFF6B6B.toInt(), 0xFF4ECDC4.toInt(), 0xFFFFFFFF.toInt(),
            0xFFFF9F43.toInt(), 0xFF54A0FF.toInt()
        )
        konfettiView.start(
            Party(
                speed = 0f, maxSpeed = 35f, damping = 0.88f, spread = 360,
                colors = colors,
                emitter = Emitter(duration = 120, TimeUnit.MILLISECONDS).max(120),
                position = Position.Relative(0.5, 0.3)
            )
        )
    }

    private fun showCompliment() {
        val compliments = listOf(
            "🎉 Großartig! Weiter so!", "💪 Du rockst das!", "⭐ Aufgabe erledigt! Klasse!",
            "🚀 Produktivitäts-Monster!", "✨ Perfekt gemacht!", "🏆 Champion!",
            "🔥 On fire heute!", "💥 Boom! Erledigt!", "🎯 Volltreffer!"
        )
        showSnackbar(compliments.random())
    }

    private fun showSnackbar(message: String) {
        tvSnackbar.text = message
        snackbarLayout.visibility = View.VISIBLE
        snackbarLayout.alpha = 0f
        snackbarLayout.translationY = 60f
        snackbarLayout.animate().alpha(1f).translationY(0f).setDuration(320)
            .setInterpolator(OvershootInterpolator(1.5f)).start()

        snackbarRunnable?.let { snackbarLayout.removeCallbacks(it) }
        snackbarRunnable = Runnable {
            snackbarLayout.animate().alpha(0f).translationY(30f).setDuration(280)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction { snackbarLayout.visibility = View.GONE; snackbarLayout.translationY = 0f }.start()
        }
        snackbarLayout.postDelayed(snackbarRunnable!!, 3000)
    }

    private fun showSnackbarWithUndo(message: String, onUndo: () -> Unit) {
        tvSnackbar.text = "$message  ↩ Rückgängig"
        snackbarLayout.visibility = View.VISIBLE
        snackbarLayout.alpha = 0f
        snackbarLayout.translationY = 60f
        snackbarLayout.animate().alpha(1f).translationY(0f).setDuration(320)
            .setInterpolator(OvershootInterpolator(1.5f)).start()

        snackbarRunnable?.let { snackbarLayout.removeCallbacks(it) }
        snackbarRunnable = Runnable {
            snackbarLayout.animate().alpha(0f).translationY(30f).setDuration(280)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction { snackbarLayout.visibility = View.GONE; snackbarLayout.translationY = 0f }.start()
        }
        snackbarLayout.postDelayed(snackbarRunnable!!, 4000)

        snackbarLayout.setOnClickListener {
            snackbarRunnable?.let { r -> snackbarLayout.removeCallbacks(r) }
            snackbarLayout.visibility = View.GONE
            snackbarLayout.setOnClickListener(null)
            onUndo()
        }
    }

    private fun updateTodoCount() {
        val count = todos.size
        val completedToday = TodoStorage.getCompletedToday(this)
        val total = count + completedToday

        tvTodoCount.text = when (count) {
            0 -> if (completedToday > 0) "✅ Alles erledigt heute!" else ""
            1 -> "1 Aufgabe offen"
            else -> "$count Aufgaben offen"
        }
        tvTodoCount.alpha = 0f
        tvTodoCount.animate().alpha(1f).setDuration(300).start()

        // Fortschrittsbalken
        if (total > 0 && completedToday > 0) {
            val percent = (completedToday * 100) / total
            progressBar.visibility = View.VISIBLE
            progressBar.progress = percent
            tvProgressPercent.text = "$percent%"
        } else {
            progressBar.visibility = View.GONE
            tvProgressPercent.text = ""
        }
    }

    private fun showReminderDialog(todo: TodoItem) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reminder, null)
        val tvTodoText = dialogView.findViewById<TextView>(R.id.tvTodoText)
        val tvCurrentReminder = dialogView.findViewById<TextView>(R.id.tvCurrentReminder)
        val btn15min = dialogView.findViewById<LinearLayout>(R.id.btn15min)
        val btn30min = dialogView.findViewById<LinearLayout>(R.id.btn30min)
        val btn1hour = dialogView.findViewById<LinearLayout>(R.id.btn1hour)
        val btnTomorrow = dialogView.findViewById<LinearLayout>(R.id.btnTomorrow)
        val editDate = dialogView.findViewById<EditText>(R.id.editDate)
        val editTime = dialogView.findViewById<EditText>(R.id.editTime)
        val btnOk = dialogView.findViewById<Button>(R.id.btnOk)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)

        tvTodoText.text = if (todo.isPriority && todo.text.startsWith("!"))
            todo.text.removePrefix("!").trimStart() else todo.text

        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN)
        val sdfDate = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
        val sdfTime = SimpleDateFormat("HH:mm", Locale.GERMAN)

        if (todo.reminderTime != null) {
            tvCurrentReminder.text = "Erinnerung: ${sdf.format(Date(todo.reminderTime!!))}"
            tvCurrentReminder.visibility = View.VISIBLE
        } else {
            tvCurrentReminder.text = "Keine Erinnerung gesetzt"
            tvCurrentReminder.visibility = View.VISIBLE
        }

        val cal = Calendar.getInstance()
        todo.reminderTime?.let { cal.timeInMillis = it }
        editDate.setText(sdfDate.format(cal.time))
        editTime.setText(sdfTime.format(cal.time))

        val dialog = AlertDialog.Builder(this, R.style.ReminderDialogTheme)
            .setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        fun setReminder(timeMs: Long) {
            todo.reminderTime = timeMs
            saveTodos()
            AlarmScheduler.scheduleAlarm(this, todo)
            adapter.notifyDataSetChanged()
            showSnackbar("⏰ Erinnerung: ${sdf.format(Date(timeMs))}")
            dialog.dismiss()
        }

        btn15min.setOnClickListener { setReminder(System.currentTimeMillis() + 15 * 60 * 1000) }
        btn30min.setOnClickListener { setReminder(System.currentTimeMillis() + 30 * 60 * 1000) }
        btn1hour.setOnClickListener { setReminder(System.currentTimeMillis() + 60 * 60 * 1000) }
        btnTomorrow.setOnClickListener {
            val tomorrow = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
            }
            setReminder(tomorrow.timeInMillis)
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
            } catch (e: Exception) {
                Toast.makeText(this, "Ungültiges Datum/Uhrzeit", Toast.LENGTH_SHORT).show()
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showNotificationSettings() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)
        val switchCompliments = dialogView.findViewById<Switch>(R.id.switchCompliments)
        val switchKonfetti = dialogView.findViewById<Switch>(R.id.switchKonfetti)
        val switchNotifications = dialogView.findViewById<Switch>(R.id.switchNotifications)
        val switchDailySummary = dialogView.findViewById<Switch>(R.id.switchDailySummary)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)

        switchCompliments.isChecked = TodoStorage.isComplimentsEnabled(this)
        switchKonfetti.isChecked = TodoStorage.isKonfettiEnabled(this)
        switchNotifications.isChecked = TodoStorage.isNotificationsEnabled(this)
        switchDailySummary?.isChecked = TodoStorage.isDailySummaryEnabled(this)

        switchCompliments.setOnCheckedChangeListener { _, c -> TodoStorage.setComplimentsEnabled(this, c) }
        switchKonfetti.setOnCheckedChangeListener { _, c -> TodoStorage.setKonfettiEnabled(this, c) }
        switchNotifications.setOnCheckedChangeListener { _, c -> TodoStorage.setNotificationsEnabled(this, c) }
        switchDailySummary?.setOnCheckedChangeListener { _, c ->
            TodoStorage.setDailySummaryEnabled(this, c)
            if (c) DailySummaryReceiver.schedule(this) else DailySummaryReceiver.cancel(this)
        }

        val dialog = AlertDialog.Builder(this, R.style.ReminderDialogTheme)
            .setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showHelpDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_help, null)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)
        val dialog = AlertDialog.Builder(this, R.style.ReminderDialogTheme)
            .setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun saveTodos() {
        TodoStorage.saveTodos(this, todos)
    }

    private fun updateEmptyView() {
        if (todos.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            emptyView.alpha = 0f; emptyView.scaleX = 0.9f; emptyView.scaleY = 0.9f
            emptyView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(400)
                .setInterpolator(OvershootInterpolator(1.2f)).start()
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun scheduleDailySummaryIfNeeded() {
        if (TodoStorage.isDailySummaryEnabled(this)) {
            DailySummaryReceiver.schedule(this)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIF_PERMISSION_REQUEST)
            }
        }
    }

    private fun checkExactAlarmPermission() {
        if (!AlarmScheduler.canScheduleExactAlarms(this)) {
            AlertDialog.Builder(this)
                .setTitle("Exakte Alarme erlauben")
                .setMessage("Damit Erinnerungen zuverlässig funktionieren, muss TodoPro exakte Alarme setzen dürfen.")
                .setPositiveButton("Einstellungen öffnen") { _, _ ->
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    })
                }
                .setNegativeButton("Später", null).show()
        }
    }
}
