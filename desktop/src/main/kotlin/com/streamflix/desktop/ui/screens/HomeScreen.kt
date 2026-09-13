package com.streamflix.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamflix.desktop.theme.*
import com.streamflix.desktop.ui.components.MediaCard
import com.streamflixreborn.streamflix.aggregator.AggregatorService
import com.streamflixreborn.streamflix.aggregator.UnifiedCategory
import com.streamflixreborn.streamflix.aggregator.UnifiedMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    onSelectMedia: (UnifiedMedia) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var categories by remember { mutableStateOf<List<UnifiedCategory>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun loadHome() {
        isLoading = true
        errorMsg = null
        coroutineScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    AggregatorService.getUnifiedHome()
                }
                categories = data
            } catch (e: Exception) {
                errorMsg = e.message ?: "Errore durante il caricamento del catalogo"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadHome()
    }

    Box(modifier = modifier.fillMaxSize().padding(24.dp)) {
        when {
            isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = AccentRed)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Aggregazione titoli da tutti i provider attivi...", color = TextSecondary, fontSize = 14.sp)
                }
            }

            errorMsg != null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(errorMsg ?: "Errore", color = AccentRed, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { loadHome() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Riprova")
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    items(categories) { category ->
                        Column {
                            Text(
                                text = category.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(bottom = 14.dp)
                            )

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(category.items) { media ->
                                    MediaCard(
                                        media = media,
                                        onClick = { onSelectMedia(media) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
