package com.practice.nanohttpd

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var spinnerProjects: Spinner
    private lateinit var btnOpen: Button
    private lateinit var titleText: TextView
    private var projectNames: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        spinnerProjects = findViewById(R.id.spinnerProjects)
        btnOpen = findViewById(R.id.btnOpen)
        titleText = findViewById(R.id.txtTitle)

        val destRoot = File(filesDir, "projects")
        copyAssetFolder("projects", destRoot)

        projectNames = destRoot.listFiles()?.filter { it.isDirectory }?.map { it.name }?.sorted() ?: emptyList()
        titleText.text = "Projects (${projectNames.size})"

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, projectNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerProjects.adapter = adapter

        btnOpen.setOnClickListener {
            val selected = spinnerProjects.selectedItem as? String ?: return@setOnClickListener
            val intent = Intent(this, ProjectActivity::class.java).apply {
                putExtra("project", selected)
            }
            startActivity(intent)
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
