package com.example.lovandroidwrapper.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest

object WebScraper {

    fun getCacheFile(context: Context, url: String): File {
        val hash = md5(url)
        val dir = File(context.filesDir, "offline_cache")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, hash)
    }

    suspend fun isCached(context: Context, url: String): Boolean = withContext(Dispatchers.IO) {
        getCacheFile(context, url).exists()
    }

    suspend fun scrapeAndCache(context: Context, url: String, progressCallback: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        try {
            progressCallback("Downloading main page...")
            val html = downloadText(url) ?: return@withContext false
            saveToFile(context, url, html.toByteArray())

            progressCallback("Parsing resources...")
            val baseUri = URI(url)
            val resources = mutableListOf<String>()

            // Extract script src
            val scriptRegex = """<script[^>]+src=["']([^"']+)["']""".toRegex()
            scriptRegex.findAll(html).forEach { match ->
                resources.add(match.groupValues[1])
            }

            // Extract link href
            val linkRegex = """<link[^>]+href=["']([^"']+)["']""".toRegex()
            linkRegex.findAll(html).forEach { match ->
                resources.add(match.groupValues[1])
            }

            // Extract img src
            val imgRegex = """<img[^>]+src=["']([^"']+)["']""".toRegex()
            imgRegex.findAll(html).forEach { match ->
                resources.add(match.groupValues[1])
            }

            val total = resources.size
            resources.distinct().forEachIndexed { index, relPath ->
                try {
                    val resolvedUrl = baseUri.resolve(relPath).toString()
                    progressCallback("Caching resource ${index + 1}/$total: ${relPath.takeLast(20)}")
                    val bytes = downloadBytes(resolvedUrl)
                    if (bytes != null) {
                        saveToFile(context, resolvedUrl, bytes)
                    }
                } catch (e: Exception) {
                    // Ignore individual resource failures
                }
            }

            progressCallback("Offline archive complete!")
            true
        } catch (e: Exception) {
            progressCallback("Archiving failed: ${e.localizedMessage}")
            false
        }
    }

    private fun saveToFile(context: Context, url: String, data: ByteArray) {
        val file = getCacheFile(context, url)
        FileOutputStream(file).use { it.write(data) }
    }

    private fun downloadText(urlStr: String): String? {
        return try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }

    private fun downloadBytes(urlStr: String): ByteArray? {
        return try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.inputStream.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
