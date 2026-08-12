package com.example.lovandroidwrapper.plugins

import android.content.Context
import android.webkit.JavascriptInterface
import org.json.JSONArray

/**
 * Native Core Plugin exposed to Transistor Web App via WebView JavascriptInterface.
 */
class CorePlugin(
    private val context: Context,
    private val enabledPlugins: List<String>
) {

    @JavascriptInterface
    fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    @JavascriptInterface
    fun getEnabledPluginsJson(): String {
        val jsonArray = JSONArray()
        for (plugin in enabledPlugins) {
            jsonArray.put(plugin)
        }
        return jsonArray.toString()
    }
}
