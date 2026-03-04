package com.recipebookmarks.domain

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters

/**
 * WorkManager Worker that wraps ImportService for background recipe import operations.
 * This worker can be enqueued to process recipe URLs in the background.
 */
class ImportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        const val KEY_URLS = "urls"
        const val KEY_SUCCESS_COUNT = "success_count"
        const val KEY_FAILURE_COUNT = "failure_count"
        const val KEY_FAILURE_URLS = "failure_urls"
        const val KEY_FAILURE_ERRORS = "failure_errors"
    }
    
    override suspend fun doWork(): Result {
        val urls = inputData.getStringArray(KEY_URLS)?.toList() ?: return Result.failure()
        
        // Initialize dependencies
        // TODO: Use dependency injection (e.g., Hilt, Koin) in production
        val database = com.recipebookmarks.data.RecipeDatabase.getDatabase(applicationContext)
        val networkClient = NetworkClientImpl()
        val recipeParser = RecipeParserImpl()
        val recipeRepository = RecipeRepositoryImpl(database.recipeDao())
        
        val importService = ImportService(networkClient, recipeParser, recipeRepository)
        
        // Process URLs and get summary
        val summary = importService.handleSharedUrls(urls)
        
        // Build output data
        val outputData = Data.Builder()
            .putInt(KEY_SUCCESS_COUNT, summary.successCount)
            .putInt(KEY_FAILURE_COUNT, summary.failureCount)
            .putStringArray(KEY_FAILURE_URLS, summary.failures.map { it.url }.toTypedArray())
            .putStringArray(KEY_FAILURE_ERRORS, summary.failures.map { it.error.name }.toTypedArray())
            .build()
        
        return Result.success(outputData)
    }
}
