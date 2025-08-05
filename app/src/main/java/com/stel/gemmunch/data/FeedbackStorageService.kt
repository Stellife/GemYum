package com.stel.gemmunch.data

import android.content.Context
import android.util.Log
import com.stel.gemmunch.data.models.MealAnalysisFeedback
import com.stel.gemmunch.utils.GsonProvider
import com.stel.gemmunch.utils.InstantTypeAdapter
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

private const val TAG = "FeedbackStorageService"
private const val FEEDBACK_DIR = "meal_feedback_rag"
private const val FEEDBACK_INDEX_FILE = "feedback_index.json"

/**
 * Service for storing meal analysis feedback documents for RAG library population.
 * Stores feedback as individual JSON files with an index for efficient retrieval.
 */
class FeedbackStorageService(
    private val context: Context,
    gsonProvider: GsonProvider
) {
    private val gson = gsonProvider.instance
    private val feedbackDir: File by lazy {
        File(context.filesDir, FEEDBACK_DIR).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    private val indexFile: File by lazy {
        File(feedbackDir, FEEDBACK_INDEX_FILE)
    }
    
    /**
     * Stores a feedback document and updates the index.
     * Returns the document ID if successful, null otherwise.
     */
    suspend fun storeFeedback(feedback: MealAnalysisFeedback): String? = withContext(Dispatchers.IO) {
        try {
            val documentId = generateDocumentId()
            val fileName = "feedback_$documentId.json"
            val feedbackFile = File(feedbackDir, fileName)
            
            // Write the feedback document
            val json = gson.toJson(feedback)
            feedbackFile.writeText(json)
            
            // Pretty print JSON for debugging
            val prettyJson = GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
                .create()
                .toJson(feedback)
            
            // Log the full JSON for debugging (split into chunks for Android log limit)
            Log.i(TAG, "Storing feedback document: $documentId")
            Log.i(TAG, "=== FULL JSON DOCUMENT ===")
            prettyJson.chunked(3000).forEach { chunk ->
                Log.i(TAG, chunk)
            }
            Log.i(TAG, "==========================")
            
            // Update the index
            updateIndex(documentId, feedback)
            
            Log.i(TAG, "Successfully stored feedback document: $documentId")
            return@withContext documentId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store feedback", e)
            return@withContext null
        }
    }
    
    /**
     * Retrieves a specific feedback document by ID.
     */
    suspend fun getFeedback(documentId: String): MealAnalysisFeedback? = withContext(Dispatchers.IO) {
        try {
            val feedbackFile = File(feedbackDir, "feedback_$documentId.json")
            if (!feedbackFile.exists()) return@withContext null
            
            val json = feedbackFile.readText()
            return@withContext gson.fromJson(json, MealAnalysisFeedback::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve feedback: $documentId", e)
            return@withContext null
        }
    }
    
    /**
     * Gets all feedback documents as a Flow for processing.
     */
    fun getAllFeedback(): Flow<List<FeedbackIndexEntry>> = flow {
        try {
            val index = loadIndex()
            emit(index)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load feedback index", e)
            emit(emptyList())
        }
    }
    
    /**
     * Gets feedback statistics for display.
     */
    suspend fun getFeedbackStats(): FeedbackStats = withContext(Dispatchers.IO) {
        try {
            val index = loadIndex()
            val totalDocuments = index.size
            val averageScore = if (index.isNotEmpty()) {
                index.mapNotNull { it.humanScore }.average()
            } else 0.0
            
            val errorCounts = index.flatMap { it.errorTypes }.groupingBy { it }.eachCount()
            val restaurantCount = index.count { it.isRestaurant }
            
            FeedbackStats(
                totalDocuments = totalDocuments,
                averageScore = averageScore,
                errorTypeCounts = errorCounts,
                restaurantMealCount = restaurantCount,
                homeCoockedMealCount = totalDocuments - restaurantCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate feedback stats", e)
            FeedbackStats()
        }
    }
    
    /**
     * Exports all feedback documents to a single JSON file for analysis.
     */
    suspend fun exportAllFeedback(): File? = withContext(Dispatchers.IO) {
        try {
            val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                .replace(':', '-').replace('.', '-')
            val exportFile = File(context.getExternalFilesDir(null), "gemmunch_feedback_export_$timestamp.json")
            
            val allFeedback = mutableListOf<MealAnalysisFeedback>()
            val index = loadIndex()
            
            index.forEach { entry ->
                getFeedback(entry.documentId)?.let { feedback ->
                    allFeedback.add(feedback)
                }
            }
            
            val exportData = mapOf(
                "exportDate" to Instant.now().toString(),
                "documentCount" to allFeedback.size,
                "documents" to allFeedback
            )
            
            exportFile.writeText(gson.toJson(exportData))
            Log.i(TAG, "Exported ${allFeedback.size} feedback documents to: ${exportFile.path}")
            return@withContext exportFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export feedback", e)
            return@withContext null
        }
    }
    
    private fun generateDocumentId(): String {
        return "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
    }
    
    private suspend fun updateIndex(documentId: String, feedback: MealAnalysisFeedback) {
        try {
            val currentIndex = loadIndex().toMutableList()
            
            val indexEntry = FeedbackIndexEntry(
                documentId = documentId,
                timestamp = feedback.insightGeneratedDate,
                modelName = feedback.modelDetails.modelName,
                humanScore = feedback.humanScore,
                errorTypes = feedback.humanReportedErrors.map { it.name },
                isRestaurant = feedback.restaurantMealDetails?.isRestaurant ?: false,
                restaurantName = feedback.restaurantMealDetails?.restaurantName,
                totalCalories = feedback.aiResponsePerItem.sumOf { it.nutritionalInfo.calories }
            )
            
            currentIndex.add(indexEntry)
            
            // Sort by timestamp descending (newest first)
            currentIndex.sortByDescending { it.timestamp }
            
            // Save updated index
            indexFile.writeText(gson.toJson(currentIndex))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update index", e)
        }
    }
    
    private fun loadIndex(): List<FeedbackIndexEntry> {
        return try {
            if (!indexFile.exists()) return emptyList()
            
            val json = indexFile.readText()
            gson.fromJson(json, Array<FeedbackIndexEntry>::class.java).toList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load index", e)
            emptyList()
        }
    }
}

/**
 * Index entry for quick lookups without loading full documents.
 */
data class FeedbackIndexEntry(
    val documentId: String,
    val timestamp: Instant,
    val modelName: String,
    val humanScore: Int?,
    val errorTypes: List<String>,
    val isRestaurant: Boolean,
    val restaurantName: String?,
    val totalCalories: Int
)

/**
 * Statistics about the feedback collection.
 */
data class FeedbackStats(
    val totalDocuments: Int = 0,
    val averageScore: Double = 0.0,
    val errorTypeCounts: Map<String, Int> = emptyMap(),
    val restaurantMealCount: Int = 0,
    val homeCoockedMealCount: Int = 0
)