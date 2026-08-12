package com.example.lovandroidwrapper.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.lovandroidwrapper.MainActivity
import com.example.lovandroidwrapper.R
import com.example.lovandroidwrapper.data.TransistorApp

object ShortcutHelper {
    fun createShortcut(context: Context, app: TransistorApp) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            Toast.makeText(context, "Pinning shortcuts not supported on this launcher", Toast.LENGTH_SHORT).show()
            return
        }

        // Target intent
        val shortcutIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("URL", app.url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        // Load custom launcher icon if available
        val iconBitmap = IconLoader.loadLauncherIcon(app.iconUrl, app.themeColor, 512, 512)
        val iconCompat = if (iconBitmap != null) {
            IconCompat.createWithAdaptiveBitmap(iconBitmap)
        } else {
            IconCompat.createWithResource(context, R.mipmap.ic_launcher)
        }

        // Build shortcut info
        val shortcut = ShortcutInfoCompat.Builder(context, app.url)
            .setShortLabel(app.title)
            .setLongLabel(app.title)
            .setIcon(iconCompat)
            .setIntent(shortcutIntent)
            .build()

        // Update shortcut if it already exists to force launcher to refresh the icon
        try {
            ShortcutManagerCompat.updateShortcuts(context, listOf(shortcut))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Request pin
        val success = ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        if (success) {
            Toast.makeText(context, "Shortcut requested for ${app.title}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to request shortcut", Toast.LENGTH_SHORT).show()
        }
    }
}
