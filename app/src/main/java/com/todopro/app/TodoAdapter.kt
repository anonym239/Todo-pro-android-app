package com.todopro.app

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TodoAdapter(
    private val todos: MutableList<TodoItem>,
    private val onDelete: (TodoItem) -> Unit,
    private val onComplete: (TodoItem) -> Unit,
    private val onReminderClick: (TodoItem) -> Unit,
    private val onTextChanged: (TodoItem, String) -> Unit
) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    inner class TodoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val editText: EditText = view.findViewById(R.id.editTodoText)
        val btnComplete: ImageButton = view.findViewById(R.id.btnComplete)
        val btnReminder: ImageButton = view.findViewById(R.id.btnReminder)
        val tvReminderTime: TextView = view.findViewById(R.id.tvReminderTime)
        val tvCreatedDate: TextView = view.findViewById(R.id.tvCreatedDate)
        val cardContent: LinearLayout = view.findViewById(R.id.cardContent)
        val root: View = view.findViewById(R.id.todoItemRoot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return TodoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val todo = todos[position]

        // Slide-in animation when item appears
        val slideIn = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.fade_in)
        holder.itemView.startAnimation(slideIn)

        // Text setzen ohne TextWatcher auszulösen
        // ! am Anfang wird NICHT angezeigt - nur der reine Text
        holder.editText.removeTextChangedListener(holder.editText.tag as? TextWatcher)
        val displayText = if (todo.isPriority && todo.text.startsWith("!")) {
            todo.text.removePrefix("!").trimStart()
        } else {
            todo.text
        }
        holder.editText.setText(displayText)
        holder.editText.setSelection(holder.editText.text.length)

        // Prioritäts-Hintergrund: roter linker Rand direkt an der Karte
        if (todo.isPriority) {
            holder.cardContent.setBackgroundResource(R.drawable.todo_item_bg_priority)
        } else {
            holder.cardContent.setBackgroundResource(R.drawable.todo_item_bg)
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

        // Erstellungsdatum anzeigen (z.B. 26.4.26)
        val sdfDate = SimpleDateFormat("d.M.yy", Locale.GERMAN)
        holder.tvCreatedDate.text = sdfDate.format(Date(todo.createdAt))

        // TextWatcher für Auto-Save
        // Wenn User tippt: ! am Anfang → isPriority setzen, aber ! nicht im Text speichern
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val typed = s?.toString() ?: ""
                if (typed.startsWith("!")) {
                    // ! am Anfang → Prio setzen, aber ! aus Text entfernen
                    val cleanText = typed.removePrefix("!").trimStart()
                    todo.isPriority = true
                    todo.text = "!$cleanText" // intern mit ! speichern für Sortierung
                    onTextChanged(todo, "!$cleanText")
                    // Cursor-Position nach dem Entfernen des ! korrigieren
                    holder.editText.removeTextChangedListener(this)
                    holder.editText.setText(cleanText)
                    holder.editText.setSelection(cleanText.length)
                    holder.editText.addTextChangedListener(this)
                    // Hintergrund sofort aktualisieren
                    holder.cardContent.setBackgroundResource(R.drawable.todo_item_bg_priority)
                } else {
                    todo.isPriority = false
                    todo.text = typed
                    onTextChanged(todo, typed)
                    holder.cardContent.setBackgroundResource(R.drawable.todo_item_bg)
                }
            }
        }
        holder.editText.tag = watcher
        holder.editText.addTextChangedListener(watcher)

        // Complete button: scale press + swipe-right animation then remove
        holder.btnComplete.setOnClickListener {
            holder.btnComplete.isEnabled = false

            holder.btnComplete.animate()
                .scaleX(1.3f)
                .scaleY(1.3f)
                .setDuration(120)
                .withEndAction {
                    holder.btnComplete.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(80)
                        .withEndAction {
                            val slideRight = AnimationUtils.loadAnimation(
                                holder.itemView.context,
                                R.anim.slide_right_out
                            )
                            slideRight.fillAfter = true
                            holder.cardContent.startAnimation(slideRight)
                            holder.cardContent.postDelayed({
                                onComplete(todo)
                            }, 360)
                        }.start()
                }.start()
        }

        // Reminder button with scale animation
        holder.btnReminder.setOnClickListener {
            holder.btnReminder.animate()
                .scaleX(0.85f).scaleY(0.85f).setDuration(80)
                .withEndAction {
                    holder.btnReminder.animate()
                        .scaleX(1f).scaleY(1f).setDuration(120)
                        .setInterpolator(android.view.animation.OvershootInterpolator())
                        .start()
                }.start()
            onReminderClick(todo)
        }
    }

    override fun getItemCount() = todos.size
}
