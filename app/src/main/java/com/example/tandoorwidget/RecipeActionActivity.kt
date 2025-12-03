package com.example.tandoorwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

/**
 * Activity that presents options for interacting with a recipe card:
 * 1. View Recipe - Opens the recipe URL in a browser
 * 2. Edit Dates - Opens MealPlanEditActivity to modify meal plan dates
 * 
 * This dialog provides a workaround for Android widget limitations (no long-press support).
 * Users can choose between viewing the recipe or editing the meal plan dates.
 */
class RecipeActionActivity : Activity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_action)
        
        // Get data from intent
        val recipeUrl = intent.getStringExtra("recipe_url")
        val mealName = intent.getStringExtra("meal_name") ?: "Meal"
        val mealPlanId = intent.getIntExtra("meal_plan_id", -1)
        val fromDate = intent.getStringExtra("from_date")
        val toDate = intent.getStringExtra("to_date")
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        
        // Set meal name in title
        val titleTextView = findViewById<TextView>(R.id.meal_name_title)
        titleTextView.text = mealName
        
        // View Recipe button
        val viewRecipeButton = findViewById<Button>(R.id.view_recipe_button)
        if (recipeUrl != null && recipeUrl.isNotEmpty()) {
            viewRecipeButton.setOnClickListener {
                // Open recipe URL in browser
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(recipeUrl))
                try {
                    startActivity(browserIntent)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to open browser: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Disable button if no recipe URL available (placeholder meals)
            viewRecipeButton.isEnabled = false
            viewRecipeButton.alpha = 0.5f
        }
        
        // Edit Dates button
        val editDatesButton = findViewById<Button>(R.id.edit_dates_button)
        editDatesButton.setOnClickListener {
            // Open MealPlanEditActivity
            val editIntent = Intent(this, MealPlanEditActivity::class.java)
            editIntent.putExtra("meal_plan_id", mealPlanId)
            editIntent.putExtra("meal_name", mealName)
            editIntent.putExtra("from_date", fromDate)
            editIntent.putExtra("to_date", toDate)
            editIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            startActivity(editIntent)
            finish()
        }
        
        // Cancel button
        val cancelButton = findViewById<Button>(R.id.cancel_button)
        cancelButton.setOnClickListener {
            finish()
        }
    }
}
