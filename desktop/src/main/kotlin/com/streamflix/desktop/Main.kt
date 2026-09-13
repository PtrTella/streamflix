package com.streamflix.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.streamflix.desktop.theme.BackgroundDark
import com.streamflix.desktop.theme.StreamFlixTheme
import com.streamflix.desktop.ui.components.NavDestination
import com.streamflix.desktop.ui.components.Sidebar
import com.streamflix.desktop.ui.screens.*
import com.streamflixreborn.streamflix.aggregator.UnifiedMedia
import com.streamflixreborn.streamflix.models.Video
import java.awt.Dimension

fun main() = application {
    val windowState = remember { WindowState(width = 1200.dp, height = 800.dp) }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "StreamFlix Reborn"
    ) {
        window.minimumSize = Dimension(960, 640)

        StreamFlixTheme {
            var currentDestination by remember { mutableStateOf(NavDestination.HOME) }
            var selectedMedia by remember { mutableStateOf<UnifiedMedia?>(null) }
            var activeVideo by remember { mutableStateOf<Video?>(null) }

            LaunchedEffect(Unit) {
                if (com.streamflixreborn.streamflix.utils.UserPreferences.remoteDomainsUrl.isNotBlank()) {
                    com.streamflixreborn.streamflix.utils.UserPreferences.syncRemoteDomains()
                }
            }

            Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
                val currentVideo = activeVideo
                if (currentVideo != null) {
                    PlayerScreen(
                        video = currentVideo,
                        onBack = { activeVideo = null }
                    )
                } else {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Sidebar(
                            currentDestination = currentDestination,
                            onNavigate = { currentDestination = it }
                        )

                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            when (currentDestination) {
                                NavDestination.HOME -> HomeScreen(
                                    onSelectMedia = { selectedMedia = it }
                                )
                                NavDestination.SEARCH -> SearchScreen(
                                    onSelectMedia = { selectedMedia = it }
                                )
                                NavDestination.MOVIES -> MoviesScreen(
                                    onSelectMedia = { selectedMedia = it }
                                )
                                NavDestination.TV_SHOWS -> TvShowsScreen(
                                    onSelectMedia = { selectedMedia = it }
                                )
                                NavDestination.SETTINGS -> SettingsScreen()
                            }
                        }
                    }

                    // Dialog dettaglio film/serie
                    val media = selectedMedia
                    if (media != null) {
                        DetailDialog(
                            media = media,
                            onDismiss = { selectedMedia = null },
                            onPlayVideo = { video ->
                                selectedMedia = null
                                activeVideo = video
                            }
                        )
                    }
                }
            }
        }
    }
}
