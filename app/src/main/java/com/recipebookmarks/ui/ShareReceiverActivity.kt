package com.recipebookmarks.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.recipebookmarks.R
import com.recipebookmarks.domain.ImportError
import com.recipebookmarks.domain.ImportWorker

/**
 * Activity that receives shared URLs and initiates recipe import.
 * Displays appropriate notifications based on import results.
 */
class ShareReceiverActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("ShareReceiverActivity", "onCreate called")
        Log.d("ShareReceiverActivity", "Intent action: ${intent.action}")
        Log.d("ShareReceiverActivity", "Intent type: ${intent.type}")
        Log.d("ShareReceiverActivity", "Intent extras: ${intent.extras?.keySet()?.joinToString()}")
        
        // Extract URLs from intent
        val urls = extractUrlsFromIntent(intent)
        
        Log.d("ShareReceiverActivity", "Extracted URLs: $urls")
        
        if (urls.isEmpty()) {
            Toast.makeText(this, "No URLs found to import", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Start import process
        startImport(urls)
    }
    
    private fun extractUrlsFromIntent(intent: Intent): List<String> {
        val urls = mutableListOf<String>()
        
        Log.d("ShareReceiverActivity", "Extracting URLs from intent")
        
        when (intent.action) {
            Intent.ACTION_SEND -> {
                Log.d("ShareReceiverActivity", "ACTION_SEND detected")
                // Single URL shared
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                    Log.d("ShareReceiverActivity", "EXTRA_TEXT: $text")
                    extractUrlFromText(text)?.let { 
                        urls.add(it)
                        Log.d("ShareReceiverActivity", "Extracted URL: $it")
                    }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                Log.d("ShareReceiverActivity", "ACTION_SEND_MULTIPLE detected")
                // Multiple URLs shared (e.g., from bookmark folder)
                intent.getStringArrayListExtra(Intent.EXTRA_TEXT)?.forEach { text ->
                    Log.d("ShareReceiverActivity", "EXTRA_TEXT item: $text")
                    extractUrlFromText(text)?.let { 
                        urls.add(it)
                        Log.d("ShareReceiverActivity", "Extracted URL: $it")
                    }
                }
            }
        }
        
        return urls
    }
    
    private fun extractUrlFromText(text: String): String? {
        // Simple URL extraction - looks for http/https URLs
        val urlPattern = Regex("https?://[^\\s]+")
        return urlPattern.find(text)?.value
    }
    
    private fun startImport(urls: List<String>) {
        // Show progress message
        Toast.makeText(this, "Importing ${urls.size} recipe(s)...", Toast.LENGTH_SHORT).show()
        
        // Create work request
        val inputData = Data.Builder()
            .putStringArray(ImportWorker.KEY_URLS, urls.toTypedArray())
            .build()
        
        val importRequest = OneTimeWorkRequestBuilder<ImportWorker>()
            .setInputData(inputData)
            .build()
        
        // Enqueue work and observe result
        val workManager = WorkManager.getInstance(applicationContext)
        workManager.enqueue(importRequest)
        
        workManager.getWorkInfoByIdLiveData(importRequest.id)
            .observe(this, Observer { workInfo ->
                if (workInfo != null && workInfo.state.isFinished) {
                    handleImportComplete(workInfo, urls.size)
                }
            })
    }
    
    private fun handleImportComplete(workInfo: WorkInfo, totalUrls: Int) {
        // Check if activity is finishing to avoid window leak
        if (isFinishing) {
            return
        }
        
        val outputData = workInfo.outputData
        val successCount = outputData.getInt(ImportWorker.KEY_SUCCESS_COUNT, 0)
        val failureCount = outputData.getInt(ImportWorker.KEY_FAILURE_COUNT, 0)
        val failureUrls = outputData.getStringArray(ImportWorker.KEY_FAILURE_URLS) ?: emptyArray()
        val failureErrors = outputData.getStringArray(ImportWorker.KEY_FAILURE_ERRORS) ?: emptyArray()
        
        if (totalUrls == 1) {
            // Single URL import - show specific message
            handleSingleUrlResult(successCount > 0, failureUrls.firstOrNull(), failureErrors.firstOrNull())
        } else {
            // Multi-URL import - show summary
            handleMultiUrlResult(successCount, failureCount, failureUrls, failureErrors)
        }
        
        finish()
    }
    
    private fun handleSingleUrlResult(success: Boolean, @Suppress("UNUSED_PARAMETER") url: String?, errorName: String?) {
        // Check if activity is finishing to avoid window leak
        if (isFinishing) {
            return
        }
        
        if (success) {
            // Requirement 11.9: Show success confirmation for single URL imports
            Toast.makeText(
                this,
                ImportNotificationHelper.getSingleUrlSuccessMessage(),
                Toast.LENGTH_LONG
            ).show()
        } else {
            // Show error message based on error type
            val error = try {
                ImportError.valueOf(errorName ?: "")
            } catch (e: IllegalArgumentException) {
                ImportError.NETWORK_ERROR
            }
            
            val errorMessage = ImportNotificationHelper.getSingleUrlErrorMessage(error)
            
            AlertDialog.Builder(this)
                .setTitle("Import Failed")
                .setMessage(errorMessage)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .setOnDismissListener { finish() }
                .show()
        }
    }
    
    private fun handleMultiUrlResult(
        successCount: Int,
        failureCount: Int,
        failureUrls: Array<String>,
        failureErrors: Array<String>
    ) {
        // Check if activity is finishing to avoid window leak
        if (isFinishing) {
            return
        }
        
        // Requirement 11.12: Show import summary for multi-URL imports with success/failure counts
        val message = ImportNotificationHelper.getMultiUrlSummaryMessage(
            successCount,
            failureCount,
            failureUrls,
            failureErrors
        )
        
        AlertDialog.Builder(this)
            .setTitle("Import Summary")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener { finish() }
            .show()
    }
}
