package com.example.lovandroidwrapper.plugins

import android.content.Context
import android.webkit.JavascriptInterface
import com.example.lovandroidwrapper.data.AppDao
import com.example.lovandroidwrapper.data.TransistorApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest

class MetadataPlugin(
    private val context: Context,
    private val dao: AppDao,
    private val scope: CoroutineScope,
    private val currentUrl: String
) {
    @JavascriptInterface
    fun onMetadataExtracted(title: String, iconUrl: String?) {
        android.util.Log.d("MetadataPlugin", "onMetadataExtracted: title='$title', iconUrl='$iconUrl'")
        scope.launch(Dispatchers.IO) {
            try {
                val existingApp = dao.getAppByUrl(currentUrl)
                var localIconPath: String? = existingApp?.iconUrl

                if (!iconUrl.isNullOrEmpty()) {
                    android.util.Log.d("MetadataPlugin", "Downloading icon from: $iconUrl")
                    val iconFile = downloadIcon(context, iconUrl)
                    if (iconFile != null) {
                        localIconPath = iconFile.absolutePath
                        android.util.Log.d("MetadataPlugin", "Downloaded icon path: $localIconPath")
                    } else {
                        android.util.Log.e("MetadataPlugin", "Icon download failed (returned null file)")
                    }
                }

                val finalTitle = if (title.isNotBlank()) title else existingApp?.title ?: currentUrl
                val updatedApp = existingApp?.copy(
                    title = finalTitle,
                    iconUrl = localIconPath
                ) ?: TransistorApp(
                    url = currentUrl,
                    title = finalTitle,
                    iconUrl = localIconPath
                )
                dao.insertApp(updatedApp)
                android.util.Log.d("MetadataPlugin", "App metadata saved in database successfully: $updatedApp")
            } catch (e: Exception) {
                android.util.Log.e("MetadataPlugin", "Exception in onMetadataExtracted", e)
            }
        }
    }

    private fun downloadIcon(context: Context, iconUrl: String): File? {
        return try {
            val url = URL(iconUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val bytes = connection.getInputStream().use { it.readBytes() }

            val hash = md5(iconUrl)
            val ext = if (iconUrl.contains(".svg", ignoreCase = true)) "svg" else "png"
            val dir = File(context.filesDir, "favicons")
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, "$hash.$ext")
            if (ext == "svg") {
                val svgStr = String(bytes, Charsets.UTF_8)
                val sanitizedSvg = sanitizeSvg(svgStr)
                FileOutputStream(file).use { it.write(sanitizedSvg.toByteArray(Charsets.UTF_8)) }
            } else {
                FileOutputStream(file).use { it.write(bytes) }
            }
            file
        } catch (e: Exception) {
            android.util.Log.e("MetadataPlugin", "Error downloading icon from $iconUrl", e)
            null
        }
    }

    private fun sanitizeSvg(svgContent: String): String {
        // 1. Strip CSS Color Level 4 display-p3 overrides
        var sanitized = svgContent.replace("""fill:color\([^)]+\);?""".toRegex(), "")

        // 2. Convert SVG 2.0 Alpha Mask to standard Luminance Mask
        val maskRegex = """<mask[^>]*>([\s\S]*?)</mask>""".toRegex()
        sanitized = maskRegex.replace(sanitized) { matchResult ->
            val maskInner = matchResult.groupValues[1]
            val fixedInner = maskInner
                .replace("""fill="#000"""", """fill="#fff"""")
                .replace("""fill:#000""", """fill:#fff""")
                .replace("""stroke="#000"""", """stroke="#fff""")
                .replace("""stroke:#000""", """stroke:#fff""")
            
            var maskTag = matchResult.value.substringBefore(">")
            maskTag = maskTag.replace("""mask-type="alpha"""", "")
            maskTag = maskTag.replace("""mask-type:alpha;?""".toRegex(), "")
            maskTag = maskTag.replace("""mask-type="luminance"""", "")
            maskTag = maskTag.replace("""mask-type:luminance;?""".toRegex(), "")
            maskTag = maskTag.replace("""style="" """, "")
            maskTag = maskTag.replace("""style="\s*"""".toRegex(), "")
            
            "$maskTag>$fixedInner</mask>"
        }
        return sanitized
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
