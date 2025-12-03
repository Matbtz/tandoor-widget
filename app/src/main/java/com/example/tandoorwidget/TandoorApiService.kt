package com.example.tandoorwidget

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Body

import retrofit2.http.Header

interface TandoorApiService {
    @GET("api/meal-plan/")
    fun getMealPlan(
        @Header("Authorization") authorization: String,
        @Query("from_date") fromDate: String,
        @Query("to_date") toDate: String
    ): Call<MealPlanResponse>
    
    @PATCH("api/meal-plan/{id}/")
    fun updateMealPlan(
        @Header("Authorization") authorization: String,
        @Path("id") mealPlanId: Int,
        @Body updates: MealPlanUpdate
    ): Call<MealPlan>
}
