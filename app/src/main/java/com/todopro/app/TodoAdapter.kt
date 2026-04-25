package com.todopro.app

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
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
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        val btnComplete: ImageButton = view.findViewById(R.id.btnComplete)
        val btnReminder: ImageButton = view.findViewById(R.id.btnReminder)
        val tvReminderTime: TextView = view.findViewById(R.id.tvReminderTime)
        val priorityIndicator: View = view.findViewById(R.id.priorityIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return TodoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val todo = todos[position]

        // Text setzen ohne TextWatcher auszulösen
        holder.editText.removeTextChangedListener(holder.editText.tag as? TextWatcher)
        holder.editText.setText(todo.text)
        holder.editText.setSelection(holder.editText.text.length)

        // Priorität anzeigen (! am Anfang)
        if (todo.isPriority) {
            holder.priorityIndicator.visibility = View.VISIBLE
        } else {
            holder.priorityIndicator.visibility = View.GONE
        }

        // Reminder-Zeit anzeigen
        if (todo.reminderTime != null) {
            val sdf = SimpleDateFormat("dd.MM. HH:mm", Locale.GERMAN)
            holder.tvReminderTime.text = sdf.format(Date(todo.reminderTime!!))
            holder.tvReminderTime.visibility = View.VISIBLE
            holder.btnReminder.setImageResource(R.drawable.ic_bell_active)
        } else {
            holder.tvReminderTime.visibility = View.GONE
            holder.btnReminder.setImageResource(R.drawable.ic_bell)
        }

        // TextWatcher für Auto-Save
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val newText = s?.toString() ?: ""
                onTextChanged(todo, newText)
            }
        }
        holder.editText.tag = watcher
        holder.editText.addTextChangedListener(watcher)

        holder.btnDelete.setOnClickListener { onDelete(todo) }
        holder.btnComplete.setOnClickListener { onComplete(todo) }
        holder.btnReminder.setOnClickListener { onReminderClick(todo) }
    }

    override fun getItemCount() = todos.size
}
