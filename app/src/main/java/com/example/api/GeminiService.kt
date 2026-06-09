package com.example.api

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.BuildConfig
import com.example.model.MediaItem
import com.example.model.MovieCatalog
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Query Gemini REST API with local context injection for structured Netflix clone matching
    suspend fun getRecommendations(userPrompt: String): RecommendationResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API Key is not set or placeholder.")
            return@withContext getFallbackRecommendations(userPrompt, "API Key is missing or placeholder. Please configure GEMINI_API_KEY in the Secrets Panel.")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val catalogStr = MovieCatalog.items.joinToString("\n") { 
            "- Title: ${it.title} (${it.type}, ${it.genre}): ${it.description}" 
        }

        val systemInstruction = """
            You are NetStream's Cinema Concierge, an AI recommendation engine for the NetStream application (a premium Netflix-clone streaming high-definition content). 
            
            The user will give a mood or description of what they want to watch. 
            You must reply in this exact schema format with a single JSON object. Do not include markdown codeblocks (no ```json code blocks or extra text). Output raw JSON only.
            
            JSON schema:
            {
               "analysis": "A personalized greeting and custom 2-sentence cinematic review of why these movies fit the user's mood perfectly.",
               "recommendedTitles": ["TITLE_1", "TITLE_2", "TITLE_3"]
            }
            
            Make sure "recommendedTitles" matches the exact TITLE casing from the available catalog below:
            $catalogStr
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("contents", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", userPrompt)
                        })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemInstruction)
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.4)
            })
        }

        val body = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errCode = response.code
                    val errMsg = response.body?.string() ?: ""
                    Log.e(TAG, "Unsuccessful Gemini response: $errCode - $errMsg")
                    return@withContext getFallbackRecommendations(userPrompt, "Network response unsuccessful ($errCode).")
                }

                val responseBody = response.body?.string()
                if (responseBody.isNullOrEmpty()) {
                    return@withContext getFallbackRecommendations(userPrompt, "Empty response body.")
                }

                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext getFallbackRecommendations(userPrompt, "No candidates returned from generative model.")
                }

                val text = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                // Parse Structured Output
                val parsedResult = JSONObject(text.trim())
                val analysis = parsedResult.optString("analysis", "Based on your mood, we recommend these outstanding title releases.")
                val recommendedTitlesJson = parsedResult.optJSONArray("recommendedTitles")
                val matchedItems = mutableListOf<MediaItem>()

                if (recommendedTitlesJson != null) {
                    for (i in 0 until recommendedTitlesJson.length()) {
                        val title = recommendedTitlesJson.getString(i)
                        val match = MovieCatalog.items.find { it.title.equals(title, ignoreCase = true) }
                        if (match != null) matchedItems.add(match)
                    }
                }

                if (matchedItems.isEmpty()) {
                    matchedItems.addAll(MovieCatalog.items.shuffled().take(3))
                }

                RecommendationResult(
                    analysis = analysis,
                    recItems = matchedItems,
                    usingMockFallback = false
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception performing Gemini call: ${e.message}", e)
            getFallbackRecommendations(userPrompt, "Failure: ${e.localizedMessage}")
        }
    }

    private fun getFallbackRecommendations(prompt: String, errorNote: String): RecommendationResult {
        // Return highly intuitive matching fallback recommendations based on simple keyword parsing
        val matched = mutableListOf<MediaItem>()
        val query = prompt.lowercase()

        when {
            query.contains("action") || query.contains("cyber") || query.contains("neon") -> {
                matched.addAll(MovieCatalog.items.filter { it.genre.contains("Cyberpunk") || it.genre.contains("Action") })
            }
            query.contains("sci-fi") || query.contains("space") || query.contains("cosmos") || query.contains("alien") -> {
                matched.addAll(MovieCatalog.items.filter { it.genre.contains("Sci-Fi") || it.description.contains("space", ignoreCase = true) })
            }
            query.contains("sport") || query.contains("race") || query.contains("car") || query.contains("speed") -> {
                matched.addAll(MovieCatalog.items.filter { it.genre.contains("Sport") })
            }
            query.contains("anime") || query.contains("japan") || query.contains("samurai") -> {
                matched.addAll(MovieCatalog.items.filter { it.genre.contains("Anime") })
            }
            else -> {
                matched.addAll(MovieCatalog.items.shuffled().take(3))
            }
        }

        if (matched.isEmpty()) {
            matched.addAll(MovieCatalog.items.shuffled().take(3))
        }

        return RecommendationResult(
            analysis = "Our smart local cinema engine suggests these titles based on keywords in your request. [$errorNote]",
            recItems = matched,
            usingMockFallback = true
        )
    }
}

data class RecommendationResult(
    val analysis: String,
    val recItems: List<MediaItem>,
    val usingMockFallback: Boolean
)
