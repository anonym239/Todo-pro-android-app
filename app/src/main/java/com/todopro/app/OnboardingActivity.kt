package com.todopro.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

data class OnboardingPage(
    val icon: String,
    val title: String,
    val description: String,
    val highlightIcon: String = "",
    val highlightText: String = ""
)

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var dotsLayout: LinearLayout
    private lateinit var btnNext: TextView
    private lateinit var btnSkip: TextView

    private val pages = listOf(
        OnboardingPage(
            icon = "✦",
            title = "Willkommen bei\nTODOPRO",
            description = "Deine smarte To-Do App.\nOrganisiere deinen Tag – einfach, schnell und stylisch.",
            highlightIcon = "💡",
            highlightText = "Wische durch die Seiten um alles zu entdecken."
        ),
        OnboardingPage(
            icon = "✏️",
            title = "Aufgaben hinzufügen",
            description = "Tippe oben in das Eingabefeld und drücke den Haken-Button.\n\nMit einem ! am Anfang wird die Aufgabe als Priorität markiert.",
            highlightIcon = "⭐",
            highlightText = "Tipp: Schreibe !Wichtige Aufgabe für Priorität."
        ),
        OnboardingPage(
            icon = "✅",
            title = "Aufgaben erledigen",
            description = "Tippe auf den Haken links neben einer Aufgabe um sie als erledigt zu markieren.\n\nSie wird ins Archiv verschoben und dein Streak wächst!",
            highlightIcon = "🔥",
            highlightText = "Erledige täglich Aufgaben für einen langen Streak."
        ),
        OnboardingPage(
            icon = "☰",
            title = "Das Menü",
            description = "Tippe oben rechts auf die drei Striche um das Menü zu öffnen.\n\n• Statistiken – deine Fortschritte\n• Archiv – erledigte Aufgaben\n• Tagesziel – setze dir ein Ziel\n• Monats-Rückblick – Heatmap\n• Dark / Light Mode",
            highlightIcon = "🎯",
            highlightText = "Setze dir ein Tagesziel für extra Motivation!"
        ),
        OnboardingPage(
            icon = "⏰",
            title = "Erinnerungen & mehr",
            description = "Tippe auf das Glocken-Symbol bei einer Aufgabe um eine Erinnerung zu setzen.\n\nWenn die Erinnerung kommt, öffne die App – die Aufgabe wartet auf dich.\n\nWische eine Aufgabe nach links um sie zu löschen.",
            highlightIcon = "💡",
            highlightText = "Tipp: In der Benachrichtigung steht (Wischen = Todo erledigt) – einfach in der App nach links wischen!"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Theme anwenden
        val isDark = TodoStorage.isDarkMode(this)
        if (isDark) setTheme(R.style.Theme_TodoPro_Dark)
        else setTheme(R.style.Theme_TodoPro_Light)

        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        dotsLayout = findViewById(R.id.dotsLayout)
        btnNext = findViewById(R.id.btnNext)
        btnSkip = findViewById(R.id.btnSkip)

        viewPager.adapter = OnboardingAdapter(pages)
        viewPager.offscreenPageLimit = 1

        setupDots(0)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setupDots(position)
                updateButtons(position)
            }
        })

        btnNext.setOnClickListener {
            val current = viewPager.currentItem
            if (current < pages.size - 1) {
                viewPager.setCurrentItem(current + 1, true)
            } else {
                finishOnboarding()
            }
        }

        btnSkip.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun setupDots(selectedIndex: Int) {
        dotsLayout.removeAllViews()
        val density = resources.displayMetrics.density

        for (i in pages.indices) {
            val dot = View(this)
            val size = if (i == selectedIndex) (10 * density).toInt() else (7 * density).toInt()
            val params = LinearLayout.LayoutParams(size, size)
            params.setMargins((5 * density).toInt(), 0, (5 * density).toInt(), 0)
            dot.layoutParams = params

            // Runde Dots via background
            val bg = android.graphics.drawable.GradientDrawable()
            bg.shape = android.graphics.drawable.GradientDrawable.OVAL
            bg.setColor(
                if (i == selectedIndex) android.graphics.Color.parseColor("#CCFF00")
                else android.graphics.Color.parseColor("#444444")
            )
            dot.background = bg

            // Animation für aktiven Dot
            if (i == selectedIndex) {
                dot.scaleX = 0.5f
                dot.scaleY = 0.5f
                dot.animate().scaleX(1f).scaleY(1f).setDuration(300)
                    .setInterpolator(OvershootInterpolator(2f)).start()
            }

            dotsLayout.addView(dot)
        }
    }

    private fun updateButtons(position: Int) {
        if (position == pages.size - 1) {
            btnNext.text = "Los geht's ✓"
            btnSkip.visibility = View.INVISIBLE
        } else {
            btnNext.text = "Weiter →"
            btnSkip.visibility = View.VISIBLE
        }
    }

    private fun finishOnboarding() {
        TodoStorage.setFirstLaunchDone(this)
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    // ─── Adapter ─────────────────────────────────────────────────────────────

    inner class OnboardingAdapter(private val items: List<OnboardingPage>) :
        RecyclerView.Adapter<OnboardingAdapter.PageViewHolder>() {

        inner class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvIcon: TextView = view.findViewById(R.id.tvPageIcon)
            val tvTitle: TextView = view.findViewById(R.id.tvPageTitle)
            val tvDesc: TextView = view.findViewById(R.id.tvPageDesc)
            val highlightBox: LinearLayout = view.findViewById(R.id.highlightBox)
            val tvHighlightIcon: TextView = view.findViewById(R.id.tvHighlightIcon)
            val tvHighlightText: TextView = view.findViewById(R.id.tvHighlightText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_onboarding_page, parent, false)
            return PageViewHolder(view)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            val page = items[position]
            holder.tvIcon.text = page.icon
            holder.tvTitle.text = page.title
            holder.tvDesc.text = page.description

            if (page.highlightText.isNotEmpty()) {
                holder.highlightBox.visibility = View.VISIBLE
                holder.tvHighlightIcon.text = page.highlightIcon
                holder.tvHighlightText.text = page.highlightText
            } else {
                holder.highlightBox.visibility = View.GONE
            }

            // Einblend-Animation
            holder.itemView.alpha = 0f
            holder.itemView.translationY = 40f
            holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }

        override fun getItemCount() = items.size
    }
}
