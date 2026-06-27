package com.sitotv.iptv.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

// ─── Playlist (stored locally) ───────────────────────────────────────────────
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,           // M3U URL or Xtream server
    val type: String,          // "m3u_url" | "m3u_file" | "xtream"
    val username: String = "", // Xtream only
    val password: String = "", // Xtream only
    val addedAt: Long = System.currentTimeMillis(),
    val lastSync: Long = 0L
)

// ─── M3U / Channel ───────────────────────────────────────────────────────────
@Parcelize
data class Channel(
    val id: String = "",
    val name: String = "",
    val logo: String = "",
    val url: String = "",
    val group: String = "",
    val epgId: String = "",
    val type: StreamType = StreamType.LIVE
) : Parcelable

enum class StreamType { LIVE, MOVIE, SERIES }

// ─── Category ────────────────────────────────────────────────────────────────
data class Category(
    val id: String = "",
    val name: String = "",
    val count: Int = 0
)

// ─── Xtream API models ────────────────────────────────────────────────────────
data class XtreamUserInfo(
    val username: String = "",
    val password: String = "",
    val status: String = "",
    val expDate: String = "",
    val maxConnections: String = ""
)

data class XtreamCategory(
    val category_id: String = "",
    val category_name: String = "",
    val parent_id: Int = 0
)

data class XtreamStream(
    val num: Int = 0,
    val name: String = "",
    val stream_type: String = "",
    val stream_id: Int = 0,
    val stream_icon: String = "",
    val epg_channel_id: String = "",
    val added: String = "",
    val category_id: String = "",
    val direct_source: String = "",
    val custom_sid: String = ""
)

data class XtreamVod(
    val num: Int = 0,
    val name: String = "",
    val stream_type: String = "",
    val stream_id: Int = 0,
    val stream_icon: String = "",
    val rating: String = "",
    val rating_5based: Double = 0.0,
    val added: String = "",
    val category_id: String = "",
    val container_extension: String = ""
)

data class XtreamSeries(
    val num: Int = 0,
    val name: String = "",
    val series_id: Int = 0,
    val cover: String = "",
    val plot: String = "",
    val cast: String = "",
    val director: String = "",
    val genre: String = "",
    val releaseDate: String = "",
    val last_modified: String = "",
    val rating: String = "",
    val rating_5based: Double = 0.0,
    val backdrop_path: List<String> = emptyList(),
    val youtube_trailer: String = "",
    val episode_run_time: String = "",
    val category_id: String = ""
)

// ─── UI State ─────────────────────────────────────────────────────────────────
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
