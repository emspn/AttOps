package com.app.attops.core.location

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

object MapLinkResolver {
    private const val TAG = "MapLinkResolver"

    /**
     * Extracts coordinates from a string that might contain a Google Maps link.
     */
    suspend fun resolve(sharedText: String): ResolvedLocation? = withContext(Dispatchers.IO) {
        try {
            // 1. Extract URL from text
            val url = extractUrl(sharedText) ?: return@withContext null
            
            // 2. Resolve short URL if necessary
            val finalUrl = if (url.contains("maps.app.goo.gl") || url.contains("goo.gl/maps")) {
                getFinalUrl(url)
            } else {
                url
            }

            // 3. Parse coordinates from final URL
            return@withContext parseCoordinates(finalUrl, sharedText)
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving map link", e)
            null
        }
    }

    private fun extractUrl(text: String): String? {
        val urlPattern = Pattern.compile(
            "(https?://(?:[\\w\\-]+\\.)+[\\w\\-]+(?:/[\\w\\-./?%&=]*)?)",
            Pattern.CASE_INSENSITIVE,
        )
        val matcher = urlPattern.matcher(text)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun getFinalUrl(shortUrl: String): String {
        return try {
            val url = URL(shortUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connect()
            val expandedUrl = connection.getHeaderField("Location")
            connection.disconnect()
            expandedUrl ?: shortUrl
        } catch (e: Exception) {
            shortUrl
        }
    }

    private fun parseCoordinates(url: String, originalText: String): ResolvedLocation? {
        // Pattern 1: @lat,lng
        val atPattern = Pattern.compile("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)")
        val atMatcher = atPattern.matcher(url)
        if (atMatcher.find()) {
            val lat = atMatcher.group(1)?.toDoubleOrNull()
            val lng = atMatcher.group(2)?.toDoubleOrNull()
            if ((lat != null) && (lng != null)) {
                return ResolvedLocation(lat, lng, extractLabel(originalText))
            }
        }

        // Pattern 2: q=lat,lng
        val qPattern = Pattern.compile("q=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)")
        val qMatcher = qPattern.matcher(url)
        if (qMatcher.find()) {
            val lat = qMatcher.group(1)?.toDoubleOrNull()
            val lng = qMatcher.group(2)?.toDoubleOrNull()
            if ((lat != null) && (lng != null)) {
                return ResolvedLocation(lat, lng, extractLabel(originalText))
            }
        }

        // Pattern 3: ll=lat,lng
        val llPattern = Pattern.compile("ll=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)")
        val llMatcher = llPattern.matcher(url)
        if (llMatcher.find()) {
            val lat = llMatcher.group(1)?.toDoubleOrNull()
            val lng = llMatcher.group(2)?.toDoubleOrNull()
            if ((lat != null) && (lng != null)) {
                return ResolvedLocation(lat, lng, extractLabel(originalText))
            }
        }

        return null
    }

    private fun extractLabel(text: String): String? {
        val lines = text.split("\n")
        val label = lines.firstOrNull()?.trim()
        return if (label != null && !label.startsWith("http") && label.length < 100) {
            label
        } else {
            null
        }
    }
}

data class ResolvedLocation(
    val latitude: Double,
    val longitude: Double,
    val label: String? = null
)
