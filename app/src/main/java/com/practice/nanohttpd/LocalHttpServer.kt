package com.practice.nanohttpd

import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class LocalHttpServer(
    private val root: File,
    port: Int = 8080
) : NanoHTTPD(port) {

    private val mime = mapOf(
        "html" to "text/html; charset=utf-8",
        "htm"  to "text/html; charset=utf-8",
        "js"   to "application/javascript; charset=utf-8",
        "mjs"  to "application/javascript; charset=utf-8",
        "css"  to "text/css; charset=utf-8",
        "png"  to "image/png",
        "jpg"  to "image/jpeg",
        "jpeg" to "image/jpeg",
        "gif"  to "image/gif",
        "svg"  to "image/svg+xml",
        "json" to "application/json; charset=utf-8",
        "txt"  to "text/plain; charset=utf-8",
        "ico"  to "image/x-icon"
    )

    override fun serve(session: IHTTPSession): Response {
        return try {
            val uri = URLDecoder.decode(session.uri, StandardCharsets.UTF_8.name())
                .removePrefix("/")

            val requested = if (uri.isEmpty()) {
                // default to first project index
                File(root, "projects/memory/index.html")
            } else {
                File(root, uri)
            }

            val canonicalRoot = root.canonicalFile
            val canonicalRequested = requested.canonicalFile

            if (!canonicalRequested.path.startsWith(canonicalRoot.path)) {
                return newFixedLengthResponse(Response.Status.FORBIDDEN, "text/plain", "Forbidden")
            }

            val file = if (canonicalRequested.isDirectory) {
                File(canonicalRequested, "index.html")
            } else canonicalRequested

            if (!file.exists() || !file.isFile) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found")
            }

            val contentType = mime[file.extension.lowercase()] ?: "application/octet-stream"
            newChunkedResponse(Response.Status.OK, contentType, file.inputStream())
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal error: ${e.message}")
        }
    }
}


