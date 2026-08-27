package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.audio.NarrationAudioManager
import com.example.data.db.LandmarkDatabase
import com.example.data.db.LandmarkTourEntity
import com.example.data.network.GeminiApiClient
import com.example.data.repository.CuratedCityLandmark
import com.example.data.repository.LandmarkRepository
import com.example.model.ArHotspot
import com.example.model.ArTourClip
import com.example.model.HistoricalDossier
import com.example.model.HistoricalEraView
import com.example.model.LandmarkRecognitionResult
import com.example.model.TimelineMilestone
import com.example.model.TourPipelineStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class LandmarkViewModel(application: Application) : AndroidViewModel(application) {

    private val geminiClient = GeminiApiClient()
    private val database = LandmarkDatabase.getInstance(application)
    private val repository = LandmarkRepository(application, database.landmarkDao(), geminiClient)
    val audioManager = NarrationAudioManager(application)

    val savedTours: StateFlow<List<LandmarkTourEntity>> = repository.savedTours.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val sampleLandmarks: List<CuratedCityLandmark> = repository.sampleLandmarks

    private val _pipelineStep = MutableStateFlow(TourPipelineStep.IDLE)
    val pipelineStep: StateFlow<TourPipelineStep> = _pipelineStep.asStateFlow()

    private val _currentRecognition = MutableStateFlow<LandmarkRecognitionResult?>(null)
    val currentRecognition: StateFlow<LandmarkRecognitionResult?> = _currentRecognition.asStateFlow()

    private val _currentDossier = MutableStateFlow<HistoricalDossier?>(null)
    val currentDossier: StateFlow<HistoricalDossier?> = _currentDossier.asStateFlow()

    private val _currentTourClip = MutableStateFlow<ArTourClip?>(null)
    val currentTourClip: StateFlow<ArTourClip?> = _currentTourClip.asStateFlow()

    private val _currentPhotoBitmap = MutableStateFlow<Bitmap?>(null)
    val currentPhotoBitmap: StateFlow<Bitmap?> = _currentPhotoBitmap.asStateFlow()

    private val _currentPhotoUri = MutableStateFlow<Uri?>(null)
    val currentPhotoUri: StateFlow<Uri?> = _currentPhotoUri.asStateFlow()

    private val _selectedHotspot = MutableStateFlow<ArHotspot?>(null)
    val selectedHotspot: StateFlow<ArHotspot?> = _selectedHotspot.asStateFlow()

    private val _selectedEra = MutableStateFlow(HistoricalEraView.PRESENT)
    val selectedEra: StateFlow<HistoricalEraView> = _selectedEra.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _selectedVoice = MutableStateFlow("Puck")
    val selectedVoice: StateFlow<String> = _selectedVoice.asStateFlow()

    val audioState = audioManager.playerState

    init {
        // Auto-select the first curated landmark for immediate testable preview
        selectSampleLandmark(sampleLandmarks.first(), autoPlay = false)
    }

    fun setApiKey(key: String) {
        _customApiKey.value = key.trim()
    }

    fun setVoice(voice: String) {
        _selectedVoice.value = voice
        // Regenerate or replay narration with new voice if tour ready
        _currentTourClip.value?.let { clip ->
            if (clip.audioNarrationScript.isNotBlank()) {
                viewModelScope.launch {
                    val updated = repository.generateTourAudioClip(
                        script = clip.audioNarrationScript,
                        voiceName = voice,
                        apiKey = _customApiKey.value
                    )
                    _currentTourClip.value = updated
                    audioManager.playClip(
                        script = updated.audioNarrationScript,
                        audioBase64 = updated.audioBase64,
                        audioMimeType = updated.audioMimeType
                    )
                }
            }
        }
    }

    fun selectSampleLandmark(sample: CuratedCityLandmark, autoPlay: Boolean = true) {
        viewModelScope.launch {
            _pipelineStep.value = TourPipelineStep.CAPTURING
            _errorMessage.value = null
            audioManager.stop()

            val bitmap = repository.loadBitmapFromResource(sample.drawableRes)
            _currentPhotoBitmap.value = bitmap
            _currentPhotoUri.value = null

            val recognition = LandmarkRecognitionResult(
                landmarkName = sample.name,
                city = sample.city,
                country = sample.country,
                architecturalStyle = sample.style,
                yearBuilt = sample.yearBuilt,
                confidenceScore = 0.99f,
                keyVisualFeatures = listOf(sample.style, "World Heritage Heritage", sample.era),
                arHotspots = sample.hotspots,
                shortSummary = sample.summary
            )
            _currentRecognition.value = recognition

            val dossier = HistoricalDossier(
                historicalEra = sample.era,
                deepHistoryNarrative = sample.narrative,
                constructionTimeline = sample.timeline,
                funFolkloreAndSecrets = sample.folklore,
                culturalSignificance = "Global cultural monument of monumental heritage.",
                visitorIntel = "Best experienced during golden hour for panoramic vistas.",
                searchSources = listOf("Google Search Grounding", "UNESCO World Heritage Data", "Official Historical Registry")
            )
            _currentDossier.value = dossier

            val clip = ArTourClip(
                audioNarrationScript = sample.defaultScript,
                audioBase64 = null,
                voiceName = _selectedVoice.value,
                durationSeconds = (sample.defaultScript.split(" ").size / 2.5).toInt()
            )
            _currentTourClip.value = clip
            _pipelineStep.value = TourPipelineStep.READY

            if (autoPlay) {
                audioManager.playClip(
                    script = clip.audioNarrationScript,
                    audioBase64 = clip.audioBase64,
                    audioMimeType = clip.audioMimeType
                )
            }
        }
    }

    fun processCapturedPhoto(bitmap: Bitmap, imagePathOrUri: String = "camera_capture") {
        viewModelScope.launch {
            try {
                audioManager.stop()
                _currentPhotoBitmap.value = bitmap
                _errorMessage.value = null

                // 1. Multimodal Recognition with gemini-3.1-pro-preview
                _pipelineStep.value = TourPipelineStep.RECOGNIZING
                val (recognition, dossier) = repository.recognizeAndResearch(
                    bitmap = bitmap,
                    apiKey = _customApiKey.value.ifBlank { null }
                )
                _currentRecognition.value = recognition

                // 2. Search Grounded History with gemini-3.5-flash
                _pipelineStep.value = TourPipelineStep.FETCHING_HISTORY
                _currentDossier.value = dossier

                // 3. Audio Narration with gemini-3.1-flash-tts-preview
                _pipelineStep.value = TourPipelineStep.GENERATING_NARRATION
                val narrationScript = buildNarrationScript(recognition, dossier)
                val clip = repository.generateTourAudioClip(
                    script = narrationScript,
                    voiceName = _selectedVoice.value,
                    apiKey = _customApiKey.value.ifBlank { null }
                )
                _currentTourClip.value = clip

                // Auto-save to Room Database
                repository.saveTourToDatabase(
                    recognition = recognition,
                    dossier = dossier,
                    audioScript = narrationScript,
                    imagePath = imagePathOrUri
                )

                _pipelineStep.value = TourPipelineStep.READY

                // Auto-play the AR Tour
                audioManager.playClip(
                    script = clip.audioNarrationScript,
                    audioBase64 = clip.audioBase64,
                    audioMimeType = clip.audioMimeType
                )
            } catch (e: Exception) {
                _pipelineStep.value = TourPipelineStep.ERROR
                _errorMessage.value = e.localizedMessage ?: "Failed to analyze landmark photo."
            }
        }
    }

    private fun buildNarrationScript(recognition: LandmarkRecognitionResult, dossier: HistoricalDossier): String {
        return "Welcome to ${recognition.city}. Standing before you is the iconic ${recognition.landmarkName}, constructed in ${recognition.yearBuilt}. " +
                "${recognition.shortSummary} " +
                (dossier.funFolkloreAndSecrets.firstOrNull()?.let { "Fascinating trivia: $it " } ?: "") +
                "Explore the highlighted AR anchor points on your display for in-depth structural secrets."
    }

    fun loadTourFromEntity(entity: LandmarkTourEntity) {
        viewModelScope.launch {
            audioManager.stop()
            _errorMessage.value = null

            val hotspots = parseHotspotsJson(entity.hotspotsJson)
            val timeline = parseTimelineJson(entity.timelineJson)
            val folklore = parseStringListJson(entity.folkloreJson)
            val sources = parseStringListJson(entity.searchSourcesJson)

            val recognition = LandmarkRecognitionResult(
                landmarkName = entity.landmarkName,
                city = entity.city,
                country = entity.country,
                architecturalStyle = entity.style,
                yearBuilt = entity.yearBuilt,
                confidenceScore = 0.98f,
                keyVisualFeatures = listOf(entity.style, entity.yearBuilt),
                arHotspots = hotspots,
                shortSummary = entity.summary
            )
            _currentRecognition.value = recognition

            val dossier = HistoricalDossier(
                historicalEra = entity.style,
                deepHistoryNarrative = entity.narrative,
                constructionTimeline = timeline,
                funFolkloreAndSecrets = folklore,
                culturalSignificance = "Historic saved landmark tour.",
                visitorIntel = "Revisit this world heritage tour anytime from your passport.",
                searchSources = sources
            )
            _currentDossier.value = dossier

            val clip = ArTourClip(
                audioNarrationScript = entity.audioScript,
                audioBase64 = null,
                voiceName = _selectedVoice.value,
                durationSeconds = (entity.audioScript.split(" ").size / 2.5).toInt().coerceAtLeast(15)
            )
            _currentTourClip.value = clip
            _pipelineStep.value = TourPipelineStep.READY

            audioManager.playClip(
                script = clip.audioNarrationScript,
                audioBase64 = clip.audioBase64,
                audioMimeType = clip.audioMimeType
            )
        }
    }

    fun selectHotspot(hotspot: ArHotspot?) {
        _selectedHotspot.value = hotspot
    }

    fun setHistoricalEra(era: HistoricalEraView) {
        _selectedEra.value = era
    }

    fun playAudioTour() {
        _currentTourClip.value?.let { clip ->
            audioManager.playClip(
                script = clip.audioNarrationScript,
                audioBase64 = clip.audioBase64,
                audioMimeType = clip.audioMimeType
            )
        }
    }

    fun togglePlayPause() {
        audioManager.togglePlayPause()
    }

    fun seekAudio(progress: Float) {
        audioManager.seekTo(progress)
    }

    fun setSpeed(speed: Float) {
        audioManager.setSpeed(speed)
    }

    fun toggleFavorite(id: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
        }
    }

    fun deleteTour(entity: LandmarkTourEntity) {
        viewModelScope.launch {
            repository.deleteTour(entity)
        }
    }

    fun resetToCamera() {
        audioManager.stop()
        _selectedHotspot.value = null
        _pipelineStep.value = TourPipelineStep.IDLE
    }

    private fun parseHotspotsJson(jsonStr: String): List<ArHotspot> {
        val list = mutableListOf<ArHotspot>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ArHotspot(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        category = obj.optString("category"),
                        normalizedX = obj.optDouble("normalizedX", 0.5).toFloat(),
                        normalizedY = obj.optDouble("normalizedY", 0.5).toFloat(),
                        detail = obj.optString("detail")
                    )
                )
            }
        } catch (e: Exception) {
            // Ignore
        }
        return list
    }

    private fun parseTimelineJson(jsonStr: String): List<TimelineMilestone> {
        val list = mutableListOf<TimelineMilestone>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    TimelineMilestone(
                        year = obj.optString("year"),
                        title = obj.optString("title"),
                        description = obj.optString("description")
                    )
                )
            }
        } catch (e: Exception) {
            // Ignore
        }
        return list
    }

    private fun parseStringListJson(jsonStr: String): List<String> {
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (e: Exception) {
            // Ignore
        }
        return list
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }
}
