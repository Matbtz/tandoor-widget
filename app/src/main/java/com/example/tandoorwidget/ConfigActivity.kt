package com.example.tandoorwidget

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.widget.Toast
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Build

class ConfigActivity : Activity() {
    private lateinit var debugLogsTextView: TextView
    private val logs = StringBuilder()
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    
    private val logReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val widgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (widgetId == appWidgetId) {
                when (intent?.action) {
                    "com.example.tandoorwidget.ACTION_WIDGET_LOG" -> {
                        val message = intent.getStringExtra("log_message") ?: ""
                        appendLog("[LOG] $message")
                    }
                    "com.example.tandoorwidget.ACTION_WIDGET_ERROR" -> {
                        val message = intent.getStringExtra("error_message") ?: ""
                        appendLog("[ERROR] $message")
                    }
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        val tandoorUrlEditText = findViewById<EditText>(R.id.tandoor_url)
        val apiKeyEditText = findViewById<EditText>(R.id.api_key)
        val saveButton = findViewById<Button>(R.id.save_button)
        val testApiButton = findViewById<Button>(R.id.test_api_button)
        debugLogsTextView = findViewById(R.id.debug_logs)
        val clearLogsButton = findViewById<Button>(R.id.clear_logs_button)
        val doneButton = findViewById<Button>(R.id.done_button)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val sharedPrefs = getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE)

        // Pre-fill existing values if present
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val existingUrl = sharedPrefs.getString("tandoor_url_$appWidgetId", "")
            val existingKey = sharedPrefs.getString("api_key_$appWidgetId", "")
            if (!existingUrl.isNullOrEmpty()) {
                tandoorUrlEditText.setText(existingUrl)
            }
            if (!existingKey.isNullOrEmpty()) {
                apiKeyEditText.setText(existingKey)
            }
        }

        // Register broadcast receivers for logs
        try {
            val logFilter = IntentFilter().apply {
                addAction("com.example.tandoorwidget.ACTION_WIDGET_LOG")
                addAction("com.example.tandoorwidget.ACTION_WIDGET_ERROR")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(logReceiver, logFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(logReceiver, logFilter)
            }
            isReceiverRegistered = true
        } catch (e: Exception) {
            appendLog("[ERROR] Failed to register log receiver: ${e.message}")
        }

        clearLogsButton.setOnClickListener {
            logs.clear()
            debugLogsTextView.text = "Logs cleared. Refresh widget to see new logs..."
        }

        doneButton.setOnClickListener {
            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }

        testApiButton.setOnClickListener {
            var tandoorUrl = tandoorUrlEditText.text.toString().trim()
            val apiKey = apiKeyEditText.text.toString().trim()

            // Normalize URL
            tandoorUrl = tandoorUrl.replace(Regex("/api/meal-plan/?$"), "")
            if (!tandoorUrl.endsWith("/")) {
                tandoorUrl += "/"
            }

            if (tandoorUrl.isEmpty() || apiKey.isEmpty()) {
                Toast.makeText(this, "Please enter both a URL and an API key.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Clear logs and test API
            logs.clear()
            logs.append("=== Testing API Connection ===\n")
            logs.append("Base URL: $tandoorUrl\n")
            logs.append("API Key: ***${apiKey.length} characters***\n\n")
            debugLogsTextView.text = logs.toString()

            testApiConnection(tandoorUrl, apiKey)
        }

        saveButton.setOnClickListener {
            var tandoorUrl = tandoorUrlEditText.text.toString().trim()
            val apiKey = apiKeyEditText.text.toString().trim()

            // Normalize URL: remove /api/meal-plan/ if present at the end ($ anchor ensures only end-of-string matches)
            // This prevents double paths like /api/meal-plan/api/meal-plan/ when Retrofit adds the endpoint
            tandoorUrl = tandoorUrl.replace(Regex("/api/meal-plan/?$"), "")
            if (!tandoorUrl.endsWith("/")) {
                tandoorUrl += "/"
            }

            if (tandoorUrl.isEmpty() || apiKey.isEmpty()) {
                Toast.makeText(this, "Please enter both a URL and an API key.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sharedPrefs = getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE)

            with(sharedPrefs.edit()) {
                putString("tandoor_url_$appWidgetId", tandoorUrl)
                putString("api_key_$appWidgetId", apiKey)
                apply()
            }

            // Clear logs before refresh
            logs.clear()
            logs.append("Widget configuration saved. Refreshing...\n\n")
            debugLogsTextView.text = logs.toString()

            val appWidgetManager = AppWidgetManager.getInstance(this)
            TandoorWidgetProvider().onUpdate(this, appWidgetManager, intArrayOf(appWidgetId))

            Toast.makeText(this, "Configuration saved! Widget updating... Check logs below.", Toast.LENGTH_LONG).show()
            
            // Note: We don't finish() here anymore to allow viewing logs
            // The user can press back or home button when done viewing logs
        }
    }
    
    private fun appendLog(message: String) {
        runOnUiThread {
            logs.append(message).append("\n")
            debugLogsTextView.text = logs.toString()
        }
    }
    
    private fun testApiConnection(baseUrl: String, apiKey: String) {
        // Use a separate thread for network operations
        // Note: In production, consider using coroutines or ExecutorService for better lifecycle management
        Thread {
            try {
                val apiService = ApiClient.getApiService(baseUrl)
                val authorization = "Token $apiKey"
                
                // Get current week dates for testing
                val calendar = java.util.Calendar.getInstance()
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                
                // Find Saturday
                while (calendar.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.SATURDAY) {
                    calendar.add(java.util.Calendar.DATE, -1)
                }
                
                val fromDate = sdf.format(calendar.time)
                calendar.add(java.util.Calendar.DATE, 6)
                val toDate = sdf.format(calendar.time)
                
                appendLog("Request URL: ${baseUrl}api/meal-plan/")
                appendLog("Query params: from_date=$fromDate&to_date=$toDate")
                appendLog("Making API call...\n")
                
                val response = apiService.getMealPlan(authorization, fromDate, toDate).execute()
                
                appendLog("Response code: ${response.code()}")
                appendLog("Response message: ${response.message()}\n")
                
                if (response.isSuccessful) {
                    val mealPlans = response.body()?.results
                    val count = mealPlans?.size ?: 0
                    appendLog("✓ SUCCESS: Received $count meal plans\n")
                    
                    if (count > 0) {
                        appendLog("First few meal plans:")
                        mealPlans?.take(3)?.forEachIndexed { index, meal ->
                            appendLog("${index + 1}. ${meal.recipe.name}")
                            appendLog("   from_date: ${meal.from_date}")
                            appendLog("   Parsed date: ${meal.from_date.substring(0, 10)}")
                            appendLog("   Meal type: ${meal.meal_type_name}")
                        }
                    } else {
                        appendLog("⚠ No meal plans found for this date range.")
                        appendLog("Check if you have meals planned in Tandoor for these dates:")
                        appendLog("  $fromDate to $toDate")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    appendLog("✗ ERROR: ${response.code()}")
                    appendLog("Error body: $errorBody")
                }
            } catch (e: Exception) {
                appendLog("✗ EXCEPTION: ${e.javaClass.simpleName}")
                appendLog("Message: ${e.message}")
                e.printStackTrace()
            }
        }.start()
    }
    
    private var isReceiverRegistered = false
    
    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(logReceiver)
                isReceiverRegistered = false
            } catch (e: IllegalArgumentException) {
                // Receiver was already unregistered
            }
        }
    }
}
