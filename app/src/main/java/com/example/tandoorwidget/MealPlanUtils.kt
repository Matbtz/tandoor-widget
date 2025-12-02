package com.example.tandoorwidget

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Utility functions for handling meal plans, especially cases where recipe is null (placeholders).
 */
object MealPlanUtils {
    private const val TAG = "MealPlanUtils"
    
    // Thread-local date format for safe concurrent usage
    private val dateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }
    }
    
    /**
     * Get display name for a meal plan entry.
     * Returns recipe name if available, otherwise falls back to title, or "Untitled" as last resort.
     */
    fun getDisplayName(recipe: Recipe?, title: String?): String {
        return when {
            recipe != null && recipe.name.isNotBlank() -> recipe.name
            !title.isNullOrBlank() -> title
            else -> {
                Log.w(TAG, "MealPlan has no recipe name or title, using 'Untitled'")
                "Untitled"
            }
        }
    }
    
    /**
     * Get recipe URL for a meal plan entry.
     * Returns a properly formatted URL if recipe exists, null otherwise.
     */
    fun getRecipeUrl(recipe: Recipe?, baseUrl: String): String? {
        if (recipe == null) {
            Log.d(TAG, "Recipe is null, no URL to generate")
            return null
        }
        
        if (recipe.id <= 0) {
            Log.w(TAG, "Recipe has invalid ID: ${recipe.id}, cannot generate URL")
            return null
        }
        
        return try {
            "$baseUrl/recipe/${recipe.id}/"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate recipe URL", e)
            null
        }
    }
    
    /**
     * Safely parse a date string and return YYYY-MM-DD format.
     * If parsing fails, returns the first 10 characters or the original string as fallback.
     */
    fun safeParseDate(rawDate: String): String {
        return try {
            // Try to extract just the date part (first 10 characters for YYYY-MM-DD)
            if (rawDate.length >= 10) {
                val datePart = rawDate.substring(0, 10)
                // Validate it's a proper date format using the thread-local date formatter
                dateFormat.get()!!.parse(datePart) // Will throw if invalid
                datePart
            } else {
                Log.w(TAG, "Date string too short: '$rawDate', using as-is")
                rawDate
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse date: '$rawDate'", e)
            // Return first 10 chars if available, otherwise the original string
            if (rawDate.length >= 10) rawDate.substring(0, 10) else rawDate
        }
    }
}
