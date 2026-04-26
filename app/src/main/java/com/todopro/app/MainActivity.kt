package com.todopro.app

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
    private lateinit var emptyView: LinearLayout
    private lateinit var konfettiView: KonfettiView
    private lateinit var tvSnackbar: TextView
    private lateinit var snackbarLayout: LinearLayout
    private lateinit var header: LinearLayout
    private lateinit var inputRow: LinearLayout

    private val todos = mutableListOf<TodoItem>()
    private var isDarkMode = true
    private var snackbarRunnable: Runnable? = null

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
        setupListeners()
        setupButtonAnimations()
        requestNotificationPermission()
        checkExactAlarmPermission()
        updateEmptyView()

        // Header Slide-Down Animation beim Start
        animateHeaderIn()
    }

    private fun applyTheme() {
        if (isDarkMode) {
            setTheme(R.style.Theme_TodoPro_Dark)
        } else {
            setTheme(R.style.Theme_TodoPro_Light)
        }
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        editInput = findViewById(R.id.editInput)
        btnAdd = findViewById(R.id.btnAdd)
        btnTheme = findViewById(R.id.btnTheme)
        btnNotifications = findViewById(R.id.btnNotifications)
        btnHelp = findViewById(R.id.btnHelp)
        emptyView = findViewById(R.id.emptyView)
        konfettiView = findViewById(R.id.konfettiView)
        tvSnackbar = findViewById(R.id.tvSnackbar)
        snackbarLayout = findViewById(R.id.snackbarLayout)
        header = findViewById(R.id.header)
        inputRow = findViewById(R.id.inputRow)
    }

    private fun animateHeaderIn() {
        // Header von oben reinschieben
        header.translationY = -100f
        header.alpha = 0f
        header.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(400)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        // Input Row mit Verzögerung
        inputRow.alpha = 0f
        inputRow.translationY = 30f
        inputRow.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setStartDelay(200)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    private fun setupButtonAnimations() {
        // Scale-Animation für alle Buttons
        listOf(btnAdd, btnTheme, btnNotifications, btnHelp).forEach { btn ->
            btn.setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(100).start()
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(150)
                            .setInterpolator(android.view.animation.OvershootInterpolator())
                            .start()
                    }
                }
                false
            }
        }
    }

    private fun loadTodos() {
        todos.clear()
        todos.addAll(TodoStorage.loadTodos(this))
        sortTodos()
    }

    private fun sortTodos() {
        todos.sortWith(compareByDescending<TodoItem> { it.isPriority }.thenBy { it.text })
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
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Item Animator für smooth Animationen
        recyclerView.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator().apply {
            addDuration = 300
            removeDuration = 250
            moveDuration = 200
        }
    }

    private fun setupListeners() {
        btnAdd.setOnClickListener { addTodo() }

        editInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTodo()
                true
            } else false
        }

        btnTheme.setOnClickListener {
            isDarkMode = !isDarkMode
            TodoStorage.setDarkMode(this, isDarkMode)
            recreate()
        }

        btnNotifications.setOnClickListener {
            showNotificationSettings()
        }

        btnHelp.setOnClickListener {
            showHelpDialog()
        }
    }

    private fun addTodo() {
        val text = editInput.text.toString().trim()
        if (text.isEmpty()) {
            // Shake-Animation wenn leer
            editInput.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake_input))
            return
        }

        val todo = TodoItem(
            text = text,
            isPriority = text.startsWith("!")
        )
        todos.add(0, todo)
        sortTodos()
        val insertPos = todos.indexOf(todo)
        adapter.notifyItemInserted(insertPos)
        saveTodos()
        editInput.setText("")
        updateEmptyView()

        // Add-Button kurz aufleuchten
        btnAdd.animate().scaleX(1.15f).scaleY(1.15f).setDuration(100).withEndAction {
            btnAdd.animate().scaleX(1f).scaleY(1f).setDuration(150)
                .setInterpolator(android.view.animation.OvershootInterpolator()).start()
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
        }
    }

    private fun completeTodo(todo: TodoItem) {
        AlarmScheduler.cancelAlarm(this, todo)
        val index = todos.indexOf(todo)
        if (index >= 0) {
            todos.removeAt(index)
            adapter.notifyItemRemoved(index)
            saveTodos()
            updateEmptyView()

            if (TodoStorage.isKonfettiEnabled(this)) {
                launchKonfetti()
            }

            if (TodoStorage.isComplimentsEnabled(this)) {
                showCompliment()
            }
        }
    }

    private fun launchKonfetti() {
        val colors = listOf(0xFFCCFF00.toInt(), 0xFF00FF88.toInt(), 0xFFFFD700.toInt(),
            0xFFFF6B6B.toInt(), 0xFF4ECDC4.toInt(), 0xFFFFFFFF.toInt())

        konfettiView.start(
            Party(
                speed = 0f,
                maxSpeed = 30f,
                damping = 0.9f,
                spread = 360,
                colors = colors,
                emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
                position = Position.Relative(0.5, 0.3)
            )
        )
    }

    private fun showCompliment() {
        val compliments = listOf(
            "🎉 Großartig! Weiter so!",
            "💪 Du rockst das!",
            "⭐ Aufgabe erledigt! Klasse!",
            "🚀 Produktivitäts-Monster!",
            "✨ Perfekt gemacht!",
            "🏆 Champion!",
            "🔥 On fire heute!"
        )
        showSnackbar(compliments.random())
    }

    private fun showSnackbar(message: String) {
        tvSnackbar.text = message
        snackbarLayout.visibility = View.VISIBLE
        snackbarLayout.alpha = 0f
        snackbarLayout.translationY = 40f
        snackbarLayout.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        snackbarRunnable?.let { snackbarLayout.removeCallbacks(it) }
        snackbarRunnable = Runnable {
            snackbarLayout.animate().alpha(0f).translationY(20f).setDuration(300).withEndAction {
                snackbarLayout.visibility = View.GONE
                snackbarLayout.translationY = 0f
            }.start()
        }
        snackbarLayout.postDelayed(snackbarRunnable!!, 3000)
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

        tvTodoText.text = todo.text

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
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        fun setReminder(timeMs: Long) {
            todo.reminderTime = timeMs
            saveTodos()
            AlarmScheduler.scheduleAlarm(this, todo)
            adapter.notifyDataSetChanged()
            val formatted = sdf.format(Date(timeMs))
            showSnackbar("⏰ Erinnerung gesetzt: $formatted")
            dialog.dismiss()
        }

        btn15min.setOnClickListener { setReminder(System.currentTimeMillis() + 15 * 60 * 1000) }
        btn30min.setOnClickListener { setReminder(System.currentTimeMillis() + 30 * 60 * 1000) }
        btn1hour.setOnClickListener { setReminder(System.currentTimeMillis() + 60 * 60 * 1000) }
        btnTomorrow.setOnClickListener {
            val tomorrow = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            setReminder(tomorrow.timeInMillis)
        }

        editDate.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                val picked = Calendar.getInstance().apply { set(y, m, d) }
                editDate.setText(sdfDate.format(picked.time))
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
                val dateStr = editDate.text.toString()
                val timeStr = editTime.text.toString()
                val combined = sdf.parse("$dateStr $timeStr")
                if (combined != null) {
                    if (combined.time > System.currentTimeMillis()) {
                        setReminder(combined.time)
                    } else {
                        Toast.makeText(this, "Zeitpunkt liegt in der Vergangenheit!", Toast.LENGTH_SHORT).show()
                    }
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
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)

        switchCompliments.isChecked = TodoStorage.isComplimentsEnabled(this)
        switchKonfetti.isChecked = TodoStorage.isKonfettiEnabled(this)
        switchNotifications.isChecked = TodoStorage.isNotificationsEnabled(this)

        switchCompliments.setOnCheckedChangeListener { _, checked ->
            TodoStorage.setComplimentsEnabled(this, checked)
        }
        switchKonfetti.setOnCheckedChangeListener { _, checked ->
            TodoStorage.setKonfettiEnabled(this, checked)
        }
        switchNotifications.setOnCheckedChangeListener { _, checked ->
            TodoStorage.setNotificationsEnabled(this, checked)
        }

        val dialog = AlertDialog.Builder(this, R.style.ReminderDialogTheme)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showHelpDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_help, null)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)

        val dialog = AlertDialog.Builder(this, R.style.ReminderDialogTheme)
            .setView(dialogView)
            .create()
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
            emptyView.alpha = 0f
            emptyView.animate().alpha(1f).setDuration(400).start()
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIF_PERMISSION_REQUEST
                )
            }
        }
    }

    private fun checkExactAlarmPermission() {
        if (!AlarmScheduler.canScheduleExactAlarms(this)) {
            AlertDialog.Builder(this)
                .setTitle("Exakte Alarme erlauben")
                .setMessage("Damit Erinnerungen zuverlässig funktionieren (auch wenn das Handy gesperrt ist), muss TodoPro exakte Alarme setzen dürfen.\n\nBitte erlaube dies in den Einstellungen.")
                .setPositiveButton("Einstellungen öffnen") { _, _ ->
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                }
                .setNegativeButton("Später", null)
                .show()
        }
    }
}
