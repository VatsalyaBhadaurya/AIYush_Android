package com.vatty.aiyush

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.cardview.widget.CardView
import kotlinx.coroutines.*

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    private val splashScope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        // Find views
        val logoContainer = findViewById<CardView>(R.id.logoContainer)
        val tagline = findViewById<TextView>(R.id.splashTagline)

        // Load animations
        val scaleAndFadeIn = AnimationUtils.loadAnimation(this, R.anim.scale_and_fade_in)
        val slideUpFadeIn = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in)

        // Apply animations with delay
        splashScope.launch {
            delay(300) // Short delay before starting animations
            logoContainer.startAnimation(scaleAndFadeIn)
            delay(500) // Delay before tagline animation
            tagline.startAnimation(slideUpFadeIn)
            
            delay(2200) // Wait for animations to complete
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        splashScope.cancel()
        super.onDestroy()
    }
} 