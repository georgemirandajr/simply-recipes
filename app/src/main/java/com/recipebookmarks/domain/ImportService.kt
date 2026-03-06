package com.recipebookmarks.domain

import android.util.Log

/**
 * Service for importing recipes from shared URLs.
 * Processes URLs sequentially and provides import summary.
 * 
 * This class can be used by IntentService, WorkManager Worker, or any other
 * Android component that needs to import recipes from URLs.
 */
class ImportService(
    private val networkClient: NetworkClient,
    private val recipeParser: RecipeParser,
    private val recipeRepository: RecipeRepository
) {
    
    companion object {
        private const val TAG = "ImportService"
    }
    
    /**
     * Handles importing recipes from a list of shared URLs.
     * Processes each URL sequentially, tracking successes and failures.
     *
     * @param urls List of URLs to import recipes from
     * @return ImportSummary containing success/failure counts and failure details
     */
    suspend fun handleSharedUrls(urls: List<String>): ImportSummary {
        var successCount = 0
        val failures = mutableListOf<ImportFailure>()
        val fallbackUrls = mutableListOf<String>()  // Track fallback recipes
        
        for (url in urls) {
            try {
                Log.d(TAG, "Processing URL: $url")
                
                // Step 1: Fetch HTML from URL
                val fetchResult = networkClient.fetchHtml(url)
                
                when (fetchResult) {
                    is NetworkResult.Success -> {
                        // Step 2: Parse recipe from HTML
                        val parseResult = recipeParser.parseRecipe(fetchResult.html, url)
                        
                        when (parseResult) {
                            is ParseResult.Success -> {
                                // Step 3: Ensure original URL is stored
                                val recipeWithUrl = parseResult.recipe.copy(originalUrl = url)
                                
                                // Step 4: Insert recipe into repository
                                val recipeId = recipeRepository.insertRecipe(recipeWithUrl)
                                
                                // Track if this was a fallback
                                if (recipeWithUrl.isFallback) {
                                    fallbackUrls.add(url)
                                    Log.d(TAG, "Created fallback recipe from $url with ID: $recipeId")
                                } else {
                                    Log.d(TAG, "Successfully imported recipe from $url with ID: $recipeId")
                                }
                                
                                successCount++
                            }
                            is ParseResult.Failure -> {
                                Log.w(TAG, "Failed to parse recipe from $url: ${parseResult.error}")
                                failures.add(
                                    ImportFailure(
                                        url = url,
                                        error = ImportError.PARSE_FAILED
                                    )
                                )
                            }
                        }
                    }
                    is NetworkResult.Failure -> {
                        Log.w(TAG, "Failed to fetch URL $url: ${fetchResult.error}")
                        val importError = when (fetchResult.error) {
                            NetworkError.URL_INACCESSIBLE,
                            NetworkError.INVALID_URL,
                            NetworkError.TIMEOUT -> ImportError.URL_INACCESSIBLE
                            NetworkError.NETWORK_ERROR -> ImportError.NETWORK_ERROR
                        }
                        failures.add(
                            ImportFailure(
                                url = url,
                                error = importError
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error processing URL $url", e)
                failures.add(
                    ImportFailure(
                        url = url,
                        error = ImportError.NETWORK_ERROR
                    )
                )
            }
        }
        
        val summary = ImportSummary(
            successCount = successCount,
            failureCount = failures.size,
            failures = failures,
            fallbackCount = fallbackUrls.size  // Set fallback count
        )
        
        Log.d(TAG, "Import complete: $successCount successes (${fallbackUrls.size} fallbacks), ${failures.size} failures")
        return summary
    }
}

/**
 * Summary of an import operation containing success/failure statistics.
 */
data class ImportSummary(
    val successCount: Int,
    val failureCount: Int,
    val failures: List<ImportFailure>,
    val fallbackCount: Int = 0
) : java.io.Serializable

/**
 * Details about a single import failure.
 */
data class ImportFailure(
    val url: String,
    val error: ImportError
) : java.io.Serializable
