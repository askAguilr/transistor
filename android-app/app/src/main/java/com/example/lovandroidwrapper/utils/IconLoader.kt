package com.example.lovandroidwrapper.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import com.caverock.androidsvg.SVG
import java.io.File

object IconLoader {
    fun loadIcon(filePath: String?, width: Int = 192, height: Int = 192): Bitmap? {
        if (filePath.isNullOrEmpty()) return null
        return try {
            val file = File(filePath)
            if (!file.exists()) return null
            if (filePath.endsWith(".svg", ignoreCase = true)) {
                val svg = file.inputStream().use { SVG.getFromInputStream(it) }
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.TRANSPARENT)
                svg.renderToCanvas(canvas, RectF(0f, 0f, width.toFloat(), height.toFloat()))
                bitmap
            } else if (filePath.endsWith(".ico", ignoreCase = true)) {
                val icoBitmap = decodeIco(file) ?: BitmapFactory.decodeFile(file.absolutePath)
                if (icoBitmap != null) {
                    Bitmap.createScaledBitmap(icoBitmap, width, height, true)
                } else {
                    null
                }
            } else {
                val originalBitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
                Bitmap.createScaledBitmap(originalBitmap, width, height, true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decodeIco(file: File): Bitmap? {
        return try {
            val bytes = file.readBytes()
            if (bytes.size < 6) return null
            
            // Check type (1 for icon)
            val type = ((bytes[3].toInt() and 0xFF) shl 8) or (bytes[2].toInt() and 0xFF)
            if (type != 1) return null
            
            // Get number of images
            val count = ((bytes[5].toInt() and 0xFF) shl 8) or (bytes[4].toInt() and 0xFF)
            if (count <= 0) return null
            
            var bestOffset = 0
            var bestSize = 0
            var maxWidth = 0
            
            for (i in 0 until count) {
                val entryOffset = 6 + i * 16
                if (entryOffset + 16 > bytes.size) break
                
                var w = bytes[entryOffset].toInt() and 0xFF
                var h = bytes[entryOffset + 1].toInt() and 0xFF
                if (w == 0) w = 256
                if (h == 0) h = 256
                
                // Read image size (4 bytes)
                val imgSize = ((bytes[entryOffset + 11].toInt() and 0xFF) shl 24) or
                              ((bytes[entryOffset + 10].toInt() and 0xFF) shl 16) or
                              ((bytes[entryOffset + 9].toInt() and 0xFF) shl 8) or
                              (bytes[entryOffset + 8].toInt() and 0xFF)
                
                // Read image offset (4 bytes)
                val imgOffset = ((bytes[entryOffset + 15].toInt() and 0xFF) shl 24) or
                                ((bytes[entryOffset + 14].toInt() and 0xFF) shl 16) or
                                ((bytes[entryOffset + 13].toInt() and 0xFF) shl 8) or
                                (bytes[entryOffset + 12].toInt() and 0xFF)
                
                // Pick the highest resolution image
                if (w > maxWidth && imgOffset + imgSize <= bytes.size) {
                    maxWidth = w
                    bestOffset = imgOffset
                    bestSize = imgSize
                }
            }
            
            if (bestSize > 0) {
                BitmapFactory.decodeByteArray(bytes, bestOffset, bestSize)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun loadLauncherIcon(filePath: String?, themeColorHex: String?, width: Int = 512, height: Int = 512): Bitmap? {
        val logo = loadIcon(filePath, (width * 0.80f).toInt(), (height * 0.80f).toInt()) ?: return null
        return try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            val color = try {
                if (!themeColorHex.isNullOrBlank()) {
                    android.graphics.Color.parseColor(themeColorHex)
                } else {
                    android.graphics.Color.parseColor("#863bff")
                }
            } catch (e: Exception) {
                android.graphics.Color.parseColor("#863bff")
            }
            
            // Full-bleed background color (required for adaptive launcher masking)
            canvas.drawColor(color)
            
            // Draw logo centered inside canvas
            val left = (width - logo.width) / 2f
            val top = (height - logo.height) / 2f
            canvas.drawBitmap(logo, left, top, null)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
