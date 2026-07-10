package com.app.attops.core.common.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    /**
     * Compresses the image at [originalPath] and returns the path to the compressed image.
     * Reduces quality to 70% and scales down if necessary.
     */
    fun compressImage(context: Context, originalPath: String): String {
        val originalFile = File(originalPath)
        if (!originalFile.exists()) return originalPath
        
        // If file is already small (< 200KB), don't compress
        if (originalFile.length() < 200 * 1024) return originalPath

        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(originalPath, options)
            
            // Limit max dimension to 1280px for production balance (quality vs cost)
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
            }
            val bitmap = BitmapFactory.decodeFile(originalPath, decodeOptions)
            
            val compressedFile = File(context.cacheDir, "COMP_" + originalFile.name)
            
            val out = FileOutputStream(compressedFile)
            // 75% quality is the production sweet spot
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)
            out.flush()
            out.close()
            
            return compressedFile.absolutePath
        } catch (e: Exception) {
            return originalPath
        }
    }
}
