package com.example.tandoorwidget

import org.junit.Test
import org.junit.Assert.*
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Unit tests for MealPlanUtils
 */
class MealPlanUtilsTest {

    @Test
    fun getDisplayName_withValidRecipe_returnsRecipeName() {
        // Given
        val recipe = Recipe(id = 1, name = "Spaghetti Bolognese", image = null)
        val title = "Placeholder Title"

        // When
        val result = MealPlanUtils.getDisplayName(recipe, title)

        // Then
        assertEquals("Spaghetti Bolognese", result)
    }

    @Test
    fun getDisplayName_withNullRecipe_returnsTitle() {
        // Given
        val recipe: Recipe? = null
        val title = "Test Placeholder"

        // When
        val result = MealPlanUtils.getDisplayName(recipe, title)

        // Then
        assertEquals("Test Placeholder", result)
    }

    @Test
    fun getDisplayName_withNullRecipeAndEmptyTitle_returnsEmptyString() {
        // Given
        val recipe: Recipe? = null
        val title = ""

        // When
        val result = MealPlanUtils.getDisplayName(recipe, title)

        // Then
        assertEquals("", result)
    }

    @Test
    fun getRecipeUrl_withValidRecipe_returnsRecipeId() {
        // Given
        val recipe = Recipe(id = 42, name = "Test Recipe", image = null)

        // When
        val result = MealPlanUtils.getRecipeUrl(recipe)

        // Then
        assertEquals(42, result)
    }

    @Test
    fun getRecipeUrl_withNullRecipe_returnsNull() {
        // Given
        val recipe: Recipe? = null

        // When
        val result = MealPlanUtils.getRecipeUrl(recipe)

        // Then
        assertNull(result)
    }

    @Test
    fun getRecipeUrl_withInvalidRecipeId_returnsNull() {
        // Given
        val recipe = Recipe(id = 0, name = "Test Recipe", image = null)

        // When
        val result = MealPlanUtils.getRecipeUrl(recipe)

        // Then
        assertNull(result)
    }

    @Test
    fun getRecipeUrl_withNegativeRecipeId_returnsNull() {
        // Given
        val recipe = Recipe(id = -1, name = "Test Recipe", image = null)

        // When
        val result = MealPlanUtils.getRecipeUrl(recipe)

        // Then
        assertNull(result)
    }

    @Test
    fun safeParseDate_withValidIsoDate_extractsDatePortion() {
        // Given
        val rawDate = "2025-12-01T22:24:35.522000+01:00"

        // When
        val result = MealPlanUtils.safeParseDate(rawDate)

        // Then
        assertEquals("2025-12-01", result)
    }

    @Test
    fun safeParseDate_withSimpleDate_returnsDate() {
        // Given
        val rawDate = "2025-12-01"

        // When
        val result = MealPlanUtils.safeParseDate(rawDate)

        // Then
        assertEquals("2025-12-01", result)
    }

    @Test
    fun safeParseDate_withShortString_returnsOriginal() {
        // Given
        val rawDate = "2025-12"

        // When
        val result = MealPlanUtils.safeParseDate(rawDate)

        // Then
        assertEquals("2025-12", result)
    }

    @Test
    fun safeParseDate_withEmptyString_returnsEmpty() {
        // Given
        val rawDate = ""

        // When
        val result = MealPlanUtils.safeParseDate(rawDate)

        // Then
        assertEquals("", result)
    }

    @Test
    fun formatDateForDisplay_withValidDate_formatsCorrectly() {
        // Given
        val dateString = "2025-12-01"
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val outputFormat = SimpleDateFormat("EEE dd/MM", Locale.US)

        // When
        val result = MealPlanUtils.formatDateForDisplay(dateString, inputFormat, outputFormat)

        // Then
        // Should be formatted as day of week, day/month
        assertTrue(result.matches(Regex("\\w{3} \\d{2}/\\d{2}")))
    }

    @Test
    fun formatDateForDisplay_withInvalidDate_returnsOriginal() {
        // Given
        val dateString = "invalid-date"
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val outputFormat = SimpleDateFormat("EEE dd/MM", Locale.US)

        // When
        val result = MealPlanUtils.formatDateForDisplay(dateString, inputFormat, outputFormat)

        // Then
        assertEquals("invalid-date", result)
    }

    @Test
    fun buildRecipeUrl_withValidRecipe_returnsFullUrlWithTrailingSlash() {
        // Given
        val tandoorUrl = "https://tandoor.example.com"
        val recipe = Recipe(id = 42, name = "Test Recipe", image = null)

        // When
        val result = MealPlanUtils.buildRecipeUrl(tandoorUrl, recipe)

        // Then
        assertEquals("https://tandoor.example.com/recipe/42/", result)
    }

    @Test
    fun buildRecipeUrl_withNullRecipe_returnsBaseUrl() {
        // Given
        val tandoorUrl = "https://tandoor.example.com"
        val recipe: Recipe? = null

        // When
        val result = MealPlanUtils.buildRecipeUrl(tandoorUrl, recipe)

        // Then
        assertEquals("https://tandoor.example.com", result)
    }

    @Test
    fun buildRecipeUrl_withInvalidRecipeId_returnsBaseUrl() {
        // Given
        val tandoorUrl = "https://tandoor.example.com"
        val recipe = Recipe(id = 0, name = "Test Recipe", image = null)

        // When
        val result = MealPlanUtils.buildRecipeUrl(tandoorUrl, recipe)

        // Then
        assertEquals("https://tandoor.example.com", result)
    }

    @Test
    fun buildRecipeUrl_withTrailingSlashInBaseUrl_handlesCorrectly() {
        // Given
        val tandoorUrl = "https://tandoor.example.com/"
        val recipe = Recipe(id = 123, name = "Test Recipe", image = null)

        // When
        val result = MealPlanUtils.buildRecipeUrl(tandoorUrl, recipe)

        // Then
        // Should trim trailing slash from base URL to avoid double slashes
        assertEquals("https://tandoor.example.com/recipe/123/", result)
    }
}
