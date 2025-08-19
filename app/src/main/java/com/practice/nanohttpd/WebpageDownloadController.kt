package com.practice.nanohttpd

import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.net.URI

/**
 * Controller class that integrates the WebpageDownloader with the UI interface
 */
class WebpageDownloadController(private val rootDir: File) {
    
    companion object {
        private const val TAG = "WebpageDownloadController"
    }
    
    enum class DuplicateAction {
        SKIP, OVERRIDE, CREATE_NEW
    }

    interface ProgressCallback {
        fun onProgress(message: String, progress: Int, total: Int)
        fun onComplete(projectName: String)
        fun onError(error: String)
        fun onDuplicateFound(projectName: String, existingPath: String): DuplicateAction
    }

    suspend fun downloadWebsite(url: String, callback: ProgressCallback) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting download for URL: $url")
            
            // Validate and fix URL format
            val normalizedUrl = normalizeUrl(url)
            Log.d(TAG, "Normalized URL: $normalizedUrl")
            
            val baseUri = URI(normalizedUrl)
            var projectName = baseUri.host?.replace(".", "_") ?: "website_${System.currentTimeMillis()}"
            var projectDir = File(rootDir, projectName)
            
            Log.d(TAG, "Initial project name: $projectName, Directory: ${projectDir.absolutePath}")
            
            // Check for duplicates
            if (projectDir.exists()) {
                Log.d(TAG, "Project already exists: $projectName")
                val action = callback.onDuplicateFound(projectName, projectDir.absolutePath)
                
                when (action) {
                    DuplicateAction.SKIP -> {
                        Log.d(TAG, "User chose to skip duplicate: $projectName")
                        callback.onComplete(projectName)
                        return@withContext
                    }
                    DuplicateAction.OVERRIDE -> {
                        Log.d(TAG, "User chose to override: $projectName")
                        if (projectDir.exists()) {
                            deleteRecursively(projectDir)
                            Log.d(TAG, "Deleted existing project directory")
                        }
                    }
                    DuplicateAction.CREATE_NEW -> {
                        val timestamp = System.currentTimeMillis()
                        projectName = "${projectName}_$timestamp"
                        projectDir = File(rootDir, projectName)
                        Log.d(TAG, "Creating new project with timestamp: $projectName")
                    }
                }
            }
            
            if (!projectDir.exists()) {
                val created = projectDir.mkdirs()
                Log.d(TAG, "Directory created: $created - ${projectDir.absolutePath}")
            }

            callback.onProgress("Initializing downloader...", 5, 100)
            
            // Create the webpage downloader and file saver
            val webpageDownloader = WebpageDownloader()
            val fileSaver = ProgressTrackingFileSaver(projectDir, callback)
            
            callback.onProgress("Downloading webpage content...", 10, 100)
            Log.d(TAG, "Starting download with WebpageDownloader")
            
            // Download the webpage
            webpageDownloader.download(normalizedUrl, fileSaver)
            
            // Count downloaded file types
            callback.onProgress("Finalizing download...", 95, 100)
            val projectFiles = projectDir.listFiles() ?: emptyArray()
            val cssCount = projectFiles.count { it.name.endsWith(".css", true) }
            val jsCount = projectFiles.count { it.name.endsWith(".js", true) }
            val imageCount = projectFiles.count { file ->
                val ext = file.name.substringAfterLast('.', "").lowercase()
                ext in listOf("png", "jpg", "jpeg", "gif", "webp", "svg", "ico")
            }
            val fontCount = projectFiles.count { file ->
                val ext = file.name.substringAfterLast('.', "").lowercase()
                ext in listOf("woff", "woff2", "ttf", "otf")
            }
            
            val summary = "Downloaded: $cssCount CSS, $jsCount JS, $imageCount images, $fontCount fonts"
            Log.d(TAG, "Download summary: $summary")
            callback.onProgress(summary, 100, 100)
            
            Log.d(TAG, "Download completed successfully for project: $projectName")
            callback.onComplete(projectName)
            
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            val errorMessage = when (e) {
                is java.net.UnknownHostException -> "Cannot resolve website address. Check your internet connection."
                is java.net.ConnectException -> "Cannot connect to website. Website may be down."
                is java.net.SocketTimeoutException -> "Connection timeout. Website is taking too long to respond."
                is javax.net.ssl.SSLException -> "SSL/Security error. Website may have certificate issues."
                is IllegalArgumentException -> "Invalid URL format: ${e.message}"
                else -> "Download failed: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
            }
            callback.onError(errorMessage)
        }
    }
    
    private fun normalizeUrl(url: String): String {
        var normalized = url.trim()
        
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://$normalized"
            Log.d(TAG, "Added HTTPS protocol to URL: $normalized")
        }
        
        if (normalized.endsWith("/")) {
            normalized = normalized.dropLast(1)
        }
        
        return normalized
    }
    
    private fun deleteRecursively(file: File): Boolean {
        return try {
            if (file.isDirectory) {
                file.listFiles()?.forEach { child ->
                    deleteRecursively(child)
                }
            }
            val deleted = file.delete()
            Log.d(TAG, "Deleted ${if (file.isDirectory) "directory" else "file"}: ${file.name} - $deleted")
            deleted
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file: ${file.absolutePath}", e)
            false
        }
    }
    
    /**
     * A FileSaver implementation that tracks download progress and provides progress updates
     */
    private class ProgressTrackingFileSaver(
        private val outputDir: File,
        private val callback: ProgressCallback
    ) : WebpageDownloader.FileSaver {
        
        private var downloadedFiles = 0
        private var totalFiles = 0
        
        init {
            outputDir.mkdirs()
        }

        override fun save(filename: String, content: String) {
            try {
                val file = File(outputDir, filename)
                file.writeText(content)
                reportProgress(filename)
                Log.d(TAG, "Saved text file: $filename (${content.length} characters)")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving text file: $filename", e)
                throw e
            }
        }

        override fun save(filename: String, content: java.io.InputStream) {
            try {
                val file = File(outputDir, filename)
                file.outputStream().use { content.copyTo(it) }
                reportProgress(filename)
                Log.d(TAG, "Saved binary file: $filename")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving binary file: $filename", e)
                throw e
            }
        }
        
        private fun reportProgress(filename: String) {
            downloadedFiles++
            // Estimate total files (this is rough estimation since we don't know beforehand)
            if (totalFiles == 0) totalFiles = 50 // Initial estimate
            if (downloadedFiles > totalFiles) totalFiles = downloadedFiles + 10
            
            val progress = 10 + ((downloadedFiles * 80) / totalFiles).coerceAtMost(80)
            val fileType = getFileType(filename)
            callback.onProgress("Downloaded $fileType: $filename", progress, 100)
        }
        
        private fun getFileType(fileName: String): String {
            return when (fileName.substringAfterLast('.', "").lowercase()) {
                "html", "htm" -> "Page"
                "css" -> "CSS"
                "js" -> "JavaScript" 
                "png", "jpg", "jpeg", "gif", "webp", "svg" -> "Image"
                "woff", "woff2", "ttf", "otf" -> "Font"
                "ico" -> "Icon"
                else -> "File"
            }
        }
    }
}