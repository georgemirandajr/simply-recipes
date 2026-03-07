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
    
    /**
     * Test that verifies message selection with no fallbacks.
     * When all imports are successful with structured data, should show standard success message.
     * Validates Requirements 4.2, 4.3, 4.4
     */
    @Test
    fun `showImportSummary should show standard success message with no fallbacks`() {
        // Requirements 4.2, 4.3, 4.4: Show appropriate message based on success/fallback/failure counts
        val message = ImportNotificationHelper.getImportSummaryMessage(
            successCount = 5,
            fallbackCount = 0,
            failureCount = 0
        )
        
        // Should not mention fallbacks when there are none
        assertFalse(message.contains("bookmark"))
        assertTrue(message.contains("5"))
    }
    
    /**
     * Test that verifies message selection with only fallbacks.
     * When all imports result in fallback recipes, should indicate bookmarks were created.
     * Validates Requirements 4.2, 4.3, 4.4
     */
    @Test
    fun `showImportSummary should show fallback message when only fallbacks exist`() {
        // Requirements 4.2, 4.3, 4.4: Show appropriate message based on success/fallback/failure counts
        val message = ImportNotificationHelper.getImportSummaryMessage(
            successCount = 3,
            fallbackCount = 3,
            failureCount = 0
        )
        
        // Should mention bookmarks when all are fallbacks
        assertTrue(message.contains("bookmark"))
        assertTrue(message.contains("3"))
    }
    
    /**
     * Test that verifies message selection with mixed results.
     * When imports have mix of success, fallback, and failures, should show all counts.
     * Validates Requirements 4.2, 4.3, 4.4
     */
    @Test
    fun `showImportSummary should show mixed results message with all counts`() {
        // Requirements 4.2, 4.3, 4.4: Show appropriate message based on success/fallback/failure counts
        val message = ImportNotificationHelper.getImportSummaryMessage(
            successCount = 5,
            fallbackCount = 2,
            failureCount = 1
        )
        
        // Should show all three counts
        assertTrue(message.contains("5"))
        assertTrue(message.contains("2"))
        assertTrue(message.contains("1"))
        assertTrue(message.contains("bookmark"))
    }
    
    /**
     * Test that verifies message with successes and fallbacks but no failures.
     * Validates Requirements 4.2, 4.3
     */
    @Test
    fun `showImportSummary should show success with fallbacks message when no failures`() {
        // Requirements 4.2, 4.3: Show appropriate message for success with fallbacks
        val message = ImportNotificationHelper.getImportSummaryMessage(
            successCount = 4,
            fallbackCount = 2,
            failureCount = 0
        )
        
        // Should mention both successes and fallbacks
        assertTrue(message.contains("4"))
        assertTrue(message.contains("2"))
        assertTrue(message.contains("bookmark"))
        // Should not mention failures
        assertFalse(message.contains("failed") || message.contains("Failed"))
    }
    
    /**
     * Test that verifies message with only failures.
     * Edge case: all imports failed completely.
     */
    @Test
    fun `showImportSummary should handle all failures case`() {
        val message = ImportNotificationHelper.getImportSummaryMessage(
            successCount = 0,
            fallbackCount = 0,
            failureCount = 3
        )
        
        // Should indicate failures
        assertTrue(message.contains("3"))
    }
}
