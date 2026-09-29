package com.mediasandesh.news

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class SplashActivity : ComponentActivity() {

    private var finished = false
    private lateinit var effects: SplashAnimationView

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withAppLocale())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setOnExitAnimationListener { it.remove() }
        super.onCreate(savedInstanceState)

        val metrics = resources.displayMetrics
        val root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(250, 250, 250)) }

        effects = SplashAnimationView(this)
        root.addView(effects, FrameLayout.LayoutParams(-1, -1))

        val logoSize = (minOf(metrics.widthPixels, metrics.heightPixels) * 0.70f).toInt()
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.media_sandesh_logo)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            contentDescription = getString(R.string.app_name)
            alpha = 0f
            scaleX = 0.72f
            scaleY = 0.72f
        }
        root.addView(logo, FrameLayout.LayoutParams(logoSize, logoSize).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            topMargin = (metrics.heightPixels * 0.39f - logoSize / 2f).toInt()
        })

        val tagline = TextView(this).apply {
            text = getString(R.string.splash_tagline)
            textSize = 25f
            setTextColor(Color.rgb(145, 12, 18))
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            alpha = 0f
        }
        root.addView(tagline, FrameLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            topMargin = (metrics.heightPixels * 0.39f + logoSize * 0.62f).toInt()
        })

        setContentView(root)

        logo.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(720)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
        tagline.animate()
            .alpha(1f).translationY(-12f)
            .setStartDelay(500).setDuration(450)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        val start = SystemClock.uptimeMillis()
        val duration = 2400L
        val updater = object : Runnable {
            override fun run() {
                if (finished) return
                val p = ((SystemClock.uptimeMillis() - start).toFloat() / duration).coerceIn(0f, 1f)
                effects.setProgress(p)
                if (p < 1f) effects.postOnAnimation(this) else openApp()
            }
        }
        effects.postOnAnimation(updater)
    }

    private fun openApp() {
        if (finished) return
        finished = true
        startActivity(Intent(this, MainActivity::class.java))
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onDestroy() {
        finished = true
        super.onDestroy()
    }
}
