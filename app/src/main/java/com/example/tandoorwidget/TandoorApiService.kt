package com.example.tandoorwidget

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

import retrofit2.http.Header

interface TandoorApiService {
    @GET("api/meal-plan/")
    fun getMealPlan(
        @Header("Authorization") authorization: String,
        @Query("from_date") fromDate: String,
        @Query("to_date") toDate: String
    ): Call<MealPlanResponse>
}
