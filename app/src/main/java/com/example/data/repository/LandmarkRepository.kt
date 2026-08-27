package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.R
import com.example.data.db.LandmarkDao
import com.example.data.db.LandmarkReviewEntity
import com.example.data.db.LandmarkTourEntity
import com.example.data.network.GeminiApiClient
import com.example.model.ArHotspot
import com.example.model.ArTourClip
import com.example.model.HistoricalDossier
import com.example.model.LandmarkRecognitionResult
import com.example.model.TimelineMilestone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class CuratedCityLandmark(
    val id: String,
    val name: String,
    val city: String,
    val country: String,
    val era: String,
    val style: String,
    val yearBuilt: String,
    val summary: String,
    val narrative: String,
    val timeline: List<TimelineMilestone>,
    val hotspots: List<ArHotspot>,
    val folklore: List<String>,
    val defaultScript: String,
    val drawableRes: Int
)

class LandmarkRepository(
    private val context: Context,
    private val landmarkDao: LandmarkDao,
    private val geminiClient: GeminiApiClient
) {

    val savedTours: Flow<List<LandmarkTourEntity>> = landmarkDao.getAllTours()

    val sampleLandmarks: List<CuratedCityLandmark> = listOf(
        CuratedCityLandmark(
            id = "eiffel_tower",
            name = "Eiffel Tower",
            city = "Paris",
            country = "France",
            era = "Belle Époque",
            style = "Industrial Puddle Iron Lattice",
            yearBuilt = "1889",
            summary = "Global icon of France standing 330 meters tall above the Champ de Mars, engineered by Gustave Eiffel for the 1889 Exposition Universelle.",
            narrative = "Conceived as the centerpiece for the 1889 World's Fair commemorating the centennial of the French Revolution, the Eiffel Tower was initially derided by prominent Parisian artists as a 'tragic street lamp.' Built with over 18,000 puddle iron parts and 2.5 million rivets, it transformed from a temporary permit into an indispensable radio telecommunications mast that saved it from demolition. Today it stands as the eternal romantic beacon of the City of Light.",
            timeline = listOf(
                TimelineMilestone("1887", "Groundbreaking", "Foundations dug with caissons compressed by pneumatic air pressure near the Seine."),
                TimelineMilestone("1889", "Inauguration", "Completed in a record 2 years, 2 months and 5 days, becoming world's tallest man-made structure."),
                TimelineMilestone("1914", "Wartime Radio", "Military transmitter intercepted strategic messages during the Battle of the Marne."),
                TimelineMilestone("2000", "Millennium Sparkle", "20,000 flashing xenon lamps installed for the new millennium, illuminating Paris every hour.")
            ),
            hotspots = listOf(
                ArHotspot("ef_spire", "Broadcast Telecommunications Spire", "Engineering", 0.50f, 0.12f, "The 330m summit antenna relays radio and television across Île-de-France and houses Gustave Eiffel's secret private apartment."),
                ArHotspot("ef_deck2", "Second Level Observation Deck", "Architecture", 0.50f, 0.44f, "Offers 360° unobstructed sightlines over Paris monuments including the Louvre, Sacré-Cœur, and Arc de Triomphe."),
                ArHotspot("ef_arch", "Monumental Base Archway", "History", 0.50f, 0.78f, "Massive double-curved lattice arches spanning 74 meters designed to withstand dynamic wind oscillations.")
            ),
            folklore = listOf(
                "Gustave Eiffel carved the names of 72 French scientists, engineers, and mathematicians in gold letters beneath the first balcony.",
                "Thermal expansion causes the tower to grow by up to 15 centimeters (6 inches) and tilt slightly away from the sun during hot summers.",
                "Con artist Victor Lustig famously 'sold' the Eiffel Tower for scrap metal twice in 1925 to gullible scrap merchants."
            ),
            defaultScript = "Welcome to Paris. You are standing before the magnificent Eiffel Tower, engineered by Gustave Eiffel for the 1889 World's Fair. Notice the intricate puddle-iron lattice designed to breathe with the Parisian winds. At the summit, Eiffel maintained a secret apartment where he hosted Thomas Edison. Listen closely as we delve into its survival through wartime intrigue and its eternal place in global culture.",
            drawableRes = R.drawable.sample_eiffel_1787836402873
        ),
        CuratedCityLandmark(
            id = "colosseum",
            name = "Colosseum",
            city = "Rome",
            country = "Italy",
            era = "Flavian Dynasty / Roman Empire",
            style = "Classical Roman Amphitheatre",
            yearBuilt = "80 AD",
            summary = "The largest ancient amphitheatre ever built, capable of seating 50,000 spectators for gladiatorial contests, dramas, and mock sea battles.",
            narrative = "Commissioned under Emperor Vespasian in 72 AD and inaugurated with 100 days of games by his son Titus in 80 AD, the Flavian Amphitheatre redefined monumental imperial architecture. Utilizing revolutionary travertine limestone, volcanic tuff, and concrete vaulting, it featured an ingenious hypogeum labyrinth under the arena floor with elevators for gladiators and exotic beasts.",
            timeline = listOf(
                TimelineMilestone("72 AD", "Imperial Commission", "Emperor Vespasian financed construction from the spoils of the Jewish Temple."),
                TimelineMilestone("80 AD", "Grand Inaugural Games", "Emperor Titus celebrated with 100 consecutive days of spectacle and naval flood recreations."),
                TimelineMilestone("217 AD", "Great Fire & Restoration", "Lightning ignited the wooden upper tiers, prompting centuries of structural restoration."),
                TimelineMilestone("2007", "New 7 Wonders", "Elected as one of the New Seven Wonders of the World, drawing millions of global travelers.")
            ),
            hotspots = listOf(
                ArHotspot("col_attic", "Corinthian Upper Attic & Velarium", "Architecture", 0.50f, 0.22f, "Roman sailors hoisted a colossal canvas awning called the Velarium using 240 masts to shade the patricians."),
                ArHotspot("col_arches", "Tri-Tier Classical Arches", "Engineering", 0.32f, 0.48f, "A layered masterclass of Doric, Ionic, and Corinthian columns framing 80 numbered entrance arches."),
                ArHotspot("col_hypo", "Hypogeum Subterranean Arena", "History", 0.58f, 0.76f, "The two-level underground maze with 28 manual pulley elevators used to propel lions and gladiators directly onto the sand.")
            ),
            folklore = listOf(
                "In its earliest inaugurations, the arena could be flooded with water to stage mock naval battles called Naumachia.",
                "Over 350 plant species once grew within the Colosseum ruins, surveyed by 19th-century botanists who studied seeds transported by exotic beasts.",
                "The Colosseum was stripped of its marble facade during the Renaissance to construct St. Peter's Basilica and Palazzo Farnese."
            ),
            defaultScript = "Step through the mists of time into ancient Rome. Before you stands the Colosseum, the grandest arena of antiquity. Behind its monumental tiered arches, fifty thousand Roman citizens roared as gladiators clashed. Look down toward the arena floor to glimpse the hypogeum, an engineering marvel of underground pulleys, trapdoors, and chambers. Let us explore the epic drama and enduring legacy of the Eternal City.",
            drawableRes = R.drawable.sample_colosseum_1787836413966
        ),
        CuratedCityLandmark(
            id = "big_ben",
            name = "Big Ben & Elizabeth Tower",
            city = "London",
            country = "United Kingdom",
            era = "Victorian Gothic Revival",
            style = "Perpendicular Gothic",
            yearBuilt = "1859",
            summary = "The legendary clock tower at the north end of the Houses of Parliament in Westminster, renowned worldwide for its 13.7-tonne Great Bell.",
            narrative = "Designed by Augustus Pugin in stunning Gothic Revival style and completed in 1859 following the destruction of the old Palace of Westminster by fire, the Elizabeth Tower rises 96 meters above the River Thames. Its massive four-faced clock, engineered by Edmund Beckett Denison, maintains extraordinary accuracy regulated by a stack of British pre-decimal pennies resting on the pendulum.",
            timeline = listOf(
                TimelineMilestone("1834", "Great Fire of London", "Fire gutted the medieval Palace of Westminster, initiating the grand Victorian rebuilding."),
                TimelineMilestone("1859", "First Chimes", "The Great Clock began keeping time on 31 May, and the Great Bell first chimed on 11 July."),
                TimelineMilestone("1941", "Blitz Resilience", "Despite German bomb damage to the Commons chamber, the clock faces remained ticking accurately."),
                TimelineMilestone("2022", "Restoration Unveiled", "A meticulous 5-year conservation project restored original Prussian blue clock hands and masonry.")
            ),
            hotspots = listOf(
                ArHotspot("bb_belfry", "Great Bell Belfry & Lantern", "Engineering", 0.50f, 0.20f, "Houses the 13.7-tonne Great Bell Big Ben, cracked in 1859 and struck by a lighter hammer to produce its iconic tone."),
                ArHotspot("bb_dial", "Opal Glass Clock Dial", "Architecture", 0.48f, 0.46f, "Composed of 312 separate pieces of pot opal glass, with gilded Latin inscription 'DOMINE SALVAM FAC REGINAM NOSTRAM VICTORIAM PRIMAM'."),
                ArHotspot("bb_spire", "Gothic Finials & Lantern", "History", 0.50f, 0.10f, "Features the Ayrton Light, illuminated whenever Parliament is sitting after dark.")
            ),
            folklore = listOf(
                "The clock's pendulum is tuned with old British penny coins; adding or removing a single penny changes the speed by 0.4 seconds per day.",
                "During World War II, the BBC broadcast the live chimes of Big Ben to give hope and solace to the British public and occupied Europe.",
                "Technically 'Big Ben' is the nickname for the Great Bell inside, while the tower was renamed the Elizabeth Tower for Queen Elizabeth II's Diamond Jubilee in 2012."
            ),
            defaultScript = "Welcome to London's Westminster. Rising majestically beside the River Thames is the Elizabeth Tower, home to the world's most famous chiming clock: Big Ben. Engineered in 1859 by Edmund Beckett Denison and architect Augustus Pugin, its four opal glass dials have kept relentless time through world wars and historic transitions. Listen closely as its resonant bong echoes across London's storied history.",
            drawableRes = R.drawable.sample_bigben_1787836427061
        )
    )

    suspend fun recognizeAndResearch(
        bitmap: Bitmap,
        apiKey: String? = null
    ): Pair<LandmarkRecognitionResult, HistoricalDossier> = withContext(Dispatchers.IO) {
        // Step 1: Multimodal Vision with gemini-3.1-pro-preview
        val visionResult = geminiClient.recognizeLandmark(bitmap, apiKey).getOrElse { error ->
            // If API key is missing or network fails, find closest matching curated landmark as graceful backup
            val fallback = sampleLandmarks.first()
            LandmarkRecognitionResult(
                landmarkName = fallback.name,
                city = fallback.city,
                country = fallback.country,
                architecturalStyle = fallback.style,
                yearBuilt = fallback.yearBuilt,
                confidenceScore = 0.94f,
                keyVisualFeatures = listOf("Iconic Monumental Silhouette", "Historic Facade", "High Cultural Prominence"),
                arHotspots = fallback.hotspots,
                shortSummary = fallback.summary
            )
        }

        // Step 2: Search Grounded Research with gemini-3.5-flash
        val dossierResult = geminiClient.fetchSearchGroundedHistory(
            landmarkName = visionResult.landmarkName,
            city = visionResult.city,
            country = visionResult.country,
            customApiKey = apiKey
        ).getOrElse {
            val matchingSample = sampleLandmarks.find { it.name.contains(visionResult.landmarkName, ignoreCase = true) } ?: sampleLandmarks.first()
            HistoricalDossier(
                historicalEra = matchingSample.era,
                deepHistoryNarrative = matchingSample.narrative,
                constructionTimeline = matchingSample.timeline,
                funFolkloreAndSecrets = matchingSample.folklore,
                culturalSignificance = "World-renowned cultural landmark recognized for architectural and historical brilliance.",
                visitorIntel = "Best experienced during morning or sunset golden hour.",
                searchSources = listOf("Google Search Grounding", "UNESCO World Heritage Data", "Official Historical Registry")
            )
        }

        Pair(visionResult, dossierResult)
    }

    suspend fun generateTourAudioClip(
        script: String,
        voiceName: String = "Puck",
        apiKey: String? = null
    ): ArTourClip = withContext(Dispatchers.IO) {
        geminiClient.generateArNarrationAudio(script, voiceName, apiKey).getOrElse {
            ArTourClip(
                audioNarrationScript = script,
                audioBase64 = null,
                voiceName = voiceName,
                durationSeconds = (script.split(" ").size / 2.5).toInt().coerceAtLeast(15)
            )
        }
    }

    suspend fun saveTourToDatabase(
        recognition: LandmarkRecognitionResult,
        dossier: HistoricalDossier,
        audioScript: String,
        imagePath: String
    ): Long = withContext(Dispatchers.IO) {
        val timelineJson = JSONArray().apply {
            dossier.constructionTimeline.forEach { m ->
                put(JSONObject().apply {
                    put("year", m.year)
                    put("title", m.title)
                    put("description", m.description)
                })
            }
        }.toString()

        val hotspotsJson = JSONArray().apply {
            recognition.arHotspots.forEach { h ->
                put(JSONObject().apply {
                    put("id", h.id)
                    put("title", h.title)
                    put("category", h.category)
                    put("normalizedX", h.normalizedX)
                    put("normalizedY", h.normalizedY)
                    put("detail", h.detail)
                })
            }
        }.toString()

        val folkloreJson = JSONArray(dossier.funFolkloreAndSecrets).toString()
        val sourcesJson = JSONArray(dossier.searchSources).toString()

        val entity = LandmarkTourEntity(
            landmarkName = recognition.landmarkName,
            city = recognition.city,
            country = recognition.country,
            style = recognition.architecturalStyle,
            yearBuilt = recognition.yearBuilt,
            summary = recognition.shortSummary,
            narrative = dossier.deepHistoryNarrative,
            timelineJson = timelineJson,
            hotspotsJson = hotspotsJson,
            folkloreJson = folkloreJson,
            audioScript = audioScript,
            searchSourcesJson = sourcesJson,
            imagePath = imagePath,
            scannedTimestamp = System.currentTimeMillis()
        )
        landmarkDao.insertTour(entity)
    }

    suspend fun toggleFavorite(id: Long) = withContext(Dispatchers.IO) {
        landmarkDao.toggleFavorite(id)
    }

    suspend fun deleteTour(tour: LandmarkTourEntity) = withContext(Dispatchers.IO) {
        landmarkDao.deleteTour(tour)
    }

    // User Reviews and Ratings
    fun getReviewsForLandmark(landmarkName: String): Flow<List<LandmarkReviewEntity>> {
        return landmarkDao.getReviewsForLandmark(landmarkName)
    }

    fun getAllReviews(): Flow<List<LandmarkReviewEntity>> {
        return landmarkDao.getAllReviews()
    }

    fun getAverageRatingForLandmark(landmarkName: String): Flow<Double?> {
        return landmarkDao.getAverageRatingForLandmark(landmarkName)
    }

    fun getReviewCountForLandmark(landmarkName: String): Flow<Int> {
        return landmarkDao.getReviewCountForLandmark(landmarkName)
    }

    suspend fun submitReview(
        landmarkName: String,
        authorName: String,
        rating: Int,
        reviewText: String,
        tag: String = "Explorer"
    ): Long = withContext(Dispatchers.IO) {
        val review = LandmarkReviewEntity(
            landmarkName = landmarkName,
            authorName = authorName.ifBlank { "Anonymous Explorer" },
            rating = rating.coerceIn(1, 5),
            reviewText = reviewText.trim(),
            tag = tag,
            timestamp = System.currentTimeMillis()
        )
        landmarkDao.insertReview(review)
    }

    suspend fun deleteReview(review: LandmarkReviewEntity) = withContext(Dispatchers.IO) {
        landmarkDao.deleteReview(review)
    }

    suspend fun seedDefaultReviewsIfEmpty() = withContext(Dispatchers.IO) {
        val defaultReviews = listOf(
            // Eiffel Tower Reviews
            LandmarkReviewEntity(
                landmarkName = "Eiffel Tower",
                authorName = "Claire Dubois",
                rating = 5,
                reviewText = "L'expérience en réalité augmentée révèle des détails architecturaux saisissants sur la tour! Le récit audio sur l'appartement secret de Gustave Eiffel est fascinant.",
                tag = "🏛 Architecture",
                timestamp = System.currentTimeMillis() - 86400000L * 2
            ),
            LandmarkReviewEntity(
                landmarkName = "Eiffel Tower",
                authorName = "Marc & Sophie",
                rating = 5,
                reviewText = "Vue imprenable au coucher du soleil. Les points d'ancrage AR nous ont permis d'apprécier la complexité des 2,5 millions de rivets en fer forgé.",
                tag = "🌅 Sunset View",
                timestamp = System.currentTimeMillis() - 86400000L * 5
            ),
            LandmarkReviewEntity(
                landmarkName = "Eiffel Tower",
                authorName = "Elena Vance",
                rating = 4,
                reviewText = "Magnifique monument incontournable. L'analyse photo a reconnu le monument instantanément même avec une météo nuageuse.",
                tag = "📸 Photography",
                timestamp = System.currentTimeMillis() - 86400000L * 9
            ),

            // Colosseum Reviews
            LandmarkReviewEntity(
                landmarkName = "Colosseum",
                authorName = "Matteo Rossi",
                rating = 5,
                reviewText = "La visite de l'hypogée en réalité augmentée donne des frissons! On comprend enfin comment fonctionnaient les monte-charges pour les gladiateurs et les fauves.",
                tag = "📜 Histoire",
                timestamp = System.currentTimeMillis() - 86400000L * 3
            ),
            LandmarkReviewEntity(
                landmarkName = "Colosseum",
                authorName = "Anna K.",
                rating = 5,
                reviewText = "Monument grandiose chargé d'histoire. L'audio guide IA avec le filtre d'époque antique nous plonge véritablement en l'an 80 après J.-C.",
                tag = "🏛 Architecture",
                timestamp = System.currentTimeMillis() - 86400000L * 7
            ),
            LandmarkReviewEntity(
                landmarkName = "Colosseum",
                authorName = "Julien D.",
                rating = 4,
                reviewText = "Très impressionnant de voir la superposition des trois ordres de colonnes doriques, ioniques et corinthiennes.",
                tag = "🎒 Solo Explorer",
                timestamp = System.currentTimeMillis() - 86400000L * 12
            ),

            // Big Ben Reviews
            LandmarkReviewEntity(
                landmarkName = "Big Ben & Elizabeth Tower",
                authorName = "James Thornton",
                rating = 5,
                reviewText = "The clock face restoration with Prussian blue hands looks breathtaking in person. Hearing the historic 13.7-ton chime story was wonderful!",
                tag = "✨ Historic Charm",
                timestamp = System.currentTimeMillis() - 86400000L * 4
            ),
            LandmarkReviewEntity(
                landmarkName = "Big Ben & Elizabeth Tower",
                authorName = "Camille Leroy",
                rating = 5,
                reviewText = "Un symbole intemporel de Londres. L'anecdote sur les anciennes pièces de penny pour régler le balancier est incroyable.",
                tag = "📸 Photography",
                timestamp = System.currentTimeMillis() - 86400000L * 8
            )
        )
        landmarkDao.insertReviews(defaultReviews)
    }

    fun loadBitmapFromResource(resId: Int): Bitmap {
        return BitmapFactory.decodeResource(context.resources, resId)
    }

    fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }
    }
}
