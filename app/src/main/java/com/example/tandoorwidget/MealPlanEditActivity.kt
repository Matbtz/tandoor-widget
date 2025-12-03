package com.example.tandoorwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class MealPlanEditActivity : Activity() {
    private lateinit var mealNameTextView: TextView
    private lateinit var moveToDateSpinner: Spinner
    private lateinit var extendToDateSpinner: Spinner
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private lateinit var progressBar: ProgressBar
    
    private var mealPlanId: Int = -1
    private var currentFromDate: String = ""
    private var currentToDate: String? = null
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormat = SimpleDateFormat("EEE, MMM dd", Locale.US)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_plan_edit)
        
        // Get meal plan details from intent
        mealPlanId = intent.getIntExtra("meal_plan_id", -1)
        val mealName = intent.getStringExtra("meal_name") ?: ""
        currentFromDate = intent.getStringExtra("from_date") ?: ""
        currentToDate = intent.getStringExtra("to_date")
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        
        // Initialize views
        mealNameTextView = findViewById(R.id.meal_name)
        moveToDateSpinner = findViewById(R.id.move_to_date_spinner)
        extendToDateSpinner = findViewById(R.id.extend_to_date_spinner)
        saveButton = findViewById(R.id.save_button)
        cancelButton = findViewById(R.id.cancel_button)
        progressBar = findViewById(R.id.progress_bar)
        
        // Set meal name
        mealNameTextView.text = mealName
        
        // Generate date options (current week Saturday to Friday)
        val dates = generateWeekDates()
        
        // Setup move to date spinner
        val moveToDateAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            dates.map { formatDateForDisplay(it) }
        )
        moveToDateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        moveToDateSpinner.adapter = moveToDateAdapter
        
        // Set current date as selected
        val currentIndex = dates.indexOf(currentFromDate)
        if (currentIndex >= 0) {
            moveToDateSpinner.setSelection(currentIndex)
        }
        
        // Setup extend to date spinner (with empty option)
        val extendDates = listOf("None (single day)") + dates.map { formatDateForDisplay(it) }
        val extendToDateAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            extendDates
        )
        extendToDateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        extendToDateSpinner.adapter = extendToDateAdapter
        
        // Set current to_date as selected if exists
        if (currentToDate != null) {
            val toDateIndex = dates.indexOf(currentToDate)
            if (toDateIndex >= 0) {
                extendToDateSpinner.setSelection(toDateIndex + 1) // +1 for "None" option
            }
        }
        
        // Setup button listeners
        saveButton.setOnClickListener {
            saveMealPlanChanges(dates)
        }
        
        cancelButton.setOnClickListener {
            finish()
        }
    }
    
    private fun generateWeekDates(): List<String> {
        val calendar = Calendar.getInstance()
        // Find the start of the week (Saturday)
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
            calendar.add(Calendar.DATE, -1)
        }
        
        return (0..6).map {
            val date = sdf.format(calendar.time)
            calendar.add(Calendar.DATE, 1)
            date
        }
    }
    
    private fun formatDateForDisplay(dateString: String): String {
        return try {
            val date = sdf.parse(dateString)
            if (date != null) {
                displayFormat.format(date)
            } else {
                dateString
            }
        } catch (e: Exception) {
            dateString
        }
    }
    
    private fun saveMealPlanChanges(dates: List<String>) {
        val sharedPrefs = getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        val apiKey = sharedPrefs.getString("api_key_$appWidgetId", "") ?: ""
        val tandoorUrl = sharedPrefs.getString("tandoor_url_$appWidgetId", "") ?: ""
        
        if (tandoorUrl.isBlank() || apiKey.isBlank()) {
            Toast.makeText(this, "Missing API configuration", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Get selected dates
        val selectedFromDateIndex = moveToDateSpinner.selectedItemPosition
        val selectedToDateIndex = extendToDateSpinner.selectedItemPosition
        
        val newFromDate = dates[selectedFromDateIndex]
        val newToDate = if (selectedToDateIndex > 0) {
            dates[selectedToDateIndex - 1] // -1 for "None" option
        } else {
            null
        }
        
        // Validate that to_date is not before from_date
        if (newToDate != null && newToDate < newFromDate) {
            Toast.makeText(this, "End date cannot be before start date", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show progress
        progressBar.visibility = View.VISIBLE
        saveButton.isEnabled = false
        cancelButton.isEnabled = false
        
        // Make API call
        val apiService = ApiClient.getApiService(tandoorUrl)
        val authorization = "Bearer $apiKey"
        val updates = MealPlanUpdate(from_date = newFromDate, to_date = newToDate)
        
        apiService.updateMealPlan(authorization, mealPlanId, updates).enqueue(object : Callback<MealPlan> {
            override fun onResponse(call: Call<MealPlan>, response: Response<MealPlan>) {
                progressBar.visibility = View.GONE
                saveButton.isEnabled = true
                cancelButton.isEnabled = true
                
                if (response.isSuccessful) {
                    Toast.makeText(this@MealPlanEditActivity, "Meal plan updated successfully", Toast.LENGTH_SHORT).show()
                    
                    // Trigger widget refresh
                    refreshWidget()
                    
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Toast.makeText(this@MealPlanEditActivity, "Update failed: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }
            
            override fun onFailure(call: Call<MealPlan>, t: Throwable) {
                progressBar.visibility = View.GONE
                saveButton.isEnabled = true
                cancelButton.isEnabled = true
                
                Toast.makeText(this@MealPlanEditActivity, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
    
    private fun refreshWidget() {
        // Send broadcast to refresh the widget
        val intent = Intent(this, TandoorWidgetProvider::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        
        val ids = AppWidgetManager.getInstance(application)
            .getAppWidgetIds(ComponentName(application, TandoorWidgetProvider::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        sendBroadcast(intent)
    }
}
