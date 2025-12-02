package com.example.tandoorwidget

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for MealPlanUtils
 */
class MealPlanUtilsTest {
    
    @Test
    fun getDisplayName_withValidRecipe_returnsRecipeName() {
        val recipe = Recipe(id = 1, name = "Test Recipe", image = null)
        val result = MealPlanUtils.getDisplayName(recipe, "Placeholder Title")
        assertEquals("Test Recipe", result)
    }
    
    @Test
    fun getDisplayName_withNullRecipe_returnsTitle() {
        val result = MealPlanUtils.getDisplayName(null, "Placeholder Title")
        assertEquals("Placeholder Title", result)
    }
    
    @Test
    fun getDisplayName_withNullRecipeAndNullTitle_returnsUntitled() {
        val result = MealPlanUtils.getDisplayName(null, null)
        assertEquals("Untitled", result)
    }
    
    @Test
    fun getDisplayName_withNullRecipeAndEmptyTitle_returnsUntitled() {
        val result = MealPlanUtils.getDisplayName(null, "")
        assertEquals("Untitled", result)
    }
    
    @Test
    fun getDisplayName_withEmptyRecipeNameAndTitle_returnsTitle() {
        val recipe = Recipe(id = 1, name = "", image = null)
        val result = MealPlanUtils.getDisplayName(recipe, "Placeholder Title")
        assertEquals("Placeholder Title", result)
    }
    
    @Test
    fun getRecipeUrl_withValidRecipe_returnsUrl() {
        val recipe = Recipe(id = 123, name = "Test Recipe", image = null)
        val result = MealPlanUtils.getRecipeUrl(recipe, "https://tandoor.example.com")
        assertEquals("https://tandoor.example.com/recipe/123/", result)
    }
    
    @Test
    fun getRecipeUrl_withNullRecipe_returnsNull() {
        val result = MealPlanUtils.getRecipeUrl(null, "https://tandoor.example.com")
        assertNull(result)
    }
    
    @Test
    fun getRecipeUrl_withInvalidRecipeId_returnsNull() {
        val recipe = Recipe(id = 0, name = "Test Recipe", image = null)
        val result = MealPlanUtils.getRecipeUrl(recipe, "https://tandoor.example.com")
        assertNull(result)
    }
    
    @Test
    fun safeParseDate_withValidIsoDateTime_returnsDatePart() {
        val result = MealPlanUtils.safeParseDate("2023-12-25T14:30:00Z")
        assertEquals("2023-12-25", result)
    }
    
    @Test
    fun safeParseDate_withValidDate_returnsDate() {
        val result = MealPlanUtils.safeParseDate("2023-12-25")
        assertEquals("2023-12-25", result)
    }
    
    @Test
    fun safeParseDate_withShortString_returnsOriginal() {
        val result = MealPlanUtils.safeParseDate("2023")
        assertEquals("2023", result)
    }
    
    @Test
    fun safeParseDate_withInvalidDate_returnsFirst10Chars() {
        val result = MealPlanUtils.safeParseDate("9999-99-99T00:00:00Z")
        assertEquals("9999-99-99", result)
    }
}
