package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val avatarColorHex: String, // Hex string represent individual profile design e.g. red, blue, green, etc.
    val isKids: Boolean = false
)

@Entity(tableName = "watchlist", primaryKeys = ["profileId", "mediaId"])
data class WatchlistEntity(
    val profileId: Int,
    val mediaId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playback_progress", primaryKeys = ["profileId", "mediaId"])
data class PlaybackProgressEntity(
    val profileId: Int,
    val mediaId: String,
    val progressMs: Long,
    val durationMs: Long,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads", primaryKeys = ["profileId", "mediaId"])
data class DownloadEntity(
    val profileId: Int,
    val mediaId: String,
    val title: String,
    val type: String, // "movie" or "series"
    val duration: String,
    val rating: String,
    val imageUrl: String,
    val progress: Int, // 0 to 100
    val sizeMb: Double,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_movies")
data class UserMovieEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val type: String, // "movie" or "series"
    val imageUrl: String,
    val backdropUrl: String,
    val duration: String,
    val rating: String,
    val year: Int,
    val genre: String,
    val videoUrl: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMediaItem(): com.example.model.MediaItem {
        return com.example.model.MediaItem(
            id = id,
            title = title,
            description = description,
            type = type,
            imageUrl = imageUrl,
            backdropUrl = backdropUrl,
            duration = duration,
            rating = rating,
            year = year,
            genre = genre,
            matchScore = 98,
            isBillboard = false,
            videoUrl = videoUrl
        )
    }
}

