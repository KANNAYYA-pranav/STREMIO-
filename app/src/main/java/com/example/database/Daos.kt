package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY id ASC")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity): Long

    @Delete
    suspend fun deleteProfile(profile: ProfileEntity)
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist WHERE profileId = :profileId ORDER BY addedAt DESC")
    fun getWatchlistForProfile(profileId: Int): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE profileId = :profileId AND mediaId = :mediaId)")
    suspend fun isInWatchlist(profileId: Int, mediaId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWatchlist(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE profileId = :profileId AND mediaId = :mediaId")
    suspend fun removeFromWatchlist(profileId: Int, mediaId: String)
}

@Dao
interface PlaybackProgressDao {
    @Query("SELECT * FROM playback_progress WHERE profileId = :profileId ORDER BY lastUpdated DESC")
    fun getAllProgressForProfile(profileId: Int): Flow<List<PlaybackProgressEntity>>

    @Query("SELECT * FROM playback_progress WHERE profileId = :profileId AND mediaId = :mediaId")
    suspend fun getProgressForMedia(profileId: Int, mediaId: String): PlaybackProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: PlaybackProgressEntity)

    @Query("DELETE FROM playback_progress WHERE profileId = :profileId AND mediaId = :mediaId")
    suspend fun deleteProgress(profileId: Int, mediaId: String)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun getAllDownloadsForProfile(profileId: Int): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDownload(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE profileId = :profileId AND mediaId = :mediaId")
    suspend fun deleteDownload(profileId: Int, mediaId: String)
}

@Dao
interface UserMovieDao {
    @Query("SELECT * FROM user_movies ORDER BY createdAt DESC")
    fun getAllUserMovies(): Flow<List<UserMovieEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserMovie(movie: UserMovieEntity)

    @Query("DELETE FROM user_movies WHERE id = :movieId")
    suspend fun deleteUserMovie(movieId: String)
}

