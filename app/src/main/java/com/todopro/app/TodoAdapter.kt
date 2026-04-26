package com.todopro.app

import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
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

    inner class TodoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val editText: EditText = view.findViewById(R.id.editTodoText)
        val btnComplete: ImageButton = view.findViewById(R.id.btnComplete)
        val btnReminder: ImageButton = view.findViewById(R.id.btnReminder)
        val tvReminderTime: TextView = view.findViewById(R.id.tvReminderTime)
        val tvCreatedDate: TextView = view.findViewById(R.id.tvCreatedDate)
        val tvCategoryBadge: TextView = view.findViewById(R.id.tvCategoryBadge)
        val tvReminderSuggestion: TextView = view.findViewById(R.id.tvReminderSuggestion)
        val cardContent: LinearLayout = view.findViewById(R.id.cardContent)
        val priorityBar: View = view.findViewById(R.id.priorityBar)
        val categoryBar: View = view.findViewById(R.id.categoryBar)
        val root: View = view.findViewById(R.id.todoItemRoot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return TodoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val todo = todos[position]

        // Slide-in animation
        val slideIn = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.fade_in)
        holder.itemView.startAnimation(slideIn)

        // Text setzen ohne TextWatcher
        holder.editText.removeTextChangedListener(holder.editText.tag as? TextWatcher)
        val displayText = if (todo.isPriority && todo.text.startsWith("!")) {
            todo.text.removePrefix("!").trimStart()
        } else {
            todo.text
        }
        holder.editText.setText(displayText)
        holder.editText.setSelection(holder.editText.text.length)

        // Prioritäts-Streifen
        holder.priorityBar.visibility = if (todo.isPriority) View.VISIBLE else View.GONE

        // Kategorie-Streifen + Badge
        val category = try { TodoCategory.valueOf(todo.category) } catch (e: Exception) { TodoCategory.NONE }
        if (category != TodoCategory.NONE) {
            holder.categoryBar.visibility = View.VISIBLE
            holder.categoryBar.setBackgroundColor(category.color)
            holder.tvCategoryBadge.visibility = View.VISIBLE
            holder.tvCategoryBadge.text = "${category.emoji} ${category.label}"
        } else {
            holder.categoryBar.visibility = View.GONE
            holder.tvCategoryBadge.visibility = View.GONE
        }

        // Reminder-Zeit anzeigen
        if (todo.reminderTime != null) {
            val sdf = SimpleDateFormat("dd.MM. HH:mm", Locale.GERMAN)
            holder.tvReminderTime.text = "⏰ ${sdf.format(Date(todo.reminderTime!!))}"
            holder.tvReminderTime.visibility = View.VISIBLE
            holder.btnReminder.setImageResource(R.drawable.ic_bell_active)
        } else {
            holder.tvReminderTime.visibility = View.GONE
            holder.btnReminder.setImageResource(R.drawable.ic_bell)
        }

        // Erstellungsdatum
        val sdfDate = SimpleDateFormat("d.M.yy", Locale.GERMAN)
        holder.tvCreatedDate.text = sdfDate.format(Date(todo.createdAt))

        // Erinnerungsvorschlag prüfen
        checkReminderSuggestion(holder, todo)

        // TextWatcher
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val typed = s?.toString() ?: ""
                if (typed.startsWith("!")) {
                    val cleanText = typed.removePrefix("!").trimStart()
                    todo.isPriority = true
                    todo.text = "!$cleanText"
                    onTextChanged(todo, "!$cleanText")
                    holder.editText.removeTextChangedListener(this)
                    holder.editText.setText(cleanText)
                    holder.editText.setSelection(cleanText.length)
                    holder.editText.addTextChangedListener(this)
                    holder.priorityBar.visibility = View.VISIBLE
                } else {
                    todo.isPriority = false
                    todo.text = typed
                    onTextChanged(todo, typed)
                    holder.priorityBar.visibility = View.GONE
                }
                // Erinnerungsvorschlag live aktualisieren
                checkReminderSuggestion(holder, todo)
            }
        }
        holder.editText.tag = watcher
        holder.editText.addTextChangedListener(watcher)

        // Erinnerungsvorschlag antippen → Erinnerung setzen
        holder.tvReminderSuggestion.setOnClickListener {
            val suggestedTime = parseSuggestedTime(todo.text)
            if (suggestedTime != null) {
                onReminderSuggestion?.invoke(todo, suggestedTime)
                holder.tvReminderSuggestion.visibility = View.GONE
                // Kurze Bestätigungs-Animation
                holder.tvReminderSuggestion.animate()
                    .scaleX(1.1f).scaleY(1.1f).setDuration(100)
                    .withEndAction {
                        holder.tvReminderSuggestion.animate()
                            .scaleX(1f).scaleY(1f).setDuration(100).start()
                    }.start()
            }
        }

        // Langer Druck → Kategorie auswählen
        holder.cardContent.setOnLongClickListener {
            showCategoryPicker(holder, todo)
            true
        }

        // Complete button
        holder.btnComplete.setOnClickListener {
            holder.btnComplete.isEnabled = false
            holder.btnComplete.animate()
                .scaleX(1.3f).scaleY(1.3f).setDuration(120)
                .withEndAction {
                    holder.btnComplete.animate()
                        .scaleX(1f).scaleY(1f).setDuration(80)
                        .withEndAction {
                            val slideRight = AnimationUtils.loadAnimation(
                                holder.itemView.context, R.anim.slide_right_out
                            )
                            slideRight.fillAfter = true
                            holder.cardContent.startAnimation(slideRight)
                            holder.cardContent.postDelayed({ onComplete(todo) }, 360)
                        }.start()
                }.start()
        }

        // Reminder button
        holder.btnReminder.setOnClickListener {
            holder.btnReminder.animate()
                .scaleX(0.85f).scaleY(0.85f).setDuration(80)
                .withEndAction {
                    holder.btnReminder.animate()
                        .scaleX(1f).scaleY(1f).setDuration(120)
                        .setInterpolator(OvershootInterpolator())
                        .start()
                }.start()
            onReminderClick(todo)
        }
    }

    // Erinnerungsvorschlag: Zeitwörter im Text erkennen
    private fun checkReminderSuggestion(holder: TodoViewHolder, todo: TodoItem) {
        // Kein Vorschlag wenn schon eine Erinnerung gesetzt ist
        if (todo.reminderTime != null) {
            holder.tvReminderSuggestion.visibility = View.GONE
            return
        }

        val suggestedTime = parseSuggestedTime(todo.text)
        if (suggestedTime != null) {
            val sdf = SimpleDateFormat("dd.MM. HH:mm", Locale.GERMAN)
            holder.tvReminderSuggestion.text = "💡 Erinnerung: ${sdf.format(Date(suggestedTime))} tippen"
            holder.tvReminderSuggestion.visibility = View.VISIBLE
        } else {
            holder.tvReminderSuggestion.visibility = View.GONE
        }
    }

    // Zeitwörter parsen und Timestamp zurückgeben
    fun parseSuggestedTime(text: String): Long? {
        val lower = text.lowercase(Locale.GERMAN)
        val cal = Calendar.getInstance()

        // "um HH:mm" oder "um HH Uhr"
        val timePattern = Regex("""um\s+(\d{1,2})(?::(\d{2}))?\s*uhr?""")
        val timeMatch = timePattern.find(lower)
        if (timeMatch != null) {
            val hour = timeMatch.groupValues[1].toIntOrNull() ?: return null
            val minute = timeMatch.groupValues[2].toIntOrNull() ?: 0
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)

            // Wenn Zeit in der Vergangenheit → morgen
            if (cal.timeInMillis <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }

            // "morgen um X" → +1 Tag
            if (lower.contains("morgen")) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return cal.timeInMillis
        }

        // "morgen" ohne Uhrzeit → morgen 9 Uhr
        if (lower.contains("morgen")) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            return cal.timeInMillis
        }

        // "heute" → heute in 1 Stunde
        if (lower.contains("heute")) {
            cal.add(Calendar.HOUR_OF_DAY, 1)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            return cal.timeInMillis
        }

        // "in X minuten/stunden"
        val inMinPattern = Regex("""in\s+(\d+)\s*min""")
        val inMinMatch = inMinPattern.find(lower)
        if (inMinMatch != null) {
            val minutes = inMinMatch.groupValues[1].toLongOrNull() ?: return null
            return System.currentTimeMillis() + minutes * 60 * 1000
        }

        val inHourPattern = Regex("""in\s+(\d+)\s*stund""")
        val inHourMatch = inHourPattern.find(lower)
        if (inHourMatch != null) {
            val hours = inHourMatch.groupValues[1].toLongOrNull() ?: return null
            return System.currentTimeMillis() + hours * 60 * 60 * 1000
        }

        // Wochentage
        val weekdays = mapOf(
            "montag" to Calendar.MONDAY, "dienstag" to Calendar.TUESDAY,
            "mittwoch" to Calendar.WEDNESDAY, "donnerstag" to Calendar.THURSDAY,
            "freitag" to Calendar.FRIDAY, "samstag" to Calendar.SATURDAY,
            "sonntag" to Calendar.SUNDAY
        )
        for ((day, calDay) in weekdays) {
            if (lower.contains(day)) {
                val today = cal.get(Calendar.DAY_OF_WEEK)
                var daysUntil = calDay - today
                if (daysUntil <= 0) daysUntil += 7
                cal.add(Calendar.DAY_OF_YEAR, daysUntil)
                cal.set(Calendar.HOUR_OF_DAY, 9)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                return cal.timeInMillis
            }
        }

        return null
    }

    // Kategorie-Auswahl Dialog
    private fun showCategoryPicker(holder: TodoViewHolder, todo: TodoItem) {
        val context = holder.itemView.context
        val categories = TodoCategory.values()
        val items = categories.map { cat ->
            if (cat == TodoCategory.NONE) "❌ Keine Kategorie"
            else "${cat.emoji} ${cat.label}"
        }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle("Kategorie wählen")
            .setItems(items) { _, which ->
                val selected = categories[which]
                todo.category = selected.name
                onCategoryChanged?.invoke(todo)

                // Kategorie-Badge animiert einblenden
                if (selected != TodoCategory.NONE) {
                    holder.tvCategoryBadge.text = "${selected.emoji} ${selected.label}"
                    holder.tvCategoryBadge.alpha = 0f
                    holder.tvCategoryBadge.visibility = View.VISIBLE
                    holder.tvCategoryBadge.animate().alpha(1f).setDuration(300).start()
                    holder.categoryBar.visibility = View.VISIBLE
                    holder.categoryBar.setBackgroundColor(selected.color)
                } else {
                    holder.tvCategoryBadge.visibility = View.GONE
                    holder.categoryBar.visibility = View.GONE
                }
            }
            .show()
    }

    override fun getItemCount() = todos.size
}
