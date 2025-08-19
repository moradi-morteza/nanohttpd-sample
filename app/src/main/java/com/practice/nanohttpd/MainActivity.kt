package com.practice.nanohttpd

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private var server: LocalHttpServer? = null
    private val port = 8080

    private lateinit var webView: WebView
    private lateinit var btnProject1: Button
    private lateinit var btnProject2: Button
    private lateinit var btnProject3: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        btnProject1 = findViewById(R.id.btnProject1)
        btnProject2 = findViewById(R.id.btnProject2)
        btnProject3 = findViewById(R.id.btnProject3)

        // 1) Copy assets/projects/** -> filesDir/projects/**
        val destRoot = File(filesDir, "projects")
        copyAssetFolder("projects", destRoot)

        // 2) Start localhost server serving filesDir/*
        server = LocalHttpServer(filesDir, port).apply { start() }

        // 3) Configure WebView
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {}

        // 4) Hook buttons
        btnProject1.setOnClickListener {
            loadPath("/projects/memory/index.html")
        }
        btnProject2.setOnClickListener {
            loadPath("/projects/form/index.html")
        }
        btnProject3.setOnClickListener {
            loadPath("/projects/canvas/index.html")
        }

        // 5) Load default
        loadPath("/projects/memory/index.html")
    }

    private fun loadPath(path: String) {
        webView.loadUrl("http://127.0.0.1:$port$path")
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
    }

    @Deprecated("Yes, it's deprecated; yes, it's still fine here.")
    override fun onBackPressed() {
        if (this::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Recursively copy an asset folder (assets/{assetPath}) into destDir.
     */
    private fun copyAssetFolder(assetPath: String, destDir: File) {
        val assetManager = assets
        val items = assetManager.list(assetPath) ?: return

        if (!destDir.exists()) destDir.mkdirs()

        for (name in items) {
            val childAssetPath = if (assetPath.isEmpty()) name else "$assetPath/$name"
            val childDest = File(destDir, name)
            val sub = assetManager.list(childAssetPath)

            if (sub.isNullOrEmpty()) {
                // It's a file; copy
                assetManager.open(childAssetPath).use { input ->
                    FileOutputStream(childDest).use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                // It's a directory; recurse
                copyAssetFolder(childAssetPath, childDest)
            }
        }
    }
}

