package com.app.attops.core.common.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ImageUtils {
    /**
     * Compresses and watermarks the image.
     */
    fun processAndCompressImage(
        context: Context, 
        originalPath: String,
        latitude: Double? = null,
        longitude: Double? = null,
        label: String? = null
    ): String {
        val originalFile = File(originalPath)
        if (!originalFile.exists()) return originalPath
        
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(originalPath, options)
            
            val maxDimension = 1280
            var sampleSize = 1
            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / sampleSize >= maxDimension && halfWidth / sampleSize >= maxDimension) {
                    sampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inMutable = true
            }
            val originalBitmap = BitmapFactory.decodeFile(originalPath, decodeOptions) ?: return originalPath
            
            // Apply Watermark
            val watermarkedBitmap = applyWatermark(originalBitmap, latitude, longitude, label)
            
            val compressedFile = File(context.cacheDir, "ATT_" + originalFile.name)
            val out = FileOutputStream(compressedFile)
            watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            out.flush()
            out.close()
            
            return compressedFile.absolutePath
        } catch (e: Exception) {
            return originalPath
        }
    }

    private fun applyWatermark(
        bitmap: Bitmap, 
        lat: Double?, 
        lng: Double?,
        label: String?
    ): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        
        // Semi-transparent background for high contrast visibility
        val rectPaint = Paint().apply {
            color = Color.BLACK
            alpha = 140
        }

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = bitmap.height / 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm:ss a"))
        val location = if (lat != null && lng != null) "GPS: $lat, $lng" else "Location: Site Unlinked"
        val labelStr = label?.uppercase() ?: "PROOF"

        val margin = 30f
        val lineSpacing = 15f
        
        // Calculate text block height
        val textHeight = (textPaint.textSize * 3) + (lineSpacing * 2)
        val rectTop = bitmap.height - textHeight - (margin * 2)
        
        // Draw background rectangle at the bottom
        canvas.drawRect(0f, rectTop, bitmap.width.toFloat(), bitmap.height.toFloat(), rectPaint)
        
        var currentY = rectTop + margin + textPaint.textSize
        
        // Line 1: Status Label
        textPaint.color = Color.YELLOW
        canvas.drawText(labelStr, margin, currentY, textPaint)
        currentY += textPaint.textSize + lineSpacing
        
        // Line 2: Timestamp
        textPaint.color = Color.WHITE
        canvas.drawText(timestamp, margin, currentY, textPaint)
        currentY += textPaint.textSize + lineSpacing
        
        // Line 3: GPS
        canvas.drawText(location, margin, currentY, textPaint)

        return result
    }

    // Keep compatibility for old calls if any
    @Deprecated("Use processAndCompressImage")
    fun compressImage(context: Context, originalPath: String): String {
        return processAndCompressImage(context, originalPath)
    }
}
