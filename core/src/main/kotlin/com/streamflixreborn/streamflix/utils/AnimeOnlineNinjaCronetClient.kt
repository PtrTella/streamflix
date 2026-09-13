package com.streamflixreborn.streamflix.utils

import android.content.Context
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AnimeOnlineNinjaCronetClient {

    data class Response(
        val statusCode: Int,
        val finalUrl: String,
        val headers: Map<String, List<String>>,
        val body: ByteArray,
    ) {
        val isSuccessful: Boolean get() = statusCode in 200..299
        fun bodyAsString(): String = body.toString(Charsets.UTF_8)
    }

    fun init(context: Context) {}

    suspend fun get(
        context: Context,
        url: String,
        headers: Map<String, String>,
        useCache: Boolean = true,
    ): Response = withContext(Dispatchers.IO) {
        val reqBuilder = Request.Builder().url(url)
        headers.forEach { (k, v) -> reqBuilder.header(k, v) }
        val okResponse = NetworkClient.default.newCall(reqBuilder.build()).execute()
        val bytes = okResponse.body?.bytes() ?: ByteArray(0)
        val headerMap = okResponse.headers.toMultimap()
        Response(
            statusCode = okResponse.code,
            finalUrl = okResponse.request.url.toString(),
            headers = headerMap,
            body = bytes
        )
    }
}
