package com.streamflix.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamflix.desktop.theme.*
import com.streamflix.desktop.ui.components.MediaCard
import com.streamflixreborn.streamflix.aggregator.AggregatorService
import com.streamflixreborn.streamflix.aggregator.MediaSource
import com.streamflixreborn.streamflix.aggregator.UnifiedMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MoviesScreen(
    onSelectMedia: (UnifiedMedia) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var movies by remember { mutableStateOf<List<UnifiedMedia>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            isLoading = true
            val loaded = withContext(Dispatchers.IO) {
                val providers = AggregatorService.getActiveProviders()
                val list = mutableListOf<UnifiedMedia>()
                for (p in providers) {
                    runCatching {
                        val mList = p.getMovies(1)
                        mList.forEach { m ->
                            val year = m.released?.get(java.util.Calendar.YEAR)?.toString()
                            list.add(
                                UnifiedMedia(
                                    id = m.id,
                                    title = m.title,
                                    poster = m.poster,
                                    banner = m.banner,
                                    rating = m.rating,
                                    releaseYear = year,
                                    overview = m.overview,
                                    isTvShow = false,
                                    sources = mutableListOf(MediaSource(p.name, m.id, false))
                                )
                            )
                        }
                    }
                }
                list.distinctBy { it.title.lowercase().trim() }
            }
            movies = loaded
            isLoading = false
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Text("Film in Streaming", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(18.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentRed)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(movies) { media ->
                    MediaCard(media = media, onClick = { onSelectMedia(media) })
                }
            }
        }
    }
}
