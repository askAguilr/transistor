package com.example.lovandroidwrapper.plugins

import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast
import android.os.Handler
import android.os.Looper

/**
 * Custom Native Plugin to show Native Toasts.
 */
class ToastPlugin(private val context: Context) {

    @JavascriptInterface
    fun showToast(message: String) {
        // Run on the UI main thread
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
