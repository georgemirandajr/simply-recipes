package com.recipebookmarks.ui

import com.recipebookmarks.domain.ImportError
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for ImportNotificationHelper.
 * Tests verify that appropriate messages are shown for different import scenarios.
 */
class ImportNotificationHelperTest {
    
    /**
     * Test that verifies error message for inaccessible URLs.
     * Validates Requirement 11.10: Show error message for inaccessible URLs
     */
    @Test
    fun `getSingleUrlErrorMessage should return correct message for URL_INACCESSIBLE`() {
        // Requirement 11.10: Show error message for inaccessible URLs
        val message = ImportNotificationHelper.getSingleUrlErrorMessage(ImportError.URL_INACCESSIBLE)
        
        assertEquals(
            "Unable to access URL. Please check your internet connection and try again.",
            message
        )
    }
    
    /**
     * Test that verifies error message for parse failures.
     * Validates Requirement 11.11: Show error message for parse failures
     */
    @Test
    fun `getSingleUrlErrorMessage should return correct message for PARSE_FAILED`() {
        // Requirement 11.11: Show error message for parse failures
        val message = ImportNotificationHelper.getSingleUrlErrorMessage(ImportError.PARSE_FAILED)
        
        assertEquals(
            "Could not extract recipe data from this webpage. The page may not contain a recognizable recipe format.",
            message
        )
    }
    
    @Test
    fun `getSingleUrlErrorMessage should return generic message for NETWORK_ERROR`() {
        val message = ImportNotificationHelper.getSingleUrlErrorMessage(ImportError.NETWORK_ERROR)
        
        assertEquals(
            "Failed to import recipe. Please try again.",
            message
        )
    }
    
    /**
     * Test that verifies success message format for single URL import.
     * Validates Requirement 11.9: Show success confirmation for single URL imports
     */
    @Test
    fun `getSingleUrlSuccessMessage should return success confirmation`() {
        // Requirement 11.9: Show success confirmation for single URL imports
        val message = ImportNotificationHelper.getSingleUrlSuccessMessage()
        
        assertEquals("Recipe imported successfully!", message)
    }
    
    /**
     * Test that verifies import summary format for multi-URL imports with only successes.
     * Validates Requirement 11.12: Show import summary with success/failure counts
     */
    @Test
    fun `getMultiUrlSummaryMessage should show success count when all imports succeed`() {
        // Requirement 11.12: Show import summary with success/failure counts
        val message = ImportNotificationHelper.getMultiUrlSummaryMessage(
            successCount = 5,
            failureCount = 0,
            failureUrls = emptyArray(),
            failureErrors = emptyArray()
        )
        
        assertTrue(message.contains("Import Complete"))
        assertTrue(message.contains("Successfully imported: 5 recipe(s)"))
        assertFalse(message.contains("Failed:"))
    }
    
    /**
     * Test that verifies import summary includes failure counts.
     * Validates Requirement 11.12: Show import summary with success/failure counts
     */
    @Test
    fun `getMultiUrlSummaryMessage should show both success and failure counts`() {
        // Requirement 11.12: Show import summary with success/failure counts
        val message = ImportNotificationHelper.getMultiUrlSummaryMessage(
            successCount = 3,
            failureCount = 2,
            failureUrls = arrayOf("https://example.com/recipe1", "https://example.com/recipe2"),
            failureErrors = arrayOf(ImportError.URL_INACCESSIBLE.name, ImportError.PARSE_FAILED.name)
        )
        
        assertTrue(message.contains("Import Complete"))
        assertTrue(message.contains("Successfully imported: 3 recipe(s)"))
        assertTrue(message.contains("Failed: 2 recipe(s)"))
    }
    
    /**
     * Test that verifies import summary includes failure details.
     * Validates Requirement 11.12: Show import summary with failure details
     */
    @Test
    fun `getMultiUrlSummaryMessage should include failure URLs and error descriptions`() {
        // Requirement 11.12: Show import summary with failure details
        val failureUrls = arrayOf(
            "https://example.com/recipe1",
            "https://example.com/recipe2"
        )
        val failureErrors = arrayOf(
            ImportError.URL_INACCESSIBLE.name,
            ImportError.PARSE_FAILED.name
        )
        
        val message = ImportNotificationHelper.getMultiUrlSummaryMessage(
            successCount = 1,
            failureCount = 2,
            failureUrls = failureUrls,
            failureErrors = failureErrors
        )
        
        assertTrue(message.contains("Failed URLs:"))
        assertTrue(message.contains("https://example.com/recipe1"))
        assertTrue(message.contains("inaccessible"))
        assertTrue(message.contains("https://example.com/recipe2"))
        assertTrue(message.contains("no recipe data found"))
    }
    
    @Test
    fun `getMultiUrlSummaryMessage should truncate long URLs`() {
        val longUrl = "https://example.com/" + "a".repeat(100)
        val message = ImportNotificationHelper.getMultiUrlSummaryMessage(
            successCount = 0,
            failureCount = 1,
            failureUrls = arrayOf(longUrl),
            failureErrors = arrayOf(ImportError.URL_INACCESSIBLE.name)
        )
        
        // URL should be truncated to 50 characters plus "..."
        assertTrue(message.contains("..."))
        assertFalse(message.contains(longUrl))
    }
    
    @Test
    fun `getErrorDescription should return correct description for URL_INACCESSIBLE`() {
        val description = ImportNotificationHelper.getErrorDescription(ImportError.URL_INACCESSIBLE)
        assertEquals("inaccessible", description)
    }
    
    @Test
    fun `getErrorDescription should return correct description for PARSE_FAILED`() {
        val description = ImportNotificationHelper.getErrorDescription(ImportError.PARSE_FAILED)
        assertEquals("no recipe data found", description)
    }
    
    @Test
    fun `getErrorDescription should return generic description for NETWORK_ERROR`() {
        val description = ImportNotificationHelper.getErrorDescription(ImportError.NETWORK_ERROR)
        assertEquals("error", description)
    }
}

