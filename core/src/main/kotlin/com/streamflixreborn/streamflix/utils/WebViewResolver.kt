package com.streamflixreborn.streamflix.utils

import android.content.Context
import android.util.Log
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WebViewResolver(private val context: Context? = null) {

    private val TAG = "WebViewResolver"

    data class Result(
        val html: String,
        val evaluatedValue: String? = null,
        val finalUrl: String? = null,
    )

    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
        completion: ((currentUrl: String, html: String, cookies: String) -> Boolean)? = null,
        shouldAllowNavigation: ((url: String, isMainFrame: Boolean) -> Boolean)? = null,
        pageReadyScriptProvider: ((currentUrl: String, html: String, cookies: String) -> String?)? = null,
        showImmediately: Boolean = false,
    ): String {
        return getResult(
            url,
            headers,
            completion,
            shouldAllowNavigation,
            null,
            pageReadyScriptProvider,
            showImmediately
        ).html
    }

    suspend fun getResult(
        url: String,
        headers: Map<String, String> = emptyMap(),
        completion: ((currentUrl: String, html: String, cookies: String) -> Boolean)? = null,
        shouldAllowNavigation: ((url: String, isMainFrame: Boolean) -> Boolean)? = null,
        valueScript: String? = null,
        pageReadyScriptProvider: ((currentUrl: String, html: String, cookies: String) -> String?)? = null,
        showImmediately: Boolean = false,
    ): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Fetching URL directly on JVM: $url")
        val reqBuilder = Request.Builder().url(url)
        headers.forEach { (k, v) -> reqBuilder.header(k, v) }
        val response = NetworkClient.default.newCall(reqBuilder.build()).execute()
        val body = response.body?.string().orEmpty()
        val finalUrl = response.request.url.toString()
        Result(html = body, evaluatedValue = null, finalUrl = finalUrl)
    }
}
