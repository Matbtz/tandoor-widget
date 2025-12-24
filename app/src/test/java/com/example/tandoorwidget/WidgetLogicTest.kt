package com.example.tandoorwidget

import org.junit.Test
import org.junit.Assert.*
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Unit tests for TandoorWidgetService logic, specifically date grouping and display logic.
 */
class WidgetLogicTest {

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // Mock data structures matching the app's data models
    data class MockMealPlan(
        val id: Int,
        val from_date: String,
        val to_date: String? = null,
        val meal_type_name: String,
        val title: String = "Test Meal",
        val recipe: MockRecipe? = null
    ) {
        fun toMealPlan(): MealPlan {
            return MealPlan(
                id = id,
                title = title,
                recipe = recipe?.toRecipe(),
                from_date = from_date,
                to_date = to_date,
                meal_type = MealType(1, meal_type_name),
                meal_type_name = meal_type_name
            )
        }
    }

    data class MockRecipe(val id: Int, val name: String) {
        fun toRecipe() = Recipe(id, name, null)
    }

    @Test
    fun verifyDateGroupingLogic() {
        // Simulating the logic in TandoorWidgetService.onDataSetChanged

        // Given a week of dates
        val dates = listOf(
            "2025-12-06", // Sat
            "2025-12-07", // Sun
            "2025-12-08", // Mon
            "2025-12-09", // Tue
            "2025-12-10", // Wed
            "2025-12-11", // Thu
            "2025-12-12"  // Fri
        )

        // Given some meal plans
        val mealPlans = listOf(
            // Single day lunch on Saturday
            MockMealPlan(1, "2025-12-06", null, "Lunch").toMealPlan(),
            // Single day dinner on Saturday
            MockMealPlan(2, "2025-12-06", null, "Dinner").toMealPlan(),
            // Multi-day meal spanning Sat-Sun
            MockMealPlan(3, "2025-12-06", "2025-12-07", "Breakfast").toMealPlan(),
            // Meal outside range (previous week)
            MockMealPlan(4, "2025-12-01", null, "Lunch").toMealPlan(),
            // Meal spanning into next week (Fri-Sun)
            MockMealPlan(5, "2025-12-12", "2025-12-14", "Dinner").toMealPlan()
        )

        // When: Grouping logic runs (copied from Service)
        val mealPlansByDate = mutableMapOf<String, MutableList<MealPlan>>()
        mealPlans.forEach { meal ->
            dates.forEach { date ->
                if (MealPlanUtils.mealAppliesToDate(meal, date, sdf)) {
                    mealPlansByDate.getOrPut(date) { mutableListOf() }.add(meal)
                }
            }
        }

        // Then: Verify correct grouping

        // Saturday should have 3 meals: Lunch, Dinner, Breakfast(multi)
        val satMeals = mealPlansByDate["2025-12-06"]
        assertNotNull(satMeals)
        assertEquals(3, satMeals!!.size)
        assertTrue(satMeals.any { it.id == 1 })
        assertTrue(satMeals.any { it.id == 2 })
        assertTrue(satMeals.any { it.id == 3 })

        // Sunday should have 1 meal: Breakfast(multi)
        val sunMeals = mealPlansByDate["2025-12-07"]
        assertNotNull(sunMeals)
        assertEquals(1, sunMeals!!.size)
        assertTrue(sunMeals.any { it.id == 3 }) // Multi-day breakfast

        // Monday should have 0 meals
        val monMeals = mealPlansByDate["2025-12-08"]
        assertNull(monMeals)

        // Friday should have 1 meal: Dinner(multi)
        val friMeals = mealPlansByDate["2025-12-12"]
        assertNotNull(friMeals)
        assertEquals(1, friMeals!!.size)
        assertTrue(friMeals.any { it.id == 5 })
    }

    @Test
    fun verifyMealSortingLogic() {
        // Logic from TandoorWidgetService.updateFlattenedMeals

        // Given unsorted meals
        val meals = listOf(
            MockMealPlan(1, "2025-12-06", null, "Snack").toMealPlan(),
            MockMealPlan(2, "2025-12-06", null, "Dinner").toMealPlan(),
            MockMealPlan(3, "2025-12-06", null, "Lunch").toMealPlan(),
            MockMealPlan(4, "2025-12-06", null, "Breakfast").toMealPlan()
        )

        // When sorted
        val sortedMeals = meals.sortedBy { meal ->
            when (meal.meal_type_name.lowercase()) {
                "lunch" -> 0
                "dinner" -> 1
                else -> 2
            }
        }

        // Then order should be: Lunch, Dinner, Others (stable order for others)
        assertEquals("Lunch", sortedMeals[0].meal_type_name)
        assertEquals("Dinner", sortedMeals[1].meal_type_name)
        // Others come after, preserving original relative order if stable, or just after
        // Snack was before Breakfast in original list
        assertTrue(sortedMeals.indexOf(meals[0]) > 1) // Snack > Dinner
        assertTrue(sortedMeals.indexOf(meals[3]) > 1) // Breakfast > Dinner
    }

    @Test
    fun verifyMultiDayMealDateCheck() {
        // Direct test of MealPlanUtils.mealAppliesToDate for complex ranges

        val meal = MockMealPlan(1, "2025-12-01", "2025-12-05", "Trip").toMealPlan()

        assertTrue(MealPlanUtils.mealAppliesToDate(meal, "2025-12-01", sdf))
        assertTrue(MealPlanUtils.mealAppliesToDate(meal, "2025-12-03", sdf))
        assertTrue(MealPlanUtils.mealAppliesToDate(meal, "2025-12-05", sdf))

        assertFalse(MealPlanUtils.mealAppliesToDate(meal, "2025-11-30", sdf))
        assertFalse(MealPlanUtils.mealAppliesToDate(meal, "2025-12-06", sdf))
    }
}
