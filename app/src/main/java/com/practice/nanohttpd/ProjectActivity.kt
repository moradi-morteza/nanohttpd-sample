package com.practice.nanohttpd

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class ProjectActivity : AppCompatActivity() {

    private var server: LocalHttpServer? = null
    private val port = 8080

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project)

        webView = findViewById(R.id.webView)

        server = LocalHttpServer(filesDir, port).apply { start() }

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {}

        val project = intent.getStringExtra("project") ?: return
        loadProject(project)
    }

    private fun loadProject(name: String) {
        webView.loadUrl("http://127.0.0.1:$port/projects/$name/index.html")
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
}
