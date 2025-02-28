package com.vatty.aiyush

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.webkit.*
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import java.io.File

class MainActivity : ComponentActivity() {
    companion object {
        init {
            WebView.setDataDirectorySuffix("webview_data")
        }
    }

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var rootLayout: ConstraintLayout
    private val mainScope = MainScope()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        
        if (savedInstanceState == null) {
            WebView.enableSlowWholeDocumentDraw()
        }
        
        // Initialize cache directories
        initializeCacheDirs()
        
        // Handle back press
        onBackPressedDispatcher.addCallback(this) {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                finish()
            }
        }
        
        setupLayout()
        setupWebView()
        loadWebsite()

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }
    }

    private fun initializeCacheDirs() {
        // Create necessary cache directories
        val webViewCacheDir = File(cacheDir, "WebView")
        if (!webViewCacheDir.exists()) {
            webViewCacheDir.mkdirs()
        }
        
        val defaultCacheDir = File(webViewCacheDir, "Default")
        if (!defaultCacheDir.exists()) {
            defaultCacheDir.mkdirs()
        }
        
        val httpCacheDir = File(defaultCacheDir, "HTTP Cache")
        if (!httpCacheDir.exists()) {
            httpCacheDir.mkdirs()
        }
        
        val codeCacheDir = File(httpCacheDir, "Code Cache")
        if (!codeCacheDir.exists()) {
            codeCacheDir.mkdirs()
        }
        
        // Create specific cache directories
        listOf("js", "wasm").forEach { type ->
            File(codeCacheDir, type).mkdirs()
        }

        // Create variations directory
        File(defaultCacheDir, "variations").mkdirs()
    }

    private fun setupLayout() {
        // Create root layout
        rootLayout = ConstraintLayout(this).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            fitsSystemWindows = true
        }

        // Create and setup WebView
        webView = WebView(this).apply {
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            ).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            }
            
            // Optimize WebView settings
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            settings.apply {
                // Enable JavaScript and DOM Storage
                javaScriptEnabled = true
                domStorageEnabled = true
                
                // Configure viewport
                loadWithOverviewMode = true
                useWideViewPort = true
                
                // Configure zoom
                builtInZoomControls = true
                displayZoomControls = false
                
                // Cache and storage configuration
                cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                
                // Media and content settings
                loadsImagesAutomatically = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                
                // Performance optimizations
                setRenderPriority(WebSettings.RenderPriority.HIGH)
                setEnableSmoothTransition(true)
                
                // Security settings
                setGeolocationEnabled(false)
                javaScriptCanOpenWindowsAutomatically = false

                // Additional UI settings
                setSupportZoom(true)
                textZoom = 100
            }

            // Improve scrolling
            overScrollMode = View.OVER_SCROLL_NEVER
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        }

        // Create and setup ProgressBar
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                8
            ).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            }
            max = 100
            progress = 0
            progressDrawable?.setColorFilter(
                getColor(android.R.color.holo_blue_bright),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        }

        // Add views to root layout
        rootLayout.addView(webView)
        rootLayout.addView(progressBar)
        setContentView(rootLayout)
    }

    private fun setupWebView() {
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                error?.description?.toString()?.let { showError("Error: $it") }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                request?.url?.toString()?.let { url ->
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        return false
                    }
                }
                return true
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                // Enable local caching for resources
                return super.shouldInterceptRequest(view, request)
            }
        }
    }

    private fun loadWebsite() {
        try {
            webView.loadUrl("https://hack-gamma-two.vercel.app/")
        } catch (e: Exception) {
            showError("Failed to load website: ${e.message}")
        }
    }

    private fun showError(message: String) {
        Snackbar.make(rootLayout, message, Snackbar.LENGTH_LONG)
            .setAction("Retry") { loadWebsite() }
            .show()
    }

    override fun onDestroy() {
        mainScope.cancel()
        webView.apply {
            stopLoading()
            clearHistory()
            clearFormData()
            clearCache(true)
            destroy()
        }
        super.onDestroy()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onResume() {
        webView.onResume()
        super.onResume()
    }
}