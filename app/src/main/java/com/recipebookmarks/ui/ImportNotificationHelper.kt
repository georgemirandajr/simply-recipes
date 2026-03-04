package com.recipebookmarks.ui

import com.recipebookmarks.domain.ImportError

/**
 * Helper class for generating import notification messages.
 * Separated for testability.
 */
object ImportNotificationHelper {
    
    /**
     * Generates error message for single URL import failure.
     * 
     * @param error The import error type
     * @return User-friendly error message
     */
    fun getSingleUrlErrorMessage(error: ImportError): String {
        return when (error) {
            ImportError.URL_INACCESSIBLE -> {
                // Requirement 11.10: Show error message for inaccessible URLs
                "Unable to access URL. Please check your internet connection and try again."
            }
            ImportError.PARSE_FAILED -> {
                // Requirement 11.11: Show error message for parse failures
                "Could not extract recipe data from this webpage. The page may not contain a recognizable recipe format."
            }
            else -> {
                "Failed to import recipe. Please try again."
            }
        }
    }
    
    /**
     * Generates success message for single URL import.
     * 
     * @return Success confirmation message
     */
    fun getSingleUrlSuccessMessage(): String {
        // Requirement 11.9: Show success confirmation for single URL imports
        return "Recipe imported successfully!"
    }
    
    /**
     * Generates summary message for multi-URL import.
     * 
     * @param successCount Number of successfully imported recipes
     * @param failureCount Number of failed imports
     * @param failureUrls Array of URLs that failed to import
     * @param failureErrors Array of error types corresponding to failed URLs
     * @return Formatted summary message
     */
    fun getMultiUrlSummaryMessage(
        successCount: Int,
        failureCount: Int,
        failureUrls: Array<String>,
        failureErrors: Array<String>
    ): String {
        // Requirement 11.12: Show import summary for multi-URL imports with success/failure counts
        return buildString {
            append("Import Complete\n\n")
            append("Successfully imported: $successCount recipe(s)\n")
            
            if (failureCount > 0) {
                append("Failed: $failureCount recipe(s)\n\n")
                
                // Show details of failures
                append("Failed URLs:\n")
                failureUrls.forEachIndexed { index, url ->
                    val errorType = failureErrors.getOrNull(index)
                    val errorDesc = when (errorType) {
                        ImportError.URL_INACCESSIBLE.name -> "inaccessible"
                        ImportError.PARSE_FAILED.name -> "no recipe data found"
                        else -> "error"
                    }
                    append("• ${url.take(50)}... ($errorDesc)\n")
                }
            }
        }
    }
    
    /**
     * Gets a short description of an import error for display in summaries.
     * 
     * @param error The import error type
     * @return Short error description
     */
    fun getErrorDescription(error: ImportError): String {
        return when (error) {
            ImportError.URL_INACCESSIBLE -> "inaccessible"
            ImportError.PARSE_FAILED -> "no recipe data found"
            else -> "error"
        }
    }
}
