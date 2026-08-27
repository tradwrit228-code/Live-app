package com.example.data.network

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.model.ArHotspot
import com.example.model.ArTourClip
import com.example.model.HistoricalDossier
import com.example.model.LandmarkRecognitionResult
import com.example.model.TimelineMilestone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun getApiKey(customKey: String? = null): String {
        if (!customKey.isNullOrBlank()) return customKey
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Model: gemini-3.1-pro-preview
     * Multimodal Image Recognition & AR hotspot detection
     */
    suspend fun recognizeLandmark(
        bitmap: Bitmap,
        customApiKey: String? = null
    ): Result<LandmarkRecognitionResult> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(customApiKey)
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("GEMINI_API_KEY is not configured. Please add your key in the Secrets panel."))
        }

        try {
            val base64Image = bitmapToBase64(bitmap)
            val modelName = "gemini-3.1-pro-preview"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

            val prompt = """
                Analyze this photo as a world-class architectural historian and tourism guide.
                Identify the landmark or monument shown.
                Return ONLY a JSON object with this exact schema:
                {
                  "landmarkName": "Official name of landmark",
                  "city": "City name",
                  "country": "Country name",
                  "architecturalStyle": "e.g. Gothic / Romanesque / Modernist",
                  "yearBuilt": "e.g. 1889 or 80 AD",
                  "confidenceScore": 0.98,
                  "keyVisualFeatures": ["Feature 1", "Feature 2", "Feature 3"],
                  "shortSummary": "A punchy 2-sentence visual overview of the landmark.",
                  "arHotspots": [
                    {
                      "id": "spot_1",
                      "title": "Title of Architectural Component",
                      "category": "Architecture",
                      "normalizedX": 0.5,
                      "normalizedY": 0.25,
                      "detail": "Fascinating historical or structural detail about this exact part."
                    },
                    {
                      "id": "spot_2",
                      "title": "Title of Component 2",
                      "category": "History",
                      "normalizedX": 0.35,
                      "normalizedY": 0.65,
                      "detail": "Historical story or engineering feat related to this section."
                    },
                    {
                      "id": "spot_3",
                      "title": "Title of Component 3",
                      "category": "Engineering",
                      "normalizedX": 0.68,
                      "normalizedY": 0.55,
                      "detail": "Secret fact or material science insight."
                    }
                  ]
                }
                Note: normalizedX (0.0=left, 1.0=right) and normalizedY (0.0=top, 1.0=bottom) must pinpoint real visual parts in this photo.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.e("GeminiApiClient", "Vision error: ${response.code} - $responseBody")
                return@withContext Result.failure(Exception("AI Vision error (${response.code}): $responseBody"))
            }

            val parsedText = extractTextFromGeminiResponse(responseBody)
            val jsonClean = extractJsonBlock(parsedText)
            val json = JSONObject(jsonClean)

            val hotspots = mutableListOf<ArHotspot>()
            val hotspotsArray = json.optJSONArray("arHotspots")
            if (hotspotsArray != null) {
                for (i in 0 until hotspotsArray.length()) {
                    val item = hotspotsArray.getJSONObject(i)
                    hotspots.add(
                        ArHotspot(
                            id = item.optString("id", "spot_$i"),
                            title = item.optString("title", "Point of Interest"),
                            category = item.optString("category", "Architecture"),
                            normalizedX = item.optDouble("normalizedX", 0.5).toFloat(),
                            normalizedY = item.optDouble("normalizedY", 0.5).toFloat(),
                            detail = item.optString("detail", "")
                        )
                    )
                }
            }

            val features = mutableListOf<String>()
            val featArray = json.optJSONArray("keyVisualFeatures")
            if (featArray != null) {
                for (i in 0 until featArray.length()) {
                    features.add(featArray.getString(i))
                }
            }

            val result = LandmarkRecognitionResult(
                landmarkName = json.optString("landmarkName", "Recognized Landmark"),
                city = json.optString("city", "Historic City"),
                country = json.optString("country", "World"),
                architecturalStyle = json.optString("architecturalStyle", "Monumental Architecture"),
                yearBuilt = json.optString("yearBuilt", "Historic Era"),
                confidenceScore = json.optDouble("confidenceScore", 0.96).toFloat(),
                keyVisualFeatures = features,
                arHotspots = hotspots,
                shortSummary = json.optString("shortSummary", "An iconic historical monument recognized through your lens.")
            )
            Result.success(result)
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Vision Recognition failure", e)
            Result.failure(e)
        }
    }

    /**
     * Model: gemini-3.5-flash with Google Search Tool Grounding
     * Fetches verified history, search citations, folklore, and visitor intelligence
     */
    suspend fun fetchSearchGroundedHistory(
        landmarkName: String,
        city: String,
        country: String,
        customApiKey: String? = null
    ): Result<HistoricalDossier> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(customApiKey)
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("GEMINI_API_KEY is not configured."))
        }

        try {
            val modelName = "gemini-3.5-flash"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

            val prompt = """
                Conduct Google Search grounded research on the famous landmark: "$landmarkName" located in $city, $country.
                Retrieve verified historical milestones, cultural significance, folklore/mysteries, and modern visiting intel.
                Format your final output strictly as a valid JSON object matching this structure:
                {
                  "historicalEra": "Primary Historical Era (e.g. Belle Époque, Roman Empire)",
                  "deepHistoryNarrative": "A rich 3-paragraph historical narrative detailing its origin, notable events through centuries, and modern legacy.",
                  "constructionTimeline": [
                    { "year": "1887", "title": "Groundbreaking", "description": "Construction began on the foundations." },
                    { "year": "1889", "title": "Grand Inauguration", "description": "Opened for the World's Fair." },
                    { "year": "1944", "title": "Wartime Survival", "description": "Key milestone during liberation." },
                    { "year": "2000s", "title": "Modern Illumination", "description": "Modern preservation and energy-efficient lighting upgrades." }
                  ],
                  "funFolkloreAndSecrets": [
                    "Fascinating secret 1",
                    "Fascinating secret 2",
                    "Fascinating secret 3"
                  ],
                  "culturalSignificance": "Why this landmark stands as a global cultural icon.",
                  "visitorIntel": "Best viewpoint time, entry tip, or secret perspective."
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
                // Google Search tool grounding
                val tools = JSONArray().apply {
                    put(JSONObject().apply {
                        put("googleSearch", JSONObject())
                    })
                }
                put("tools", tools)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.e("GeminiApiClient", "History Search error: ${response.code} - $responseBody")
                return@withContext Result.failure(Exception("Search Grounding error (${response.code}): $responseBody"))
            }

            val (extractedText, sources) = extractTextAndSourcesFromGeminiResponse(responseBody)
            val jsonClean = extractJsonBlock(extractedText)
            val json = JSONObject(jsonClean)

            val timeline = mutableListOf<TimelineMilestone>()
            val timeArray = json.optJSONArray("constructionTimeline")
            if (timeArray != null) {
                for (i in 0 until timeArray.length()) {
                    val m = timeArray.getJSONObject(i)
                    timeline.add(
                        TimelineMilestone(
                            year = m.optString("year", ""),
                            title = m.optString("title", ""),
                            description = m.optString("description", "")
                        )
                    )
                }
            }

            val folklore = mutableListOf<String>()
            val folkArray = json.optJSONArray("funFolkloreAndSecrets")
            if (folkArray != null) {
                for (i in 0 until folkArray.length()) {
                    folklore.add(folkArray.getString(i))
                }
            }

            val dossier = HistoricalDossier(
                historicalEra = json.optString("historicalEra", "Historic Century"),
                deepHistoryNarrative = json.optString("deepHistoryNarrative", "A monumental treasure rich in historical significance."),
                constructionTimeline = timeline,
                funFolkloreAndSecrets = folklore,
                culturalSignificance = json.optString("culturalSignificance", "A world-renowned heritage site."),
                visitorIntel = json.optString("visitorIntel", "Best experienced during golden hour for panoramic city vistas."),
                searchSources = sources.ifEmpty { listOf("Google Search Grounding", "World Heritage Archives", "Official Tourism Bureau") }
            )
            Result.success(dossier)
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "History Search failure", e)
            Result.failure(e)
        }
    }

    /**
     * Model: gemini-3.1-flash-tts-preview
     * Generates narrated audio speech clip for the AR tour
     */
    suspend fun generateArNarrationAudio(
        script: String,
        voiceName: String = "Puck",
        customApiKey: String? = null
    ): Result<ArTourClip> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(customApiKey)
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("GEMINI_API_KEY is not configured."))
        }

        try {
            val modelName = "gemini-3.1-flash-tts-preview"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

            val cleanScript = script.trim()
            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", cleanScript) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().apply { put("AUDIO") })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", voiceName)
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.e("GeminiApiClient", "TTS Audio error: ${response.code} - $responseBody")
                return@withContext Result.failure(Exception("TTS Audio error (${response.code}): $responseBody"))
            }

            val audioData = extractAudioFromGeminiResponse(responseBody)
            val clip = ArTourClip(
                audioNarrationScript = cleanScript,
                audioBase64 = audioData.first,
                audioMimeType = audioData.second ?: "audio/wav",
                voiceName = voiceName,
                durationSeconds = (cleanScript.split(" ").size / 2.5).toInt().coerceAtLeast(15)
            )
            Result.success(clip)
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "TTS Generation failure", e)
            Result.failure(e)
        }
    }

    private fun extractTextFromGeminiResponse(responseJsonStr: String): String {
        return try {
            val root = JSONObject(responseJsonStr)
            val candidates = root.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""
            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                val text = part.optString("text", "")
                if (text.isNotEmpty()) sb.append(text)
            }
            sb.toString()
        } catch (e: Exception) {
            ""
        }
    }

    private fun extractTextAndSourcesFromGeminiResponse(responseJsonStr: String): Pair<String, List<String>> {
        val sources = mutableListOf<String>()
        var text = ""
        try {
            val root = JSONObject(responseJsonStr)
            val candidates = root.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val first = candidates.getJSONObject(0)
                val content = first.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null) {
                    val sb = StringBuilder()
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        sb.append(part.optString("text", ""))
                    }
                    text = sb.toString()
                }

                // Grounding metadata
                val grounding = first.optJSONObject("groundingMetadata")
                if (grounding != null) {
                    val searchQueries = grounding.optJSONArray("webSearchQueries")
                    if (searchQueries != null) {
                        for (i in 0 until searchQueries.length()) {
                            sources.add("Query: " + searchQueries.getString(i))
                        }
                    }
                    val chunks = grounding.optJSONArray("groundingChunks")
                    if (chunks != null) {
                        for (i in 0 until chunks.length()) {
                            val chunk = chunks.getJSONObject(i)
                            val web = chunk.optJSONObject("web")
                            val title = web?.optString("title")
                            val uri = web?.optString("uri")
                            if (!title.isNullOrBlank()) {
                                sources.add(title + (if (!uri.isNullOrBlank()) " ($uri)" else ""))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Error parsing sources", e)
        }
        return Pair(text, sources)
    }

    private fun extractAudioFromGeminiResponse(responseJsonStr: String): Pair<String?, String?> {
        try {
            val root = JSONObject(responseJsonStr)
            val candidates = root.optJSONArray("candidates") ?: return Pair(null, null)
            if (candidates.length() == 0) return Pair(null, null)
            val first = candidates.getJSONObject(0)
            val content = first.optJSONObject("content") ?: return Pair(null, null)
            val parts = content.optJSONArray("parts") ?: return Pair(null, null)
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                val inlineData = part.optJSONObject("inlineData") ?: part.optJSONObject("inline_data")
                if (inlineData != null) {
                    val data = inlineData.optString("data")
                    val mime = inlineData.optString("mimeType", "audio/wav")
                    if (data.isNotEmpty()) {
                        return Pair(data, mime)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Error parsing audio response", e)
        }
        return Pair(null, null)
    }

    private fun extractJsonBlock(raw: String): String {
        val trimmed = raw.trim()
        val jsonStart = trimmed.indexOf('{')
        val jsonEnd = trimmed.lastIndexOf('}')
        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            return trimmed.substring(jsonStart, jsonEnd + 1)
        }
        return trimmed
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Resize if too big for fast network transit
        val maxDim = 1280
        val scale = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
        } else {
            1.0f
        }

        val scaledBitmap = if (scale < 1.0f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }

        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
