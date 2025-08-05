package com.stel.gemmunch.data

import android.content.Context
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.metadata.Metadata as HealthMetadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import com.stel.gemmunch.agent.AnalyzedFoodItem
import com.stel.gemmunch.agent.MealAnalysis
import java.time.Instant
import java.time.ZoneOffset

private const val TAG = "HealthConnectManager"

/**
 * Manages Health Connect integration for nutrition data only.
 * Simplified implementation focusing on meal tracking functionality.
 */
class HealthConnectManager(private val context: Context) {
    
    private val healthConnectClient: HealthConnectClient? by lazy {
        if (isHealthConnectAvailable()) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }
    
    companion object {
        val NUTRITION_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(NutritionRecord::class),
            HealthPermission.getWritePermission(NutritionRecord::class)
        )
    }
    
    /**
     * Checks if Health Connect is available on the device.
     */
    fun isHealthConnectAvailable(): Boolean {
        val status = HealthConnectClient.getSdkStatus(context)
        return status == HealthConnectClient.SDK_AVAILABLE
    }
    
    /**
     * Gets the availability status with details about why it might not be available.
     */
    fun getAvailabilityStatus(): HealthConnectAvailability {
        return when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_UNAVAILABLE -> HealthConnectAvailability.NOT_INSTALLED
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UPDATE_REQUIRED
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            else -> HealthConnectAvailability.NOT_SUPPORTED
        }
    }
    
    /**
     * Checks if nutrition permissions have been granted.
     */
    suspend fun hasNutritionPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            granted.containsAll(NUTRITION_PERMISSIONS)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permissions", e)
            false
        }
    }
    
    /**
     * Creates an ActivityResultContract for requesting Health Connect permissions.
     */
    fun createPermissionRequestContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }
    
    /**
     * Writes nutrition records to Health Connect.
     * @param items The analyzed food items to write
     * @param mealDateTime The timestamp for the meal
     * @param mealName Optional name for the meal (e.g., "Breakfast", "Lunch")
     * @return true if write was successful, false otherwise
     */
    suspend fun writeNutritionRecords(
        items: List<AnalyzedFoodItem>,
        mealDateTime: Instant,
        mealName: String? = null
    ): Boolean {
        val client = healthConnectClient ?: run {
            Log.e(TAG, "Health Connect client not available")
            return false
        }
        
        return try {
            // Health Connect requires endTime to be after startTime
            // We'll add 1 minute duration for the meal
            val mealEndTime = mealDateTime.plusSeconds(60)
            
            // Get zone offset safely
            val zoneOffset = try {
                ZoneOffset.systemDefault().rules.getOffset(mealDateTime)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get zone offset, using UTC", e)
                ZoneOffset.UTC
            }
            
            Log.d(TAG, "Writing nutrition record: startTime=$mealDateTime, endTime=$mealEndTime")
            Log.d(TAG, "Zone offset: $zoneOffset")
            Log.d(TAG, "Start millis: ${mealDateTime.toEpochMilli()}, End millis: ${mealEndTime.toEpochMilli()}")
            
            val records = items.map { item ->
                // Include quantity in name if more than 1
                val displayName = when {
                    item.quantity <= 1 -> item.foodName
                    item.quantity % 1 == 0.0 -> { // Whole number
                        val count = item.quantity.toInt()
                        "$count ${pluralizeFoodName(item.foodName, count)}"
                    }
                    else -> { // Fractional quantity
                        "${item.quantity} ${item.foodName}"
                    }
                }
                
                Log.d(TAG, "Writing nutrition record for: $displayName (quantity: ${item.quantity})")
                
                NutritionRecord(
                    metadata = HealthMetadata.manualEntry(),
                    startTime = mealDateTime,
                    startZoneOffset = zoneOffset,
                    endTime = mealEndTime,
                    endZoneOffset = zoneOffset,
                    name = mealName ?: displayName,
                    mealType = determineMealType(mealDateTime),
                    energy = Energy.kilocalories(item.calories.toDouble()),
                    protein = item.protein?.let { Mass.grams(it) },
                    totalFat = item.totalFat?.let { Mass.grams(it) },
                    saturatedFat = item.saturatedFat?.let { Mass.grams(it) },
                    totalCarbohydrate = item.totalCarbs?.let { Mass.grams(it) },
                    dietaryFiber = item.dietaryFiber?.let { Mass.grams(it) },
                    sugar = item.sugars?.let { Mass.grams(it) },
                    sodium = item.sodium?.let { Mass.milligrams(it) },
                    cholesterol = item.cholesterol?.let { Mass.milligrams(it) }
                )
            }
            
            client.insertRecords(records)
            
            Log.i(TAG, "Successfully wrote ${records.size} nutrition records to Health Connect")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write nutrition records", e)
            false
        }
    }
    
    /**
     * Simple pluralization helper for food names
     */
    private fun pluralizeFoodName(foodName: String, count: Int): String {
        // Handle common food pluralization patterns
        return when {
            count == 1 -> foodName
            foodName.endsWith("s") || foodName.endsWith("x") || foodName.endsWith("ch") -> "${foodName}es"
            foodName.endsWith("y") && !foodName.endsWith("ay") && !foodName.endsWith("ey") && !foodName.endsWith("oy") -> {
                "${foodName.dropLast(1)}ies"
            }
            foodName == "taco" -> "tacos"
            foodName == "burrito" -> "burritos"
            foodName == "banana" -> "bananas"
            foodName == "apple" -> "apples"
            foodName == "sandwich" -> "sandwiches"
            foodName == "slice" -> "slices"
            foodName == "piece" -> "pieces"
            else -> "${foodName}s"
        }
    }
    
    /**
     * Determines the meal type based on time of day.
     */
    private fun determineMealType(mealTime: Instant): Int {
        val hour = mealTime.atZone(ZoneOffset.systemDefault()).hour
        return when (hour) {
            in 5..10 -> MealType.MEAL_TYPE_BREAKFAST
            in 11..14 -> MealType.MEAL_TYPE_LUNCH
            in 15..17 -> MealType.MEAL_TYPE_SNACK
            in 18..21 -> MealType.MEAL_TYPE_DINNER
            else -> MealType.MEAL_TYPE_SNACK
        }
    }
}

/**
 * Represents the availability status of Health Connect.
 */
enum class HealthConnectAvailability {
    AVAILABLE,
    NOT_INSTALLED,
    UPDATE_REQUIRED,
    NOT_SUPPORTED
}