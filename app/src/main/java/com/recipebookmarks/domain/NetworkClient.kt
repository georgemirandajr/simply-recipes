package com.recipebookmarks.domain

/**
 * Interface for fetching HTML content from URLs.
 */
interface NetworkClient {
    /**
     * Fetches HTML content from the specified URL.
     *
     * @param url The URL to fetch content from
     * @return NetworkResult containing either the HTML content or an error
     */
    suspend fun fetchHtml(url: String): NetworkResult
}

/**
 * Result of a network fetch operation.
 */
sealed class NetworkResult {
    /**
     * Successful fetch with HTML content.
     */
    data class Success(val html: String) : NetworkResult()

    /**
     * Failed fetch with error information.
     */
    data class Failure(val error: NetworkError) : NetworkResult()
}

/**
 * Types of network errors that can occur during fetch operations.
 */
enum class NetworkError {
    /**
     * The URL could not be reached (connection failed, host not found, etc.)
     */
    URL_INACCESSIBLE,

    /**
     * The request timed out after the configured timeout period.
     */
    TIMEOUT,

    /**
     * The URL format is invalid.
     */
    INVALID_URL,

    /**
     * An unexpected network error occurred.
     */
    NETWORK_ERROR
}
