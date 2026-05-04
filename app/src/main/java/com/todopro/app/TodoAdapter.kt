package com.todopro.app

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TodoAdapter(
    private val todos: MutableList<TodoItem>,
    private val onDelete: (TodoItem) -> Unit,
    private val onComplete: (TodoItem) -> Unit,
    private val onReminderClick: (TodoItem) -> Unit,
    private val onTextChanged: (TodoItem, String) -> Unit,
    private val onReminderSuggestion: ((TodoItem, Long) -> Unit)? = null,
    private val onCategoryChanged: ((TodoItem) -> Unit)? = null
) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    // ── Filter-State ────────────────────────────────────────────────────────
    private var searchQuery: String = ""
    private var filterCategory: TodoCategory = TodoCategory.NONE

    private val displayList: MutableList<TodoItem>
        get() {
            var list = todos.toList()
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.lowercase()
                list = list.filter { it.text.lowercase().contains(q) }
            }
            if (filterCategory != TodoCategory.NONE) {
                list = list.filter { it.category == filterCategory.name }
            }
            return list.toMutableList()
        }

    fun setSearchQuery(query: String) { searchQuery = query; notifyDataSetChanged() }
    fun setFilter(category: TodoCategory) { filterCategory = category; notifyDataSetChanged() }

    // ── ViewHolder ──────────────────────────────────────────────────────────
    inner class TodoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val editText: EditText             = view.findViewById(R.id.editTodoText)
        val btnComplete: ImageButton       = view.findViewById(R.id.btnComplete)
        val btnReminder: ImageButton       = view.findViewById(R.id.btnReminder)
        val tvReminderTime: TextView       = view.findViewById(R.id.tvReminderTime)
        val tvCreatedDate: TextView        = view.findViewById(R.id.tvCreatedDate)
        val tvCategoryBadge: TextView      = view.findViewById(R.id.tvCategoryBadge)
        val tvReminderSugg: TextView       = view.findViewById(R.id.tvReminderSuggestion)
        val tvSubtaskCount: TextView       = view.findViewById(R.id.tvSubtaskCount)
        val cardContent: LinearLayout      = view.findViewById(R.id.cardContent)
        val categoryBadgeRow: LinearLayout = view.findViewById(R.id.categoryBadgeRow)
        val priorityBar: View              = view.findViewById(R.id.priorityBar)
        val categoryBar: View              = view.findViewById(R.id.categoryBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TodoViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_todo, parent, false)
    )

    override fun getItemCount() = displayList.size

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        bindTodo(holder, displayList[position])
    }

    // ── Bind ────────────────────────────────────────────────────────────────
    private fun bindTodo(holder: TodoViewHolder, todo: TodoItem) {

        // Text
        holder.editText.removeTextChangedListener(holder.editText.tag as? TextWatcher)
        val displayText = if (todo.isPriority && todo.text.startsWith("!"))
            todo.text.removePrefix("!").trimStart() else todo.text
        holder.editText.setText(displayText)
        holder.editText.setSelection(displayText.length)

        // Priority bar
        holder.priorityBar.visibility = if (todo.isPriority) View.VISIBLE else View.GONE

        // Category
        val category = try { TodoCategory.valueOf(todo.category) } catch (_: Exception) { TodoCategory.NONE }
        if (category != TodoCategory.NONE) {
            holder.categoryBar.visibility = View.VISIBLE
            holder.categoryBar.setBackgroundColor(category.color)
            holder.categoryBadgeRow.visibility = View.VISIBLE
            holder.tvCategoryBadge.text = "${category.emoji} ${category.label}"
            holder.tvCategoryBadge.setTextColor(category.color)
        } else {
            holder.categoryBar.visibility = View.GONE
            holder.categoryBadgeRow.visibility = View.GONE
        }

        // Subtask badge
        if (todo.subtasks.isNotEmpty()) {
            val done = todo.subtasksDone.count { it }
            holder.tvSubtaskCount.text = "◾ $done/${todo.subtasks.size}"
            holder.tvSubtaskCount.visibility = View.VISIBLE
            holder.categoryBadgeRow.visibility = View.VISIBLE
        } else {
            holder.tvSubtaskCount.visibility = View.GONE
        }

        // Reminder
        if (todo.reminderTime != null) {
            val sdf = SimpleDateFormat("dd.MM. HH:mm", Locale.GERMAN)
            holder.tvReminderTime.text = "⏰ ${sdf.format(Date(todo.reminderTime!!))}"
            holder.tvReminderTime.visibility = View.VISIBLE
            holder.btnReminder.alpha = 1f
        } else {
            holder.tvReminderTime.visibility = View.GONE
            holder.btnReminder.alpha = 0.45f
        }

        // Created date
        holder.tvCreatedDate.text = SimpleDateFormat("d.M.", Locale.GERMAN).format(Date(todo.createdAt))

        // Reminder suggestion
        checkReminderSuggestion(holder, todo)

        // TextWatcher
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val typed = s?.toString() ?: ""
                if (typed.startsWith("!")) {
                    val clean = typed.removePrefix("!").trimStart()
                    todo.isPriority = true; todo.text = "!$clean"
                    onTextChanged(todo, "!$clean")
                    holder.editText.removeTextChangedListener(this)
                    holder.editText.setText(clean); holder.editText.setSelection(clean.length)
                    holder.editText.addTextChangedListener(this)
                    holder.priorityBar.visibility = View.VISIBLE
                } else {
                    todo.isPriority = false; todo.text = typed
                    onTextChanged(todo, typed)
                    holder.priorityBar.visibility = View.GONE
                    // Auto-Kategorisierung (nur wenn noch keine Kategorie gesetzt)
                    if (todo.category == TodoCategory.NONE.name && typed.length >= 4) {
                        val detected = AutoCategory.detect(typed)
                        if (detected != TodoCategory.NONE) {
                            todo.category = detected.name
                            onCategoryChanged?.invoke(todo)
                            holder.categoryBar.setBackgroundColor(detected.color)
                            holder.categoryBar.visibility = View.VISIBLE
                            holder.tvCategoryBadge.text = "${detected.emoji} ${detected.label}"
                            holder.tvCategoryBadge.setTextColor(detected.color)
                            if (holder.categoryBadgeRow.visibility != View.VISIBLE) {
                                holder.categoryBadgeRow.alpha = 0f
                                holder.categoryBadgeRow.visibility = View.VISIBLE
                                holder.categoryBadgeRow.animate().alpha(1f).setDuration(300).start()
                            }
                        }
                    }
                }
                checkReminderSuggestion(holder, todo)
            }
        }
        holder.editText.tag = watcher
        holder.editText.addTextChangedListener(watcher)

        // Reminder suggestion tap
        holder.tvReminderSugg.setOnClickListener {
            val t = parseSuggestedTime(todo.text)
            if (t != null) { onReminderSuggestion?.invoke(todo, t); holder.tvReminderSugg.visibility = View.GONE }
        }

        // Long press → context menu
        holder.cardContent.setOnLongClickListener { showContextMenu(holder, todo); true }

        // Complete
        holder.btnComplete.setOnClickListener {
            holder.btnComplete.isEnabled = false
            holder.btnComplete.animate().scaleX(1.35f).scaleY(1.35f).setDuration(100)
                .withEndAction {
                    holder.btnComplete.animate().scaleX(1f).scaleY(1f).setDuration(80)
                        .withEndAction {
                            holder.cardContent.animate()
                                .translationX(holder.cardContent.width.toFloat() + 100f)
                                .alpha(0f).setDuration(280)
                                .setInterpolator(DecelerateInterpolator())
                                .withEndAction { onComplete(todo) }.start()
                        }.start()
                }.start()
        }

        // Reminder button
        holder.btnReminder.setOnClickListener {
            holder.btnReminder.animate().scaleX(0.8f).scaleY(0.8f).setDuration(80)
                .withEndAction {
                    holder.btnReminder.animate().scaleX(1f).scaleY(1f).setDuration(120)
                        .setInterpolator(OvershootInterpolator()).start()
                }.start()
            onReminderClick(todo)
        }
    }

    // ── Smart Reminder-Vorschlag ────────────────────────────────────────────
    private fun checkReminderSuggestion(holder: TodoViewHolder, todo: TodoItem) {
        if (todo.reminderTime != null) { holder.tvReminderSugg.visibility = View.GONE; return }
        val t = parseSuggestedTime(todo.text)
        if (t != null) {
            holder.tvReminderSugg.text = "⏰ ${SimpleDateFormat("dd.MM. HH:mm", Locale.GERMAN).format(Date(t))} — tippe"
            holder.tvReminderSugg.visibility = View.VISIBLE
        } else {
            holder.tvReminderSugg.visibility = View.GONE
        }
    }

    // ── Zeit-Parser ─────────────────────────────────────────────────────────
    fun parseSuggestedTime(text: String): Long? {
        val lower = text.lowercase(Locale.GERMAN)
        val cal = Calendar.getInstance()
        Regex("""um\s+(\d{1,2})(?::(\d{2}))?\s*uhr?""").find(lower)?.let { m ->
            val h = m.groupValues[1].toIntOrNull() ?: return null
            val min = m.groupValues[2].toIntOrNull() ?: 0
            cal.set(Calendar.HOUR_OF_DAY, h); cal.set(Calendar.MINUTE, min); cal.set(Calendar.SECOND, 0)
            if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
            if (lower.contains("morgen")) cal.add(Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }
        if (lower.contains("morgen")) {
            cal.add(Calendar.DAY_OF_YEAR, 1); cal.set(Calendar.HOUR_OF_DAY, 9); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
            return cal.timeInMillis
        }
        if (lower.contains("heute")) {
            cal.add(Calendar.HOUR_OF_DAY, 1); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
            return cal.timeInMillis
        }
        Regex("""in\s+(\d+)\s*min""").find(lower)?.let { return System.currentTimeMillis() + (it.groupValues[1].toLongOrNull() ?: return null) * 60_000 }
        Regex("""in\s+(\d+)\s*stund""").find(lower)?.let { return System.currentTimeMillis() + (it.groupValues[1].toLongOrNull() ?: return null) * 3_600_000 }
        val weekdays = mapOf("montag" to Calendar.MONDAY,"dienstag" to Calendar.TUESDAY,"mittwoch" to Calendar.WEDNESDAY,"donnerstag" to Calendar.THURSDAY,"freitag" to Calendar.FRIDAY,"samstag" to Calendar.SATURDAY,"sonntag" to Calendar.SUNDAY)
        for ((day, calDay) in weekdays) {
            if (lower.contains(day)) {
                var d = calDay - cal.get(Calendar.DAY_OF_WEEK); if (d <= 0) d += 7
                cal.add(Calendar.DAY_OF_YEAR, d); cal.set(Calendar.HOUR_OF_DAY, 9); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                return cal.timeInMillis
            }
        }
        return null
    }

    // ── Context Menu ─────────────────────────────────────────────────────────
    private fun showContextMenu(holder: TodoViewHolder, todo: TodoItem) {
        val ctx = holder.itemView.context
        val options = arrayOf(
            "📂  Kategorie wählen",
            "📝  Subtask hinzufügen",
            if (todo.isPriority) "⬇️  Priorität entfernen" else "⬆️  Als Priorität markieren"
        )
        AlertDialog.Builder(ctx).setItems(options) { _, which ->
            when (which) {
                0 -> showCategoryPicker(holder, todo)
                1 -> showSubtaskInput(holder, todo)
                2 -> {
                    todo.isPriority = !todo.isPriority
                    todo.text = if (todo.isPriority) "!${todo.text.trimStart('!').trimStart()}"
                    else todo.text.removePrefix("!").trimStart()
                    onTextChanged(todo, todo.text)
                    holder.priorityBar.visibility = if (todo.isPriority) View.VISIBLE else View.GONE
                }
            }
        }.show()
    }

    private fun showCategoryPicker(holder: TodoViewHolder, todo: TodoItem) {
        val ctx = holder.itemView.context
        val cats = TodoCategory.values()
        val items = cats.map { if (it == TodoCategory.NONE) "❌  Keine" else "${it.emoji}  ${it.label}" }.toTypedArray()
        AlertDialog.Builder(ctx).setTitle("Kategorie wählen").setItems(items) { _, which ->
            val sel = cats[which]; todo.category = sel.name; onCategoryChanged?.invoke(todo)
            if (sel != TodoCategory.NONE) {
                holder.tvCategoryBadge.text = "${sel.emoji} ${sel.label}"
                holder.tvCategoryBadge.setTextColor(sel.color)
                holder.tvCategoryBadge.alpha = 0f; holder.tvCategoryBadge.visibility = View.VISIBLE
                holder.tvCategoryBadge.animate().alpha(1f).setDuration(300).start()
                holder.categoryBadgeRow.visibility = View.VISIBLE
                holder.categoryBar.visibility = View.VISIBLE; holder.categoryBar.setBackgroundColor(sel.color)
            } else {
                holder.tvCategoryBadge.visibility = View.GONE
                holder.categoryBar.visibility = View.GONE; holder.categoryBadgeRow.visibility = View.GONE
            }
        }.show()
    }

    private fun showSubtaskInput(holder: TodoViewHolder, todo: TodoItem) {
        val ctx = holder.itemView.context
        val input = EditText(ctx).apply { hint = "Subtask eingeben…"; setPadding(50, 24, 50, 24) }
        AlertDialog.Builder(ctx).setTitle("Subtask hinzufügen").setView(input)
            .setPositiveButton("Hinzufügen") { _, _ ->
                val t = input.text.toString().trim()
                if (t.isNotEmpty()) {
                    todo.subtasks.add(t); todo.subtasksDone.add(false)
                    onTextChanged(todo, todo.text)
                    val done = todo.subtasksDone.count { it }
                    holder.tvSubtaskCount.text = "◾ $done/${todo.subtasks.size}"
                    holder.tvSubtaskCount.visibility = View.VISIBLE
                    holder.categoryBadgeRow.visibility = View.VISIBLE
                }
            }.setNegativeButton("Abbrechen", null).show()
    }
}
