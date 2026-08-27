package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.audio.NarrationAudioManager
import com.example.data.db.LandmarkDatabase
import com.example.data.db.LandmarkReviewEntity
import com.example.data.db.LandmarkTourEntity
import com.example.data.network.GeminiApiClient
import com.example.data.repository.CuratedCityLandmark
import com.example.data.repository.LandmarkRepository
import com.example.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class LandmarkViewModel(application: Application) : AndroidViewModel(application) {

    private val geminiClient = GeminiApiClient()
    private val database = LandmarkDatabase.getDatabase(application)
    private val audioManager = NarrationAudioManager(application)
    private val repository = LandmarkRepository(application, geminiClient, database.landmarkDao())

    val sampleLandmarks = repository.getCuratedLandmarks()
    val savedTours: StateFlow<List<LandmarkTourEntity>> = repository.getSavedTours()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentRecognition = MutableStateFlow<LandmarkRecognitionResult?>(null)
    val currentRecognition: StateFlow<LandmarkRecognitionResult?> = _currentRecognition.asStateFlow()

    // Flux réactifs pour les avis et notes
    val currentReviews: StateFlow<List<LandmarkReviewEntity>> = _currentRecognition.flatMapLatest { recognition ->
        if (recognition != null) repository.getReviewsForLandmark(recognition.landmarkName)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentAverageRating: StateFlow<Double?> = _currentRecognition.flatMapLatest { recognition ->
        if (recognition != null) repository.getAverageRatingForLandmark(recognition.landmarkName)
        else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentReviewCount: StateFlow<Int> = _currentRecognition.flatMapLatest { recognition ->
        if (recognition != null) repository.getReviewCountForLandmark(recognition.landmarkName)
        else flowOf(0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _currentDossier = MutableStateFlow<HistoricalDossier?>(null)
    val currentDossier: StateFlow<HistoricalDossier?> = _currentDossier.asStateFlow()

    private val _pipelineStep = MutableStateFlow(TourPipelineStep.IDLE)
    val pipelineStep: StateFlow<TourPipelineStep> = _pipelineStep.asStateFlow()

    private val _selectedHotspot = MutableStateFlow<ArHotspot?>(null)
    val selectedHotspot: StateFlow<ArHotspot?> = _selectedHotspot.asStateFlow()

    private val _selectedEra = MutableStateFlow(HistoricalEraView.MODERN)
    val selectedEra: StateFlow<HistoricalEraView> = _selectedEra.asStateFlow()

    private val _selectedVoice = MutableStateFlow("Puck")
    val selectedVoice: StateFlow<String> = _selectedVoice.asStateFlow()

    private val _customApiKey = MutableStateFlow<String?>(null)
    val customApiKey: StateFlow<String?> = _customApiKey.asStateFlow()

    val audioState = audioManager.playerState

    init {
        viewModelScope.launch { repository.seedDefaultReviewsIfEmpty() }
        selectSampleLandmark(sampleLandmarks.first(), autoPlay = false)
    }

    fun submitReview(landmarkName: String, authorName: String, rating: Int, reviewText: String, tag: String = "Explorer") {
        viewModelScope.launch {
            repository.submitReview(landmarkName, authorName, rating, reviewText, tag)
        }
    }

    fun deleteReview(review: LandmarkReviewEntity) {
        viewModelScope.launch { repository.deleteReview(review) }
    }

    fun selectSampleLandmark(sample: CuratedCityLandmark, autoPlay: Boolean = true) {
        _currentRecognition.value = sample.recognition
        _currentDossier.value = sample.dossier
        _pipelineStep.value = TourPipelineStep.READY
        _selectedHotspot.value = sample.recognition.suggestedHotspots.firstOrNull()
        if (autoPlay) audioManager.playCuratedNarration(sample.sampleAudioResId)
    }

    fun toggleAudioPlayback() {
        if (audioState.value.isPlaying) audioManager.pause()
        else audioManager.resume()
    }

    fun seekAudio(progressFraction: Float) = audioManager.seekTo(progressFraction)
    fun setSpeed(speed: Float) = audioManager.setPlaybackSpeed(speed)
    fun setHistoricalEra(era: HistoricalEraView) { _selectedEra.value = era }
    fun setVoice(voice: String) { _selectedVoice.value = voice }
    fun selectHotspot(hotspot: ArHotspot?) { _selectedHotspot.value = hotspot }
    fun resetToCamera() { audioManager.stop(); _selectedHotspot.value = null }

    // Nouvelles méthodes manquantes
    fun loadTourFromEntity(tour: LandmarkTourEntity) {
        viewModelScope.launch {
            val recognition = LandmarkRecognitionResult(
                landmarkName = tour.landmarkName,
                city = tour.city,
                country = tour.country,
                yearBuilt = tour.yearBuilt,
                architecturalStyle = tour.architecturalStyle,
                briefSummary = tour.deepHistoryNarrative.take(200),
                keyVisualFeatures = emptyList(),
                confidence = 0.95f
            )
            _currentRecognition.value = recognition
            _pipelineStep.value = TourPipelineStep.READY
            
            if (!tour.narrationAudioPath.isNullOrEmpty()) {
                audioManager.playCustomNarration(tour.narrationAudioPath)
            }
        }
    }

    fun toggleFavorite(tour: LandmarkTourEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(tour.id)
        }
    }

    fun deleteTour(tour: LandmarkTourEntity) {
        viewModelScope.launch {
            repository.deleteTour(tour)
        }
    }

    fun setCustomApiKey(apiKey: String) {
        _customApiKey.value = apiKey
        if (apiKey.isNotEmpty()) {
            geminiClient.setApiKey(apiKey)
        }
    }
}
