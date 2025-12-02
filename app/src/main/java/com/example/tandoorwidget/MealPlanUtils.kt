package com.example.tandoorwidget

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility object for safely handling meal plan data, especially when recipe is null (placeholder entries).
 */
object MealPlanUtils {
    private const val TAG = "MealPlanUtils"

    /**
     * Get the display name for a meal plan entry.
     * If recipe is null (placeholder entry), returns the title.
     * Otherwise returns the recipe name.
     *
     * @param recipe The recipe object (can be null for placeholders)
     * @param title The meal plan title (fallback when recipe is null)
     * @return The display name to show to the user
     */
    fun getDisplayName(recipe: Recipe?, title: String): String {
        return if (recipe != null) {
            recipe.name
        } else {
            Log.d(TAG, "Placeholder entry detected (recipe == null), using title: '$title'")
            title
        }
    }

    /**
     * Get the recipe URL for a meal plan entry.
     * Returns null if recipe is null (placeholder entry) or if recipe ID is invalid.
     *
     * @param recipe The recipe object (can be null for placeholders)
     * @return The recipe ID for URL construction, or null if not available
     */
    fun getRecipeUrl(recipe: Recipe?): Int? {
        return if (recipe != null && recipe.id > 0) {
            recipe.id
        } else {
            if (recipe == null) {
                Log.d(TAG, "Placeholder entry (recipe == null), no URL available")
            }
            null
        }
    }

    /**
     * Safely parse a date string in ISO 8601 format.
     * Extracts the date portion (YYYY-MM-DD) from timestamps like "2025-12-01T22:24:35.522000+01:00"
     *
     * @param rawDate The raw date string from API
     * @return The parsed date string in YYYY-MM-DD format, or the original string if parsing fails
     */
    fun safeParseDate(rawDate: String): String {
        return try {
            if (rawDate.length >= 10) {
                rawDate.substring(0, 10)
            } else {
                Log.w(TAG, "Date string too short: '$rawDate'")
                rawDate
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse date: '$rawDate'", e)
            rawDate
        }
    }

    /**
     * Format a date string for display.
     * Converts "YYYY-MM-DD" to "EEE dd/MM" format.
     *
     * @param dateString Date in YYYY-MM-DD format
     * @param inputFormat SimpleDateFormat for parsing
     * @param outputFormat SimpleDateFormat for formatting
     * @return Formatted date string, or original string if parsing fails
     */
    fun formatDateForDisplay(
        dateString: String,
        inputFormat: SimpleDateFormat,
        outputFormat: SimpleDateFormat
    ): String {
        return try {
            val parsedDate = inputFormat.parse(dateString)
            if (parsedDate != null) {
                outputFormat.format(parsedDate)
            } else {
                Log.e(TAG, "Failed to parse date: $dateString")
                dateString
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception parsing date: $dateString", e)
            dateString
        }
    }
}
