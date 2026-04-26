package com.todopro.app

import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ArchiveActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: LinearLayout
    private lateinit var btnClose: ImageButton
    private lateinit var tvArchiveCount: TextView

    private val completedTodos = mutableListOf<TodoItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (TodoStorage.isDarkMode(this)) setTheme(R.style.Theme_TodoPro_Dark)
        else setTheme(R.style.Theme_TodoPro_Light)

        setContentView(R.layout.activity_archive)

        recyclerView = findViewById(R.id.recyclerViewArchive)
        emptyView = findViewById(R.id.emptyViewArchive)
        btnClose = findViewById(R.id.btnClose)
        tvArchiveCount = findViewById(R.id.tvArchiveCount)

        btnClose.setOnClickListener { finish() }

        loadAndDisplay()
        animateIn()
    }

    private fun loadAndDisplay() {
        completedTodos.clear()
        completedTodos.addAll(TodoStorage.loadCompletedTodos(this).reversed())

        tvArchiveCount.text = if (completedTodos.isEmpty()) "" else "${completedTodos.size} erledigt"

        if (completedTodos.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            setupRecyclerView()
        }
    }

    private fun setupRecyclerView() {
        val adapter = ArchiveAdapter(completedTodos) { todo ->
            // Wiederherstellen: Todo zurück in die aktive Liste
            val activeTodos = TodoStorage.loadTodos(this).toMutableList()
            val restored = todo.copy(
                isCompleted = false,
                completedAt = null,
                reminderTime = null
            )
            activeTodos.add(0, restored)
            TodoStorage.saveTodos(this, activeTodos)

            // Aus Archiv entfernen
            completedTodos.remove(todo)
            val completed = TodoStorage.loadCompletedTodos(this).toMutableList()
            completed.removeAll { it.id == todo.id }
            TodoStorage.saveCompletedTodos(this, completed)

            adapter?.notifyDataSetChanged()
            tvArchiveCount.text = if (completedTodos.isEmpty()) "" else "${completedTodos.size} erledigt"

            if (completedTodos.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private var adapter: ArchiveAdapter? = null

    private fun animateIn() {
        val header = findViewById<View>(R.id.archiveHeader)
        header.translationY = -80f
        header.alpha = 0f
        header.animate().translationY(0f).alpha(1f).setDuration(400)
            .setInterpolator(OvershootInterpolator(1.2f)).start()

        recyclerView.alpha = 0f
        recyclerView.translationY = 40f
        recyclerView.animate().alpha(1f).translationY(0f).setDuration(400)
            .setStartDelay(150).start()
    }
}
