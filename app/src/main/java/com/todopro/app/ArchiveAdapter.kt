package com.todopro.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ArchiveAdapter(
    private val todos: MutableList<TodoItem>,
    private val onRestore: (TodoItem) -> Unit
) : RecyclerView.Adapter<ArchiveAdapter.ArchiveViewHolder>() {

    inner class ArchiveViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvText: TextView = view.findViewById(R.id.tvArchivedText)
        val tvDate: TextView = view.findViewById(R.id.tvArchivedDate)
        val tvCategory: TextView = view.findViewById(R.id.tvArchivedCategory)
        val btnRestore: ImageButton = view.findViewById(R.id.btnRestore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArchiveViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_archive, parent, false)
        return ArchiveViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArchiveViewHolder, position: Int) {
        val todo = todos[position]

        // Text (ohne ! Prefix)
        val displayText = if (todo.isPriority && todo.text.startsWith("!")) {
            todo.text.removePrefix("!").trimStart()
        } else todo.text
        holder.tvText.text = displayText

        // Erledigungsdatum
        val sdf = SimpleDateFormat("dd.MM.yy HH:mm", Locale.GERMAN)
        val dateStr = todo.completedAt?.let { "✅ ${sdf.format(Date(it))}" }
            ?: "✅ ${sdf.format(Date(todo.createdAt))}"
        holder.tvDate.text = dateStr

        // Kategorie
        val category = try { TodoCategory.valueOf(todo.category) } catch (e: Exception) { TodoCategory.NONE }
        if (category != TodoCategory.NONE) {
            holder.tvCategory.text = "${category.emoji} ${category.label}"
            holder.tvCategory.visibility = View.VISIBLE
        } else {
            holder.tvCategory.visibility = View.GONE
        }

        // Wiederherstellen Button
        holder.btnRestore.setOnClickListener {
            holder.btnRestore.animate()
                .scaleX(1.2f).scaleY(1.2f).setDuration(100)
                .withEndAction {
                    holder.btnRestore.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    onRestore(todo)
                }.start()
        }

        // Fade-in Animation
        holder.itemView.alpha = 0f
        holder.itemView.translationX = -30f
        holder.itemView.animate()
            .alpha(1f).translationX(0f).setDuration(300)
            .setStartDelay((position * 50L).coerceAtMost(400L))
            .start()
    }

    override fun getItemCount() = todos.size
}
