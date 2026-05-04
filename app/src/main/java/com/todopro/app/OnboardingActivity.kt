package com.todopro.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
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

    // ── 4 hochwertige, verkaufsorientierte Slides ──────────────────────────
    private val pages = listOf(
        OnboardingPage(
            icon = "✦",
            title = "Willkommen bei\nTODO PRO",
            description = "Die smarte To-Do App, die dich wirklich produktiv macht — elegant, schnell und immer einen Schritt voraus.",
            highlightIcon = "🚀",
            highlightText = "Keine langen Tutorials. Einfach loslegen — in 30 Sekunden."
        ),
        OnboardingPage(
            icon = "⚡",
            title = "Blitzschnell\nAufgaben erfassen",
            description = "Tippe deine Aufgabe ein und bestätige mit dem +-Button.\n\nSchreibe ! am Anfang für eine Prioritäts-Aufgabe — die immer ganz oben bleibt.",
            highlightIcon = "💡",
            highlightText = "Smart: Schreibe z. B. \"Arzt morgen um 10 Uhr\" — TodoPro erkennt die Zeit automatisch."
        ),
        OnboardingPage(
            icon = "🔥",
            title = "Baue Gewohnheiten\nmit Streaks auf",
            description = "Erledige täglich Aufgaben und halte deinen Streak am Leben.\n\nSuche & filtere nach Kategorien, setze Tagesziele und behalte den Überblick mit der Monats-Heatmap.",
            highlightIcon = "🎯",
            highlightText = "7-Tage-Streak = besondere Belohnung. Schaffst du es?"
        ),
        OnboardingPage(
            icon = "⏰",
            title = "Nie wieder\netwas vergessen",
            description = "Tippe auf die Glocke einer Aufgabe, um eine präzise Erinnerung zu setzen.\n\nWische links = löschen · Wische rechts = sofort erledigen.",
            highlightIcon = "✅",
            highlightText = "Alle deine Daten bleiben lokal auf deinem Gerät — komplett offline."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_TodoPro_Dark)
        setContentView(R.layout.activity_onboarding)

        viewPager  = findViewById(R.id.viewPager)
        dotsLayout = findViewById(R.id.dotsLayout)
        btnNext    = findViewById(R.id.btnNext)
        btnSkip    = findViewById(R.id.btnSkip)

        viewPager.adapter = OnboardingAdapter(pages)
        viewPager.offscreenPageLimit = 1

        setupDots(0)
        updateButtons(0)

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
        btnSkip.setOnClickListener { finishOnboarding() }
    }

    // ── Dot-Indicator ──────────────────────────────────────────────────────
    private fun setupDots(selectedIndex: Int) {
        dotsLayout.removeAllViews()
        val dp = resources.displayMetrics.density

        for (i in pages.indices) {
            val isSelected = i == selectedIndex
            val dot = View(this)
            val w = if (isSelected) (24 * dp).toInt() else (8 * dp).toInt()
            val h = (8 * dp).toInt()
            val params = LinearLayout.LayoutParams(w, h)
            params.setMargins((4 * dp).toInt(), 0, (4 * dp).toInt(), 0)
            dot.layoutParams = params

            val bg = android.graphics.drawable.GradientDrawable()
            bg.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            bg.cornerRadius = 4 * dp
            bg.setColor(
                if (isSelected) android.graphics.Color.parseColor("#CCFF00")
                else android.graphics.Color.parseColor("#333355")
            )
            dot.background = bg

            if (isSelected) {
                dot.scaleX = 0.4f
                dot.animate().scaleX(1f).setDuration(300)
                    .setInterpolator(OvershootInterpolator(2f)).start()
            }
            dotsLayout.addView(dot)
        }
    }

    // ── Buttons ────────────────────────────────────────────────────────────
    private fun updateButtons(position: Int) {
        if (position == pages.size - 1) {
            btnNext.text = "Jetzt starten  ✓"
            btnSkip.visibility = View.INVISIBLE
        } else {
            btnNext.text = "Weiter"
            btnSkip.visibility = View.VISIBLE
        }
    }

    private fun finishOnboarding() {
        TodoStorage.setFirstLaunchDone(this)
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    // ── ViewPager Adapter ──────────────────────────────────────────────────
    inner class OnboardingAdapter(private val items: List<OnboardingPage>) :
        RecyclerView.Adapter<OnboardingAdapter.PageViewHolder>() {

        inner class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvIcon: TextView         = view.findViewById(R.id.tvPageIcon)
            val tvTitle: TextView        = view.findViewById(R.id.tvPageTitle)
            val tvDesc: TextView         = view.findViewById(R.id.tvPageDesc)
            val highlightBox: LinearLayout = view.findViewById(R.id.highlightBox)
            val tvHighlightIcon: TextView  = view.findViewById(R.id.tvHighlightIcon)
            val tvHighlightText: TextView  = view.findViewById(R.id.tvHighlightText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PageViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_onboarding_page, parent, false)
        )

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            val page = items[position]
            holder.tvIcon.text  = page.icon
            holder.tvTitle.text = page.title
            holder.tvDesc.text  = page.description

            if (page.highlightText.isNotEmpty()) {
                holder.highlightBox.visibility = View.VISIBLE
                holder.tvHighlightIcon.text    = page.highlightIcon
                holder.tvHighlightText.text    = page.highlightText
            } else {
                holder.highlightBox.visibility = View.GONE
            }

            // Stagger-Einblendung
            holder.itemView.alpha       = 0f
            holder.itemView.translationY = 50f
            holder.itemView.animate()
                .alpha(1f).translationY(0f)
                .setDuration(440)
                .setInterpolator(DecelerateInterpolator(1.3f))
                .start()
        }

        override fun getItemCount() = items.size
    }
}
