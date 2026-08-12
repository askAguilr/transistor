package com.example.lovandroidwrapper.ui.webview

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.lovandroidwrapper.utils.WebScraper
import java.io.FileInputStream

class TransistorWebViewClient(
    private val context: Context,
    private val offlineMode: Boolean,
    private val onPageFinishedCallback: () -> Unit,
    private val onThemeColorExtracted: (Int?) -> Unit
) : WebViewClient() {

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        val url = request?.url?.toString() ?: return null

        if (offlineMode) {
            val cacheFile = WebScraper.getCacheFile(context, url)
            if (cacheFile.exists()) {
                try {
                    val mimeType = getMimeType(url)
                    val inputStream = FileInputStream(cacheFile)
                    return WebResourceResponse(mimeType, "UTF-8", inputStream)
                } catch (e: Exception) {
                    // Fallback to network if reading cache fails
                }
            }
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageFinishedCallback()

        // Extract theme color
        view?.evaluateJavascript(
            "(function() { " +
                    "  var meta = document.querySelector('meta[name=\"theme-color\"]');" +
                    "  return meta ? meta.getAttribute('content') : null;" +
                    "})()"
        ) { result ->
            if (result != null && result != "null" && result.startsWith("\"") && result.endsWith("\"")) {
                try {
                    val colorHex = result.replace("\"", "")
                    val color = Color.parseColor(colorHex)
                    onThemeColorExtracted(color)
                } catch (e: Exception) {
                    // Ignore parsing failures
                }
            }
        }

        // Extract metadata: title and favicon
        view?.evaluateJavascript(
            """
            (async function() {
              try {
                let title = document.title;
                let iconUrl = null;

                // 1. Try to find manifest
                const manifestLink = document.querySelector('link[rel="manifest"]');
                if (manifestLink) {
                  try {
                    const response = await fetch(manifestLink.href);
                    const manifest = await response.json();
                    if (manifest.short_name || manifest.name) {
                      title = manifest.short_name || manifest.name;
                    }
                    if (manifest.icons && manifest.icons.length > 0) {
                      const bestIcon = manifest.icons[0];
                      iconUrl = new URL(bestIcon.src, manifestLink.href).href;
                    }
                  } catch (e) {
                    console.error("Error fetching manifest:", e);
                  }
                }

                // 2. If no iconUrl from manifest, try link tags
                if (!iconUrl) {
                  const iconSelectors = [
                    'link[rel="apple-touch-icon"]',
                    'link[rel="icon"]',
                    'link[rel="shortcut icon"]'
                  ];
                  for (const selector of iconSelectors) {
                    const el = document.querySelector(selector);
                    if (el && el.href) {
                      iconUrl = el.href;
                      break;
                    }
                  }
                }

                // 3. Fallback to /favicon.ico
                if (!iconUrl) {
                  iconUrl = new URL('/favicon.ico', window.location.href).href;
                }

                // Pass back to Android
                if (window.TransistorMetadataBridge) {
                  window.TransistorMetadataBridge.onMetadataExtracted(title, iconUrl);
                }
              } catch (err) {
                console.error("Metadata extraction error:", err);
              }
            })()
            """.trimIndent(),
            null
        )
    }

    private fun getMimeType(urlStr: String): String {
        val uri = Uri.parse(urlStr)
        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.path)
        return if (extension != null) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "text/html"
        } else {
            "text/html"
        }
    }
}
