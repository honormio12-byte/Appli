package com.sitotv.iptv.database

import android.content.Context
import androidx.room.*
import com.sitotv.iptv.models.PlaylistEntity
import kotlinx.coroutines.flow.Flow

// ─── DAO ─────────────────────────────────────────────────────────────────────
@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY addedAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getById(id: Int): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: PlaylistEntity): Long

    @Delete
    suspend fun delete(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET lastSync = :time WHERE id = :id")
    suspend fun updateLastSync(id: Int, time: Long)

    @Query("SELECT COUNT(*) FROM playlists")
    suspend fun count(): Int
}

// ─── Database ─────────────────────────────────────────────────────────────────
@Database(
    entities = [PlaylistEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SitoDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile private var INSTANCE: SitoDatabase? = null

        fun getInstance(context: Context): SitoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SitoDatabase::class.java,
                    "sito_tv_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
