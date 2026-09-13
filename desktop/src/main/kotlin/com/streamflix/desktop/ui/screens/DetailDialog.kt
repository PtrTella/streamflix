package com.streamflix.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.streamflix.desktop.player.VideoPlayerController
import com.streamflix.desktop.theme.*
import com.streamflix.desktop.ui.components.AsyncImage
import com.streamflixreborn.streamflix.aggregator.AggregatorService
import com.streamflixreborn.streamflix.aggregator.MediaSource
import com.streamflixreborn.streamflix.aggregator.UnifiedMedia
import com.streamflixreborn.streamflix.models.Episode
import com.streamflixreborn.streamflix.models.Movie
import com.streamflixreborn.streamflix.models.Season
import com.streamflixreborn.streamflix.models.TvShow
import com.streamflixreborn.streamflix.models.Video
import com.streamflixreborn.streamflix.providers.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DetailDialog(
    media: UnifiedMedia,
    onDismiss: () -> Unit,
    onPlayVideo: (Video) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var availableSources by remember(media) { mutableStateOf(media.sources.toMutableList()) }
    var selectedSource by remember { mutableStateOf(availableSources.firstOrNull()) }

    var currentOverview by remember { mutableStateOf(media.overview) }
    var currentPoster by remember { mutableStateOf(media.poster) }
    var currentRating by remember { mutableStateOf(media.rating) }
    var currentReleaseYear by remember { mutableStateOf(media.releaseYear) }

    var isLoadingDetails by remember { mutableStateOf(false) }
    var seasons by remember { mutableStateOf<List<Season>>(emptyList()) }
    var selectedSeason by remember { mutableStateOf<Season?>(null) }
    var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var selectedEpisode by remember { mutableStateOf<Episode?>(null) }

    var servers by remember { mutableStateOf<List<Video.Server>>(emptyList()) }
    var isLoadingServers by remember { mutableStateOf(false) }
    var resolvingServerId by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf("") }

    // Parallel multi-provider search for the current title
    LaunchedEffect(media.title) {
        val cleanQuery = AggregatorService.cleanTitleForSearch(media.title).ifBlank { media.title }
        val providers = AggregatorService.getActiveProviders()
        val currentNames = availableSources.map { it.providerName.lowercase() }.toSet()
        val providersToQuery = providers.filter { p ->
            !currentNames.any { it.contains(p.name.lowercase()) || p.name.lowercase().contains(it) }
        }

        withContext(Dispatchers.IO) {
            providersToQuery.forEach { provider ->
                launch {
                    try {
                        val results = provider.search(cleanQuery)
                        val match = results.firstOrNull { item ->
                            when (item) {
                                is Movie -> AggregatorService.isSimilarTitle(item.title, cleanQuery) || AggregatorService.isSimilarTitle(item.title, media.title)
                                is TvShow -> AggregatorService.isSimilarTitle(item.title, cleanQuery) || AggregatorService.isSimilarTitle(item.title, media.title)
                                else -> false
                            }
                        }
                        if (match != null) {
                            val (itemId, isTv) = when (match) {
                                is Movie -> match.id to false
                                is TvShow -> match.id to true
                                else -> "" to false
                            }
                            if (itemId.isNotBlank()) {
                                val newSource = MediaSource(
                                    providerName = provider.name,
                                    providerId = itemId,
                                    isTvShow = isTv
                                )
                                withContext(Dispatchers.Main) {
                                    if (availableSources.none { it.providerName.equals(provider.name, ignoreCase = true) || it.providerId == newSource.providerId }) {
                                        availableSources = (availableSources + newSource).toMutableList()
                                        if (selectedSource == null) {
                                            selectedSource = newSource
                                        }
                                    }
                                }
                            }
                        }
                    } catch (_: Throwable) {
                        // ignore provider lookup errors
                    }
                }
            }
        }
    }

    fun resolveProvider(source: MediaSource): Provider? {
        val pid = source.providerId.lowercase()
        if (pid.contains("cb01")) return com.streamflixreborn.streamflix.providers.CB01Provider
        if (pid.contains("altadefinizione")) return com.streamflixreborn.streamflix.providers.Altadefinizione01Provider
        if (pid.contains("animeworld")) return com.streamflixreborn.streamflix.providers.AnimeWorldProvider

        val byName = Provider.providers.keys.find { it.name.equals(source.providerName, ignoreCase = true) }
            ?: Provider.findByName(source.providerName)
        if (byName != null) return byName

        return when {
            pid.contains("cb01") -> com.streamflixreborn.streamflix.providers.CB01Provider
            pid.contains("altadefinizione") -> com.streamflixreborn.streamflix.providers.Altadefinizione01Provider
            pid.contains("animeworld") -> com.streamflixreborn.streamflix.providers.AnimeWorldProvider
            else -> com.streamflixreborn.streamflix.providers.StreamingCommunityProvider("it")
        }
    }

    // Load Show / Movie details for selected source
    LaunchedEffect(selectedSource) {
        val source = selectedSource ?: return@LaunchedEffect
        val provider = resolveProvider(source) ?: return@LaunchedEffect
        val providerDisplayName = provider.name
        isLoadingDetails = true
        statusMessage = "Caricamento da $providerDisplayName..."

        withContext(Dispatchers.IO) {
            try {
                if (source.isTvShow) {
                    val tvShow = provider.getTvShow(source.providerId)
                    if (!tvShow.overview.isNullOrBlank()) currentOverview = tvShow.overview
                    if (!tvShow.poster.isNullOrBlank()) currentPoster = tvShow.poster
                    val r = tvShow.rating
                    if (r != null && r > 0.0) currentRating = r
                    val y = tvShow.released?.get(java.util.Calendar.YEAR)?.toString()
                    if (!y.isNullOrBlank()) currentReleaseYear = y
                    seasons = tvShow.seasons
                    selectedSeason = tvShow.seasons.firstOrNull()
                } else {
                    // Movie: fetch full movie details first (for overview, real metadata), then get servers
                    isLoadingServers = true
                    val movie = runCatching { provider.getMovie(source.providerId) }.getOrNull()
                    if (movie != null) {
                        if (!movie.overview.isNullOrBlank()) currentOverview = movie.overview
                        if (!movie.poster.isNullOrBlank()) currentPoster = movie.poster
                        val mr = movie.rating
                        if (mr != null && mr > 0.0) currentRating = mr
                        val y = movie.released?.get(java.util.Calendar.YEAR)?.toString()
                        if (!y.isNullOrBlank()) currentReleaseYear = y
                    }
                    val movieType = Video.Type.Movie(
                        id = movie?.id ?: source.providerId,
                        title = movie?.title ?: media.title,
                        releaseDate = currentReleaseYear ?: "",
                        poster = currentPoster ?: "",
                        imdbId = movie?.imdbId
                    )
                    servers = provider.getServers(movieType.id, movieType)
                    isLoadingServers = false
                }
            } catch (e: Exception) {
                statusMessage = "Errore sorgente: ${e.message}"
                isLoadingServers = false
            }
        }
        isLoadingDetails = false
    }

    // Load episodes when season changes
    LaunchedEffect(selectedSeason) {
        val season = selectedSeason ?: return@LaunchedEffect
        val source = selectedSource ?: return@LaunchedEffect
        val provider = resolveProvider(source) ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                episodes = provider.getEpisodesBySeason(season.id)
                selectedEpisode = episodes.firstOrNull()
            } catch (e: Exception) {
                statusMessage = "Errore caricamento episodi: ${e.message}"
            }
        }
    }

    // Load servers when episode changes
    LaunchedEffect(selectedEpisode) {
        val ep = selectedEpisode ?: return@LaunchedEffect
        val source = selectedSource ?: return@LaunchedEffect
        val provider = resolveProvider(source) ?: return@LaunchedEffect
        isLoadingServers = true
        withContext(Dispatchers.IO) {
            try {
                val episodeType = Video.Type.Episode(
                    id = ep.id,
                    number = ep.number,
                    title = ep.title,
                    poster = ep.poster,
                    overview = ep.overview,
                    tvShow = Video.Type.Episode.TvShow(
                        id = source.providerId,
                        title = media.title,
                        poster = media.poster,
                        banner = media.banner,
                        releaseDate = media.releaseYear,
                        imdbId = null
                    ),
                    season = Video.Type.Episode.Season(
                        number = selectedSeason?.number ?: 1,
                        title = selectedSeason?.title
                    )
                )
                servers = provider.getServers(ep.id, episodeType)
            } catch (e: Exception) {
                statusMessage = "Errore server episodio: ${e.message}"
            }
        }
        isLoadingServers = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(820.dp)
                .height(650.dp)
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = BackgroundDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    // Header Info
                    item {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            AsyncImage(
                                url = currentPoster ?: media.poster,
                                contentDescription = media.title,
                                modifier = Modifier
                                    .width(130.dp)
                                    .height(190.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )

                            Spacer(modifier = Modifier.width(20.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = media.title,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    IconButton(onClick = onDismiss) {
                                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = TextSecondary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currentReleaseYear ?: media.releaseYear ?: "",
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )
                                    val rating = currentRating ?: media.rating
                                    if (rating != null && rating > 0.0) {
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "★ ${String.format("%.1f", rating)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFB800)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = currentOverview ?: media.overview ?: "Nessuna sinossi disponibile.",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    maxLines = 6
                                )
                            }
                        }
                    }

                    // Selettore Fonti Multi-Provider
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Sorgenti di Streaming Disponibili:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableSources) { source ->
                                val isSelected = source == selectedSource
                                val provName = resolveProvider(source)?.name ?: source.providerName
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedSource = source },
                                    label = { Text(provName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentRed,
                                        selectedLabelColor = Color.White,
                                        containerColor = SurfaceDark,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }

                    // Selettore Stagioni ed Episodi (se serie TV)
                    if (media.isTvShow && seasons.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Stagioni:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(seasons) { season ->
                                    val isSelected = season == selectedSeason
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedSeason = season },
                                        label = { Text("Stagione ${season.number}") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AccentBlue,
                                            selectedLabelColor = Color.White,
                                            containerColor = SurfaceDark,
                                            labelColor = TextSecondary
                                        )
                                    )
                                }
                            }
                        }

                        if (episodes.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Episodi:",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(episodes) { ep ->
                                        val isSelected = ep == selectedEpisode
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedEpisode = ep },
                                            label = { Text("Ep. ${ep.number} ${ep.title?.take(15) ?: ""}") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = SurfaceHighlight,
                                                selectedLabelColor = Color.White,
                                                containerColor = SurfaceDark,
                                                labelColor = TextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Server di Riproduzione
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Server e Flussi Video:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (isLoadingServers) {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = AccentRed)
                            }
                        } else if (servers.isEmpty()) {
                            Text(
                                text = if (statusMessage.isNotBlank()) statusMessage else "Nessun server trovato per questa sorgente.",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                servers.forEach { server ->
                                    val isResolving = server.id == resolvingServerId
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                        ) {
                                            Column {
                                                Text(
                                                    text = server.name.ifBlank { "Server" },
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = selectedSource?.providerName ?: "",
                                                    fontSize = 11.sp,
                                                    color = TextMuted
                                                )
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                if (isResolving) {
                                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = AccentRed)
                                                } else {
                                                     // Pulsante Apri con Player Esterno (VLC / IINA)
                                                     Button(
                                                         onClick = {
                                                             coroutineScope.launch {
                                                                 resolvingServerId = server.id
                                                                 statusMessage = "Risoluzione stream per ${server.name}..."
                                                                 try {
                                                                     val src = selectedSource ?: return@launch
                                                                     val provider = resolveProvider(src)
                                                                     val video = withContext(Dispatchers.IO) {
                                                                         provider?.getVideo(server)
                                                                     }
                                                                     if (video != null) {
                                                                         statusMessage = ""
                                                                         VideoPlayerController.launchExternalPlayer(video, server.name)
                                                                     } else {
                                                                         statusMessage = "Impossibile estrarre lo stream da ${server.name}"
                                                                     }
                                                                 } catch (e: Exception) {
                                                                     statusMessage = "Errore stream: ${e.message ?: "Fallito"}"
                                                                 } finally {
                                                                     resolvingServerId = null
                                                                 }
                                                             }
                                                         },
                                                         colors = ButtonDefaults.buttonColors(containerColor = SurfaceHighlight),
                                                         contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                         shape = RoundedCornerShape(8.dp)
                                                     ) {
                                                         Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                                         Spacer(modifier = Modifier.width(6.dp))
                                                         Text("VLC Esterno", fontSize = 12.sp)
                                                     }

                                                     // Pulsante Riproduci integrato
                                                     Button(
                                                         onClick = {
                                                             coroutineScope.launch {
                                                                 resolvingServerId = server.id
                                                                 statusMessage = "Risoluzione stream per ${server.name}..."
                                                                 try {
                                                                     val src = selectedSource ?: return@launch
                                                                     val provider = resolveProvider(src)
                                                                     val video = withContext(Dispatchers.IO) {
                                                                         provider?.getVideo(server)
                                                                     }
                                                                     if (video != null) {
                                                                         if (VideoPlayerController.isVlcDylibCompatible()) {
                                                                             statusMessage = ""
                                                                             onPlayVideo(video)
                                                                         } else {
                                                                             statusMessage = "Avviato nel player esterno (VLC/IINA)!"
                                                                             VideoPlayerController.launchExternalPlayer(video, server.name)
                                                                         }
                                                                     } else {
                                                                         statusMessage = "Impossibile estrarre lo stream da ${server.name}"
                                                                     }
                                                                 } catch (e: Exception) {
                                                                     statusMessage = "Errore stream: ${e.message ?: "Fallito"}"
                                                                 } finally {
                                                                     resolvingServerId = null
                                                                 }
                                                             }
                                                         },
                                                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Riproduci", fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
