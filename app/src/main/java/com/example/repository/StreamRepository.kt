package com.example.repository

import android.content.Context
import com.example.database.*
import com.example.model.MediaItem
import com.example.model.MovieCatalog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow

class StreamRepository(private val db: AppDatabase) {

    private val profileDao = db.profileDao()
    private val watchlistDao = db.watchlistDao()
    private val playbackProgressDao = db.playbackProgressDao()
    private val downloadDao = db.downloadDao()
    private val userMovieDao = db.userMovieDao()

    fun getAllUserMovies(): Flow<List<UserMovieEntity>> = userMovieDao.getAllUserMovies()

    suspend fun insertUserMovie(movie: UserMovieEntity) {
        userMovieDao.insertUserMovie(movie)
    }

    suspend fun deleteUserMovie(movieId: String) {
        userMovieDao.deleteUserMovie(movieId)
    }

    // Simulated Active Device list
    val activeDevices = MutableStateFlow(
        listOf(
            SimulatedDevice("Living Room Smart TV", "Ready to Stream", "1080p Ultra HD", isCurrent = false),
            SimulatedDevice("Personal Pixel Pro", "Active - Streaming RED ALLIANCE", "UHD - HDR", isCurrent = true),
            SimulatedDevice("MacBook Pro Chrome", "Idle", "Auto HD", isCurrent = false)
        )
    )

    fun getAllProfiles(): Flow<List<ProfileEntity>> = profileDao.getAllProfiles()

    suspend fun createProfile(name: String, avatarColor: String, isKids: Boolean): Long {
        return profileDao.insertProfile(ProfileEntity(name = name, avatarColorHex = avatarColor, isKids = isKids))
    }

    suspend fun deleteProfile(profile: ProfileEntity) {
        profileDao.deleteProfile(profile)
    }

    fun getWatchlist(profileId: Int): Flow<List<WatchlistEntity>> = watchlistDao.getWatchlistForProfile(profileId)

    suspend fun isInWatchlist(profileId: Int, mediaId: String): Boolean = watchlistDao.isInWatchlist(profileId, mediaId)

    suspend fun addToWatchlist(profileId: Int, mediaId: String) {
        watchlistDao.addToWatchlist(WatchlistEntity(profileId, mediaId))
    }

    suspend fun removeFromWatchlist(profileId: Int, mediaId: String) {
        watchlistDao.removeFromWatchlist(profileId, mediaId)
    }

    fun getAllPlaybackProgress(profileId: Int): Flow<List<PlaybackProgressEntity>> = 
        playbackProgressDao.getAllProgressForProfile(profileId)

    suspend fun getProgressForMedia(profileId: Int, mediaId: String): PlaybackProgressEntity? =
        playbackProgressDao.getProgressForMedia(profileId, mediaId)

    suspend fun savePlaybackProgress(profileId: Int, mediaId: String, currentPositionMs: Long, totalDurationMs: Long) {
        playbackProgressDao.saveProgress(
            PlaybackProgressEntity(
                profileId = profileId,
                mediaId = mediaId,
                progressMs = currentPositionMs,
                durationMs = totalDurationMs,
                lastUpdated = System.currentTimeMillis()
            )
        )
    }

    suspend fun deletePlaybackProgress(profileId: Int, mediaId: String) {
        playbackProgressDao.deleteProgress(profileId, mediaId)
    }

    fun getAllDownloads(profileId: Int): Flow<List<DownloadEntity>> = downloadDao.getAllDownloadsForProfile(profileId)

    suspend fun saveDownload(download: DownloadEntity) {
        downloadDao.saveDownload(download)
    }

    suspend fun deleteDownload(profileId: Int, mediaId: String) {
        downloadDao.deleteDownload(profileId, mediaId)
    }

    // Perform a background mock download simulation
    fun simulateMovieDownload(profileId: Int, item: MediaItem): Flow<Int> = flow {
        val size = 1.2 + kotlin.random.Random.nextDouble() * 1.6 // random file size in GB
        val entity = DownloadEntity(
            profileId = profileId,
            mediaId = item.id,
            title = item.title,
            type = item.type,
            duration = item.duration,
            rating = item.rating,
            imageUrl = item.imageUrl,
            progress = 0,
            sizeMb = size * 1024,
            isCompleted = false
        )
        saveDownload(entity)

        for (progress in 10..100 step 10) {
            delay(400) // Fast download simulator
            val updated = entity.copy(
                progress = progress,
                isCompleted = progress == 100
            )
            saveDownload(updated)
            emit(progress)
        }
    }

    // Handle seamless device transfer: simulated casting command
    fun transferStreamToDevice(deviceName: String, currentMediaTitle: String) {
        val currentList = activeDevices.value
        val updated = currentList.map { device ->
            when (device.name) {
                deviceName -> device.copy(
                    status = "Streaming HD: $currentMediaTitle",
                    quality = "Ultra HD 4K",
                    isCurrent = true
                )
                else -> {
                    if (device.isCurrent) {
                        device.copy(status = "Idle", isCurrent = false)
                    } else {
                        device
                    }
                }
            }
        }
        activeDevices.value = updated
    }
}

data class SimulatedDevice(
    val name: String,
    val status: String,
    val quality: String,
    val isCurrent: Boolean
)
