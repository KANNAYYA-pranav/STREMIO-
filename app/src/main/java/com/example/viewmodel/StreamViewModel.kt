package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.database.AppDatabase
import com.example.database.DownloadEntity
import com.example.database.PlaybackProgressEntity
import com.example.database.ProfileEntity
import com.example.database.WatchlistEntity
import com.example.database.UserMovieEntity
import com.example.model.MediaItem
import com.example.model.MovieCatalog
import com.example.repository.StreamRepository
import com.example.repository.SimulatedDevice
import com.example.api.GeminiService
import com.example.api.RecommendationResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StreamViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val repository = StreamRepository(db)

    // Combined catalog combining static templates and user uploaded movies
    val allMediaItems: StateFlow<List<MediaItem>> = repository.getAllUserMovies()
        .map { customEntities ->
            val customItems = customEntities.map { it.toMediaItem() }
            MovieCatalog.items + customItems
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MovieCatalog.items)

    fun getMoviesByCategory(category: String, allItems: List<MediaItem>): List<MediaItem> {
        return when (category.lowercase()) {
            "trending" -> allItems.shuffled()
            "movies" -> allItems.filter { it.type == "movie" }
            "series" -> allItems.filter { it.type == "series" }
            "cyberpunk" -> allItems.filter { it.genre.contains("Cyberpunk", ignoreCase = true) || it.description.contains("cyberpunk", ignoreCase = true) }
            "scifi" -> allItems.filter { it.genre.contains("sci-fi", ignoreCase = true) || it.genre.contains("cosmos", ignoreCase = true) }
            "anime" -> allItems.filter { it.genre.contains("anime", ignoreCase = true) }
            else -> allItems
        }
    }

    // Profiles State
    val allProfiles: StateFlow<List<ProfileEntity>> = repository.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProfile = MutableStateFlow<ProfileEntity?>(null)
    val selectedProfile: StateFlow<ProfileEntity?> = _selectedProfile.asStateFlow()

    // Watchlist State (dynamically loaded for selected profile)
    val watchlist: StateFlow<List<WatchlistEntity>> = selectedProfile
        .flatMapLatest { profile ->
            if (profile != null) repository.getWatchlist(profile.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Playback Progress State
    val allProgress: StateFlow<List<PlaybackProgressEntity>> = selectedProfile
        .flatMapLatest { profile ->
            if (profile != null) repository.getAllPlaybackProgress(profile.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Downloads State
    val downloads: StateFlow<List<DownloadEntity>> = selectedProfile
        .flatMapLatest { profile ->
            if (profile != null) repository.getAllDownloads(profile.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Devices Casting State
    val activeDevices: StateFlow<List<SimulatedDevice>> = repository.activeDevices.asStateFlow()

    // Smart Gemini Recommendation Concierge State
    private val _aiRecommendation = MutableStateFlow<RecommendationResult?>(null)
    val aiRecommendation: StateFlow<RecommendationResult?> = _aiRecommendation.asStateFlow()

    private val _aiSearching = MutableStateFlow(false)
    val aiSearching: StateFlow<Boolean> = _aiSearching.asStateFlow()

    // Mock active download state map
    private val _downloadProgressMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgressMap: StateFlow<Map<String, Int>> = _downloadProgressMap.asStateFlow()

    init {
        // Pre-create some default profiles if database is empty on launch
        viewModelScope.launch {
            allProfiles.collectLatest { profiles ->
                if (profiles.isEmpty()) {
                    repository.createProfile("Mom", "#E50914", isKids = false)
                    repository.createProfile("Dad", "#0080FF", isKids = false)
                    repository.createProfile("Junior", "#32CD32", isKids = true)
                    repository.createProfile("Guest", "#FFD700", isKids = false)
                }
            }
        }
    }

    fun selectProfile(profile: ProfileEntity?) {
        _selectedProfile.value = profile
        // Clear any previous transient searches or recommendation results to reset
        _aiRecommendation.value = null
    }

    fun createProfile(name: String, avatarColor: String, isKids: Boolean) {
        viewModelScope.launch {
            repository.createProfile(name, avatarColor, isKids)
        }
    }

    fun deleteProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            if (_selectedProfile.value?.id == profile.id) {
                _selectedProfile.value = null
            }
            repository.deleteProfile(profile)
        }
    }

    // Toggle items on user's selected watchlist
    fun toggleWatchlist(mediaId: String) {
        val currentProfile = _selectedProfile.value ?: return
        viewModelScope.launch {
            if (repository.isInWatchlist(currentProfile.id, mediaId)) {
                repository.removeFromWatchlist(currentProfile.id, mediaId)
            } else {
                repository.addToWatchlist(currentProfile.id, mediaId)
            }
        }
    }

    // Direct check if item is in current profile's list
    suspend fun isMediaWatchlisted(mediaId: String): Boolean {
        val currentProfile = _selectedProfile.value ?: return false
        return repository.isInWatchlist(currentProfile.id, mediaId)
    }

    // Save and retrieve streaming state
    fun savePlaybackProgress(mediaId: String, currentPositionMs: Long, totalDurationMs: Long) {
        val currentProfile = _selectedProfile.value ?: return
        viewModelScope.launch {
            repository.savePlaybackProgress(currentProfile.id, mediaId, currentPositionMs, totalDurationMs)
        }
    }

    fun deletePlaybackProgress(mediaId: String) {
        val currentProfile = _selectedProfile.value ?: return
        viewModelScope.launch {
            repository.deletePlaybackProgress(currentProfile.id, mediaId)
        }
    }

    suspend fun getProgressForMedia(mediaId: String): PlaybackProgressEntity? {
        val currentProfile = _selectedProfile.value ?: return null
        return repository.getProgressForMedia(currentProfile.id, mediaId)
    }

    // Cast current movie stream to external smart target screen
    fun castStreamToDevice(deviceName: String, currentMediaTitle: String) {
        viewModelScope.launch {
            repository.transferStreamToDevice(deviceName, currentMediaTitle)
        }
    }

    // Trigger seamless background downloading
    fun downloadMovie(item: MediaItem) {
        val currentProfile = _selectedProfile.value ?: return
        viewModelScope.launch {
            repository.simulateMovieDownload(currentProfile.id, item).collect { progress ->
                val newMap = _downloadProgressMap.value.toMutableMap()
                if (progress == 100) {
                    newMap.remove(item.id)
                } else {
                    newMap[item.id] = progress
                }
                _downloadProgressMap.value = newMap
            }
        }
    }

    fun deleteDownload(mediaId: String) {
        val currentProfile = _selectedProfile.value ?: return
        viewModelScope.launch {
            repository.deleteDownload(currentProfile.id, mediaId)
        }
    }

    // Ask Gemini recommendation AI
    fun searchWithGemini(prompt: String) {
        if (prompt.isBlank()) return
        _aiSearching.value = true
        _aiRecommendation.value = null
        viewModelScope.launch {
            try {
                val result = GeminiService.getRecommendations(prompt)
                _aiRecommendation.value = result
            } catch (e: Exception) {
                // Handled gracefully in mock fallback
            } finally {
                _aiSearching.value = false
            }
        }
    }

    // Custom Movie Upload Logic
    fun uploadMovie(
        title: String,
        description: String,
        type: String,
        genre: String,
        duration: String,
        rating: String,
        year: Int,
        imageUrl: String?,
        backdropUrl: String?,
        videoUrl: String?
    ) {
        viewModelScope.launch {
            val generatedId = "custom_" + System.currentTimeMillis()
            val finalImageUrl = if (imageUrl.isNullOrBlank()) {
                "https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&q=80&w=400"
            } else imageUrl

            val finalBackdropUrl = if (backdropUrl.isNullOrBlank()) {
                "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&q=80&w=1200"
            } else backdropUrl

            val finalVideoUrl = if (videoUrl.isNullOrBlank()) {
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            } else videoUrl

            val entity = UserMovieEntity(
                id = generatedId,
                title = title,
                description = description,
                type = type,
                imageUrl = finalImageUrl,
                backdropUrl = finalBackdropUrl,
                duration = duration,
                rating = rating,
                year = year,
                genre = genre,
                videoUrl = finalVideoUrl
            )
            repository.insertUserMovie(entity)
        }
    }

    fun deleteCustomMovie(movieId: String) {
        viewModelScope.launch {
            repository.deleteUserMovie(movieId)
        }
    }
}
