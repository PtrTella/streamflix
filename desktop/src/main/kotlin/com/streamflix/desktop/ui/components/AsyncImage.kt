package com.streamflix.desktop.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.streamflix.desktop.theme.SurfaceHighlight
import com.streamflix.desktop.theme.TextMuted
import com.streamflixreborn.streamflix.utils.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jetbrains.skia.Image
import java.util.concurrent.ConcurrentHashMap

private val imageCache = ConcurrentHashMap<String, ImageBitmap>()

@Composable
fun AsyncImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    var bitmap by remember(url) { mutableStateOf(url?.let { imageCache[it] }) }
    var isLoading by remember(url) { mutableStateOf(bitmap == null && !url.isNullOrBlank()) }

    LaunchedEffect(url) {
        if (url.isNullOrBlank() || bitmap != null) return@LaunchedEffect

        val cached = imageCache[url]
        if (cached != null) {
            bitmap = cached
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        val loaded = withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder().url(url).build()
                val res = NetworkClient.default.newCall(req).execute()
                if (res.isSuccessful) {
                    val bytes = res.body?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        val skiaImg = Image.makeFromEncoded(bytes)
                        skiaImg.toComposeImageBitmap().also {
                            imageCache[url] = it
                        }
                    } else null
                } else null
            } catch (_: Exception) {
                null
            }
        }
        bitmap = loaded
        isLoading = false
    }

    Box(modifier = modifier.background(SurfaceHighlight), contentAlignment = Alignment.Center) {
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                tint = TextMuted
            )
        }
    }
}
