package com.todopro.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val iconView = findViewById<ImageView>(R.id.splashIcon)
        val logoText = findViewById<TextView>(R.id.logoText)
        val subText = findViewById<TextView>(R.id.subText)

        // Icon: Scale + Bounce Animation
        val scaleAnim = AnimationUtils.loadAnimation(this, R.anim.scale_up)
        iconView.startAnimation(scaleAnim)

        // Logo Text: Slide up + Fade in
        val slideAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        logoText.startAnimation(slideAnim)

        // Sub Text: Slide up mit Verzögerung
        val slideAnim2 = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        slideAnim2.startOffset = 500
        subText.startAnimation(slideAnim2)

        // Nach 2 Sekunden: Onboarding (erster Start) oder direkt zur MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            if (TodoStorage.isFirstLaunch(this)) {
                // Erster Start → Onboarding zeigen
                startActivity(Intent(this, OnboardingActivity::class.java))
            } else {
                // Bereits bekannt → direkt zur MainActivity
                startActivity(Intent(this, MainActivity::class.java))
            }
            overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2000)
    }
}
