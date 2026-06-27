package com.sitotv.iptv.utils

import com.sitotv.iptv.models.Channel
import com.sitotv.iptv.models.StreamType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * M3U Parser – handles both URL-based and file-based M3U playlists.
 * Supports standard #EXTINF attributes: tvg-id, tvg-name, tvg-logo, group-title
 */
object M3UParser {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Download and parse an M3U playlist from a URL.
     */
    suspend fun parseFromUrl(url: String): List<Channel> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()
            parseM3UContent(body)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Parse an M3U playlist from an InputStream (local file).
     */
    suspend fun parseFromStream(inputStream: InputStream): List<Channel> = withContext(Dispatchers.IO) {
        try {
            val content = inputStream.bufferedReader(Charsets.UTF_8).readText()
            parseM3UContent(content)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Core parser: converts raw M3U text into a list of Channel objects.
     */
    private fun parseM3UContent(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()

        if (lines.isEmpty() || !lines[0].trim().startsWith("#EXTM3U")) {
            return emptyList()
        }

        var i = 1
        var idCounter = 0

        while (i < lines.size) {
            val line = lines[i].trim()

            if (line.startsWith("#EXTINF:")) {
                val info = line.removePrefix("#EXTINF:")
                val url = findNextUrl(lines, i + 1)

                if (url != null) {
                    val channel = parseExtInf(info, url, idCounter++)
                    channels.add(channel)
                    i = lines.indexOfFirst { it.trim() == url } + 1
                    continue
                }
            }
            i++
        }

        return channels
    }

    private fun findNextUrl(lines: List<String>, startIndex: Int): String? {
        for (j in startIndex until minOf(startIndex + 5, lines.size)) {
            val l = lines[j].trim()
            if (l.isNotEmpty() && !l.startsWith("#")) {
                return l
            }
        }
        return null
    }

    private fun parseExtInf(info: String, url: String, id: Int): Channel {
        // Split at comma to separate attributes from name
        val commaIdx = info.indexOf(',')
        val attrPart = if (commaIdx >= 0) info.substring(0, commaIdx) else info
        val namePart = if (commaIdx >= 0) info.substring(commaIdx + 1).trim() else "Channel $id"

        val tvgId    = extractAttr(attrPart, "tvg-id")
        val tvgName  = extractAttr(attrPart, "tvg-name").ifEmpty { namePart }
        val tvgLogo  = extractAttr(attrPart, "tvg-logo")
        val group    = extractAttr(attrPart, "group-title")

        val type = when {
            url.contains("/movie/", ignoreCase = true) -> StreamType.MOVIE
            url.contains("/series/", ignoreCase = true) -> StreamType.SERIES
            group.contains("movie", ignoreCase = true) -> StreamType.MOVIE
            group.contains("series", ignoreCase = true) || group.contains("show", ignoreCase = true) -> StreamType.SERIES
            else -> StreamType.LIVE
        }

        return Channel(
            id = tvgId.ifEmpty { id.toString() },
            name = tvgName,
            logo = tvgLogo,
            url = url,
            group = group,
            epgId = tvgId,
            type = type
        )
    }

    private fun extractAttr(attrs: String, key: String): String {
        val pattern = Regex("""$key="([^"]*)"""")
        return pattern.find(attrs)?.groupValues?.get(1) ?: ""
    }
}
