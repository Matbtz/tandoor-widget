package com.example.tandoorwidget

data class MealPlanResponse(
    val results: List<MealPlan>
)

data class MealPlan(
    val id: Int,
    val title: String,
    val recipe: Recipe?,
    val from_date: String,
    val to_date: String? = null,  // Optional field for multi-day meals
    val meal_type: MealType,
    val meal_type_name: String
)

data class Recipe(
    val id: Int,
    val name: String,
    val image: String?
)

data class MealType(
    val id: Int,
    val name: String
)

data class MealPlanUpdate(
    val from_date: String? = null,
    val to_date: String? = null
)
