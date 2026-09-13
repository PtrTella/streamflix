package com.streamflix.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamflix.desktop.player.VideoPlayerController
import com.streamflix.desktop.theme.*
import com.streamflixreborn.streamflix.models.Video
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent

@Composable
fun PlayerScreen(
    video: Video,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(true) }
    var vlcComponent by remember { mutableStateOf<EmbeddedMediaPlayerComponent?>(null) }
    val isVlcAvailable = remember { VideoPlayerController.isVlcAvailable() }

    DisposableEffect(video) {
        val comp = if (isVlcAvailable) {
            try {
                VideoPlayerController.createMediaPlayerComponent(video)
            } catch (_: Exception) {
                null
            }
        } else null
        vlcComponent = comp

        onDispose {
            try {
                comp?.mediaPlayer()?.controls()?.stop()
                comp?.release()
            } catch (_: Exception) {}
        }
    }

    Column(modifier = modifier.fillMaxSize().background(BackgroundDark)) {
        // Player Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(SidebarDark)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("Riproduzione Streaming", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }

            // Button to open in external VLC/IINA
            Button(
                onClick = { VideoPlayerController.launchExternalPlayer(video) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Apri con VLC / IINA Esterno", fontSize = 12.sp)
            }
        }

        // Player Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val comp = vlcComponent
            if (comp != null) {
                SwingPanel(
                    factory = { comp },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = AccentRed, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Streaming pronto per la riproduzione",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Puoi riprodurre il flusso video direttamente con VLC o IINA.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { VideoPlayerController.launchExternalPlayer(video) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Avvia in VLC / IINA")
                    }
                }
            }
        }

        // Bottom Controls Bar (if embedded VLC is running)
        val compForControls = vlcComponent
        if (compForControls != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SidebarDark)
                    .padding(vertical = 12.dp)
            ) {
                IconButton(onClick = {
                    try {
                        val controls = compForControls.mediaPlayer().controls()
                        if (isPlaying) controls.pause() else controls.play()
                        isPlaying = !isPlaying
                    } catch (_: Exception) {}
                }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pausa",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(onClick = {
                    try {
                        compForControls.mediaPlayer().controls().stop()
                        onBack()
                    } catch (_: Exception) {}
                }) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = AccentRed)
                }
            }
        }
    }
}
