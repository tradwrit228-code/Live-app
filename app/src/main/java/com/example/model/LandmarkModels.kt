package com.example.model

data class ArHotspot(
    val id: String = "",
    val title: String = "",
    val category: String = "Architecture",
    val normalizedX: Float = 0.5f,
    val normalizedY: Float = 0.5f,
    val detail: String = ""
)

data class TimelineMilestone(
    val year: String = "",
    val title: String = "",
    val description: String = ""
)

data class LandmarkRecognitionResult(
    val landmarkName: String = "",
    val city: String = "",
    val country: String = "",
    val architecturalStyle: String = "",
    val yearBuilt: String = "",
    val confidenceScore: Float = 0.95f,
    val keyVisualFeatures: List<String> = emptyList(),
    val arHotspots: List<ArHotspot> = emptyList(),
    val shortSummary: String = ""
)

data class HistoricalDossier(
    val historicalEra: String = "",
    val deepHistoryNarrative: String = "",
    val constructionTimeline: List<TimelineMilestone> = emptyList(),
    val funFolkloreAndSecrets: List<String> = emptyList(),
    val culturalSignificance: String = "",
    val visitorIntel: String = "",
    val searchSources: List<String> = emptyList()
)

data class ArTourClip(
    val audioNarrationScript: String = "",
    val audioBase64: String? = null,
    val audioMimeType: String? = null,
    val voiceName: String = "Puck",
    val durationSeconds: Int = 30
)

enum class TourPipelineStep {
    IDLE,
    CAPTURING,
    RECOGNIZING,        // gemini-3.1-pro-preview
    FETCHING_HISTORY,   // gemini-3.5-flash with Google Search
    GENERATING_NARRATION, // gemini-3.1-flash-tts-preview
    READY,
    ERROR
}

enum class HistoricalEraView(val label: String, val epochDesc: String) {
    PRESENT("Present Day", "Current 21st-century state & visitor perspective"),
    MID_CENTURY("Mid 20th Century", "Wartime resilience & modernization epoch"),
    ORIGINAL_CONSTRUCTION("Original Era", "Foundation epoch & architectural debut")
}

data class PassportStamp(
    val id: String,
    val landmarkName: String,
    val city: String,
    val country: String,
    val scannedAt: Long,
    val badgeIcon: String = "🏛️"
)
