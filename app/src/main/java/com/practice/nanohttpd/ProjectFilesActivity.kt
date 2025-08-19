package com.practice.nanohttpd

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class ProjectFilesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_files)

        val project = intent.getStringExtra("project") ?: return
        title = project

        val listView = findViewById<ListView>(R.id.listFiles)
        val dir = File(filesDir, "projects/$project")
        val items = dir.list()?.sorted() ?: emptyList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        listView.adapter = adapter
    }
}
