package com.recipebookmarks.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.InetAddress
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
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                return try {
                    android.util.Log.d("NetworkClientImpl", "DNS lookup for: $hostname")
                    val addresses = Dns.SYSTEM.lookup(hostname)
                    android.util.Log.d("NetworkClientImpl", "DNS resolved to: ${addresses.joinToString()}")
                    addresses
                } catch (e: UnknownHostException) {
                    android.util.Log.e("NetworkClientImpl", "DNS lookup failed for: $hostname", e)
                    throw e
                }
            }
        })
        .build()

    override suspend fun fetchHtml(url: String): NetworkResult = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("NetworkClientImpl", "Fetching URL: $url")
            
            // Validate URL format
            if (!isValidUrl(url)) {
                android.util.Log.w("NetworkClientImpl", "Invalid URL format: $url")
                return@withContext NetworkResult.Failure(NetworkError.INVALID_URL)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
                .build()

            android.util.Log.d("NetworkClientImpl", "Executing request...")
            client.newCall(request).execute().use { response ->
                android.util.Log.d("NetworkClientImpl", "Response code: ${response.code}")
                
                if (!response.isSuccessful) {
                    android.util.Log.w("NetworkClientImpl", "Unsuccessful response: ${response.code} ${response.message}")
                    return@withContext NetworkResult.Failure(NetworkError.URL_INACCESSIBLE)
                }

                val html = response.body?.string()
                if (html != null) {
                    android.util.Log.d("NetworkClientImpl", "Successfully fetched HTML (${html.length} chars)")
                    NetworkResult.Success(html)
                } else {
                    android.util.Log.w("NetworkClientImpl", "Response body was null")
                    NetworkResult.Failure(NetworkError.NETWORK_ERROR)
                }
            }
        } catch (e: SocketTimeoutException) {
            android.util.Log.e("NetworkClientImpl", "Timeout fetching URL: $url", e)
            NetworkResult.Failure(NetworkError.TIMEOUT)
        } catch (e: UnknownHostException) {
            android.util.Log.e("NetworkClientImpl", "Unknown host: $url", e)
            NetworkResult.Failure(NetworkError.URL_INACCESSIBLE)
        } catch (e: MalformedURLException) {
            android.util.Log.e("NetworkClientImpl", "Malformed URL: $url", e)
            NetworkResult.Failure(NetworkError.INVALID_URL)
        } catch (e: IOException) {
            android.util.Log.e("NetworkClientImpl", "IO error fetching URL: $url", e)
            NetworkResult.Failure(NetworkError.NETWORK_ERROR)
        } catch (e: Exception) {
            android.util.Log.e("NetworkClientImpl", "Unexpected error fetching URL: $url", e)
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
