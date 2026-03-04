package com.recipebookmarks.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Implementation of NetworkClient using OkHttp.
 *
 * Fetches HTML content from URLs with a 30-second timeout and proper error handling.
 */
class NetworkClientImpl : NetworkClient {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun fetchHtml(url: String): NetworkResult = withContext(Dispatchers.IO) {
        try {
            // Validate URL format
            if (!isValidUrl(url)) {
                return@withContext NetworkResult.Failure(NetworkError.INVALID_URL)
            }

            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext NetworkResult.Failure(NetworkError.URL_INACCESSIBLE)
                }

                val html = response.body?.string()
                if (html != null) {
                    NetworkResult.Success(html)
                } else {
                    NetworkResult.Failure(NetworkError.NETWORK_ERROR)
                }
            }
        } catch (e: SocketTimeoutException) {
            NetworkResult.Failure(NetworkError.TIMEOUT)
        } catch (e: UnknownHostException) {
            NetworkResult.Failure(NetworkError.URL_INACCESSIBLE)
        } catch (e: MalformedURLException) {
            NetworkResult.Failure(NetworkError.INVALID_URL)
        } catch (e: IOException) {
            NetworkResult.Failure(NetworkError.NETWORK_ERROR)
        } catch (e: Exception) {
            NetworkResult.Failure(NetworkError.NETWORK_ERROR)
        }
    }

    /**
     * Validates that the URL has a valid format.
     */
    private fun isValidUrl(url: String): Boolean {
        return try {
            val trimmedUrl = url.trim()
            trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://")
        } catch (e: Exception) {
            false
        }
    }
}
