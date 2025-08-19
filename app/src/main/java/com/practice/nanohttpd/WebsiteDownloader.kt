package com.practice.nanohttpd

import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit

class WebsiteDownloader(private val rootDir: File) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    interface ProgressCallback {
        fun onProgress(message: String, progress: Int, total: Int)
        fun onComplete(projectName: String)
        fun onError(error: String)
    }

    suspend fun downloadWebsite(url: String, callback: ProgressCallback) = withContext(Dispatchers.IO) {
        try {
            val baseUri = URI(url)
            val projectName = baseUri.host?.replace(".", "_") ?: "website_${System.currentTimeMillis()}"
            val projectDir = File(rootDir, projectName)
            
            if (!projectDir.exists()) {
                projectDir.mkdirs()
            }

            callback.onProgress("Downloading main page...", 1, 100)
            
            // Download main HTML page
            val mainHtml = downloadFile(url)
            val doc = Jsoup.parse(mainHtml, url)
            
            val filesToDownload = mutableSetOf<String>()
            
            // Extract all resource URLs
            extractResourceUrls(doc, baseUri, filesToDownload)
            
            callback.onProgress("Found ${filesToDownload.size} resources to download", 10, 100)
            
            // Save main HTML file
            val indexFile = File(projectDir, "index.html")
            indexFile.writeText(doc.outerHtml())
            
            // Download all resources
            val totalFiles = filesToDownload.size
            var downloadedFiles = 0
            
            filesToDownload.forEach { resourceUrl ->
                try {
                    val fileName = getFileNameFromUrl(resourceUrl, baseUri)
                    val localFile = File(projectDir, fileName)
                    
                    // Create directory if needed
                    localFile.parentFile?.mkdirs()
                    
                    val content = downloadFile(resourceUrl)
                    localFile.writeBytes(content.toByteArray())
                    
                    downloadedFiles++
                    val progress = 10 + (downloadedFiles * 80 / totalFiles)
                    callback.onProgress("Downloaded: $fileName", progress, 100)
                    
                } catch (e: Exception) {
                    // Continue downloading other files even if one fails
                    downloadedFiles++
                }
            }
            
            // Update HTML to use local references
            updateHtmlReferences(doc, baseUri, projectDir)
            indexFile.writeText(doc.outerHtml())
            
            callback.onProgress("Download completed!", 100, 100)
            callback.onComplete(projectName)
            
        } catch (e: Exception) {
            callback.onError("Download failed: ${e.message}")
        }
    }

    private suspend fun extractResourceUrls(doc: Document, baseUri: URI, urls: MutableSet<String>) {
        // CSS files
        doc.select("link[rel=stylesheet]").forEach { link ->
            link.attr("href")?.let { href ->
                urls.add(resolveUrl(href, baseUri))
            }
        }
        
        // JavaScript files
        doc.select("script[src]").forEach { script ->
            script.attr("src")?.let { src ->
                urls.add(resolveUrl(src, baseUri))
            }
        }
        
        // Images
        doc.select("img[src]").forEach { img ->
            img.attr("src")?.let { src ->
                urls.add(resolveUrl(src, baseUri))
            }
        }
        
        // Fonts and other assets in CSS
        doc.select("link, style").forEach { element ->
            val cssContent = if (element.tagName() == "style") {
                element.html()
            } else {
                element.attr("href")?.let { href ->
                    try {
                        downloadFile(resolveUrl(href, baseUri))
                    } catch (e: Exception) {
                        ""
                    }
                } ?: ""
            }
            
            // Extract URLs from CSS content
            val urlPattern = Regex("url\\(['\"]?([^'\"\\)]+)['\"]?\\)")
            urlPattern.findAll(cssContent).forEach { match ->
                urls.add(resolveUrl(match.groupValues[1], baseUri))
            }
        }
    }

    private fun resolveUrl(url: String, baseUri: URI): String {
        return if (url.startsWith("http")) {
            url
        } else {
            baseUri.resolve(url).toString()
        }
    }

    private fun getFileNameFromUrl(url: String, baseUri: URI): String {
        val uri = URI(url)
        var path = uri.path
        
        if (path.isEmpty() || path == "/") {
            path = "/index.html"
        }
        
        // Remove leading slash and ensure proper file structure
        return path.removePrefix("/").ifEmpty { "index.html" }
    }

    private fun updateHtmlReferences(doc: Document, baseUri: URI, projectDir: File) {
        // Update CSS links
        doc.select("link[rel=stylesheet]").forEach { link ->
            val href = link.attr("href")
            if (href.isNotEmpty()) {
                val localPath = getFileNameFromUrl(resolveUrl(href, baseUri), baseUri)
                link.attr("href", localPath)
            }
        }
        
        // Update script sources
        doc.select("script[src]").forEach { script ->
            val src = script.attr("src")
            if (src.isNotEmpty()) {
                val localPath = getFileNameFromUrl(resolveUrl(src, baseUri), baseUri)
                script.attr("src", localPath)
            }
        }
        
        // Update image sources
        doc.select("img[src]").forEach { img ->
            val src = img.attr("src")
            if (src.isNotEmpty()) {
                val localPath = getFileNameFromUrl(resolveUrl(src, baseUri), baseUri)
                img.attr("src", localPath)
            }
        }
    }

    private suspend fun downloadFile(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .build()
        
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                response.body?.string() ?: ""
            } else {
                throw Exception("Failed to download $url: ${response.code}")
            }
        }
    }
}