package com.example.tandoorwidget

data class MealPlanResponse(
    val results: List<MealPlan>
)

data class MealPlan(
    val id: Int,
    val title: String,
    val recipe: Recipe,
    val from_date: String,
    val meal_type: MealType
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
