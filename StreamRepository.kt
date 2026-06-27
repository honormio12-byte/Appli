package com.sitotv.iptv.repository

import com.sitotv.iptv.models.*
import com.sitotv.iptv.utils.M3UParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Repository that handles both M3U playlists and Xtream Codes API.
 */
class StreamRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // ─── M3U ──────────────────────────────────────────────────────────────────

    suspend fun loadM3UFromUrl(url: String): List<Channel> {
        return M3UParser.parseFromUrl(url)
    }

    // ─── Xtream Codes ─────────────────────────────────────────────────────────

    private fun xtreamBase(serverUrl: String, user: String, pass: String) =
        "${serverUrl.trimEnd('/')}/player_api.php?username=$user&password=$pass"

    suspend fun getXtreamLiveStreams(
        serverUrl: String, user: String, pass: String, categoryId: String = ""
    ): List<Channel> = withContext(Dispatchers.IO) {
        try {
            val url = xtreamBase(serverUrl, user, pass) + "&action=get_live_streams" +
                    if (categoryId.isNotEmpty()) "&category_id=$categoryId" else ""
            val json = fetch(url) ?: return@withContext emptyList()
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Channel(
                    id   = o.optString("epg_channel_id"),
                    name = o.optString("name"),
                    logo = o.optString("stream_icon"),
                    url  = "$serverUrl/live/$user/$pass/${o.optInt("stream_id")}.ts",
                    group = o.optString("category_id"),
                    epgId = o.optString("epg_channel_id"),
                    type = StreamType.LIVE
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getXtreamVod(
        serverUrl: String, user: String, pass: String, categoryId: String = ""
    ): List<Channel> = withContext(Dispatchers.IO) {
        try {
            val url = xtreamBase(serverUrl, user, pass) + "&action=get_vod_streams" +
                    if (categoryId.isNotEmpty()) "&category_id=$categoryId" else ""
            val json = fetch(url) ?: return@withContext emptyList()
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val ext = o.optString("container_extension", "mp4")
                Channel(
                    id   = o.optString("stream_id"),
                    name = o.optString("name"),
                    logo = o.optString("stream_icon"),
                    url  = "$serverUrl/movie/$user/$pass/${o.optInt("stream_id")}.$ext",
                    group = o.optString("category_id"),
                    type = StreamType.MOVIE
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getXtreamSeries(
        serverUrl: String, user: String, pass: String, categoryId: String = ""
    ): List<Channel> = withContext(Dispatchers.IO) {
        try {
            val url = xtreamBase(serverUrl, user, pass) + "&action=get_series" +
                    if (categoryId.isNotEmpty()) "&category_id=$categoryId" else ""
            val json = fetch(url) ?: return@withContext emptyList()
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Channel(
                    id   = o.optString("series_id"),
                    name = o.optString("name"),
                    logo = o.optString("cover"),
                    url  = "$serverUrl/series/$user/$pass/${o.optInt("series_id")}",
                    group = o.optString("category_id"),
                    type = StreamType.SERIES
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getXtreamCategories(
        serverUrl: String, user: String, pass: String, action: String
    ): List<Category> = withContext(Dispatchers.IO) {
        try {
            val url = xtreamBase(serverUrl, user, pass) + "&action=$action"
            val json = fetch(url) ?: return@withContext emptyList()
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Category(
                    id   = o.optString("category_id"),
                    name = o.optString("category_name")
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun validateXtream(serverUrl: String, user: String, pass: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val url = xtreamBase(serverUrl, user, pass)
                val json = fetch(url)
                if (json != null) {
                    val obj = JSONObject(json)
                    obj.has("user_info")
                } else false
            } catch (e: Exception) {
                false
            }
        }

    private fun fetch(url: String): String? {
        return try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) response.body?.string() else null
        } catch (e: Exception) {
            null
        }
    }
}
