package com.practice.nanohttpd

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var autoProjects: AutoCompleteTextView
    private lateinit var btnOpen: MaterialButton
    private lateinit var btnExplore: MaterialButton
    private lateinit var titleText: TextView
    private lateinit var etUrl: TextInputEditText
    private lateinit var btnDownload: MaterialButton
    private lateinit var progressDownload: LinearProgressIndicator
    private lateinit var txtProgress: TextView
    private var projectNames: List<String> = emptyList()
    private lateinit var websiteDownloader: WebsiteDownloader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        autoProjects = findViewById(R.id.autoProjects)
        btnOpen = findViewById(R.id.btnOpen)
        btnExplore = findViewById(R.id.btnExplore)
        etUrl = findViewById(R.id.etUrl)
        btnDownload = findViewById(R.id.btnDownload)
        progressDownload = findViewById(R.id.progressDownload)
        txtProgress = findViewById(R.id.txtProgress)

        titleText = findViewById(R.id.txtTitle)

        val destRoot = File(filesDir, "projects")
        copyAssetFolder("projects", destRoot)
        websiteDownloader = WebsiteDownloader(destRoot)

        refreshProjectsList()

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, projectNames)
        autoProjects.setAdapter(adapter)
        autoProjects.setOnClickListener { autoProjects.showDropDown() }

        btnOpen.setOnClickListener {
            val selected = autoProjects.text.toString()
            if (selected.isBlank()) return@setOnClickListener
            val intent = Intent(this, ProjectActivity::class.java).apply {
                putExtra("project", selected)
            }
            startActivity(intent)
        }

        btnExplore.setOnClickListener {
            val selected = autoProjects.text.toString()
            if (selected.isBlank()) return@setOnClickListener
            val intent = Intent(this, ProjectFilesActivity::class.java).apply {
                putExtra("project", selected)
            }
            startActivity(intent)
        }

        btnDownload.setOnClickListener {
            val url = etUrl.text.toString().trim()
            if (url.isBlank()) {
                Toast.makeText(this, getString(R.string.enter_valid_url), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                Toast.makeText(this, getString(R.string.url_must_start_with_http), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            startDownload(url)
        }
    }

    private fun refreshProjectsList() {
        val destRoot = File(filesDir, "projects")
        projectNames = destRoot.listFiles()?.filter { it.isDirectory }?.map { it.name }?.sorted() ?: emptyList()
        titleText.text = getString(R.string.title_projects, projectNames.size)
    }

    private fun startDownload(url: String) {
        progressDownload.visibility = View.VISIBLE
        txtProgress.visibility = View.VISIBLE
        btnDownload.isEnabled = false
        btnDownload.text = getString(R.string.downloading)

        lifecycleScope.launch {
            websiteDownloader.downloadWebsite(url, object : WebsiteDownloader.ProgressCallback {
                override fun onProgress(message: String, progress: Int, total: Int) {
                    runOnUiThread {
                        progressDownload.progress = progress
                        txtProgress.text = message
                    }
                }

                override fun onComplete(projectName: String) {
                    runOnUiThread {
                        progressDownload.visibility = View.GONE
                        txtProgress.visibility = View.GONE
                        btnDownload.isEnabled = true
                        btnDownload.text = getString(R.string.start_download)
                        etUrl.text?.clear()

                        refreshProjectsList()
                        val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, projectNames)
                        autoProjects.setAdapter(adapter)

                        Toast.makeText(this@MainActivity, 
                            getString(R.string.download_completed, projectName), 
                            Toast.LENGTH_LONG).show()
                    }
                }

                override fun onError(error: String) {
                    runOnUiThread {
                        progressDownload.visibility = View.GONE
                        txtProgress.visibility = View.GONE
                        btnDownload.isEnabled = true
                        btnDownload.text = getString(R.string.start_download)

                        Toast.makeText(this@MainActivity, 
                            getString(R.string.download_error, error), 
                            Toast.LENGTH_LONG).show()
                    }
                }
            })
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
                assetManager.open(childAssetPath).use { input ->
                    FileOutputStream(childDest).use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                copyAssetFolder(childAssetPath, childDest)
            }
        }
    }
}
