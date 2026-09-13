package com.streamflix.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamflix.desktop.theme.*
import com.streamflixreborn.streamflix.utils.UserPreferences
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()
    var dohUrl by remember { mutableStateOf(UserPreferences.dohProviderUrl) }
    var remoteUrl by remember { mutableStateOf(UserPreferences.remoteDomainsUrl) }
    var scDomain by remember { mutableStateOf(UserPreferences.streamingcommunityDomain) }
    var cb01Domain by remember { mutableStateOf(UserPreferences.getCustomProviderDomain("CB01", "cb01uno.homes")) }
    var altaDomain by remember { mutableStateOf(UserPreferences.getCustomProviderDomain("Altadefinizione01", "altadefinizione01.baby")) }
    var animeWorldDomain by remember { mutableStateOf(UserPreferences.getCustomProviderDomain("AnimeWorld", "animeworld.so")) }
    var guardaSerieDomain by remember { mutableStateOf(UserPreferences.getCustomProviderDomain("GuardaSerie", "guardaserie.skin")) }
    var eurostreamingDomain by remember { mutableStateOf(UserPreferences.getCustomProviderDomain("Eurostreaming", "eurostreaming.quest")) }

    var importJsonText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var savedMessage by remember { mutableStateOf(false) }
    var isSyncingRemote by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(28.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text("Impostazioni e Sorgenti", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Configura i domini dei provider, il bypass DNS e importa liste aggiornate.", fontSize = 13.sp, color = TextMuted)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { showImportDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceHighlight),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Importa Lista Domini")
                }

                Button(
                    onClick = {
                        UserPreferences.dohProviderUrl = dohUrl
                        UserPreferences.remoteDomainsUrl = remoteUrl
                        UserPreferences.streamingcommunityDomain = scDomain
                        UserPreferences.setCustomProviderDomain("CB01", cb01Domain)
                        UserPreferences.setCustomProviderDomain("Altadefinizione01", altaDomain)
                        UserPreferences.setCustomProviderDomain("AnimeWorld", animeWorldDomain)
                        UserPreferences.setCustomProviderDomain("GuardaSerie", guardaSerieDomain)
                        UserPreferences.setCustomProviderDomain("Eurostreaming", eurostreamingDomain)
                        savedMessage = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Salva Modifiche")
                }
            }
        }

        if (savedMessage) {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A2F)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Impostazioni salvate con successo!", color = Color(0xFF4ADE80), fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
            // Sezione Lista Canali / Domini Remota (Auto-aggiornante)
            item {
                Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = AccentRed)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Lista Domini Remota (Auto-aggiornante)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Inserisci l'URL di un file remoto (es. Pastebin o GitHub raw). L'app scaricherà e aggiornerà automaticamente i domini ad ogni avvio o premendo Aggiorna Ora.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = remoteUrl,
                                onValueChange = { remoteUrl = it },
                                label = { Text("URL File Remoto / Pastebin") },
                                placeholder = { Text("https://pastebin.com/raw/...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SurfaceHighlight,
                                    unfocusedContainerColor = SurfaceHighlight,
                                    focusedBorderColor = AccentRed,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isSyncingRemote = true
                                        UserPreferences.remoteDomainsUrl = remoteUrl
                                        val ok = UserPreferences.syncRemoteDomains()
                                        if (ok) {
                                            scDomain = UserPreferences.streamingcommunityDomain
                                            cb01Domain = UserPreferences.getCustomProviderDomain("CB01", "cb01uno.homes")
                                            altaDomain = UserPreferences.getCustomProviderDomain("Altadefinizione01", "altadefinizione01.baby")
                                            animeWorldDomain = UserPreferences.getCustomProviderDomain("AnimeWorld", "animeworld.so")
                                            guardaSerieDomain = UserPreferences.getCustomProviderDomain("GuardaSerie", "guardaserie.skin")
                                            eurostreamingDomain = UserPreferences.getCustomProviderDomain("Eurostreaming", "eurostreaming.quest")
                                            syncMessage = "Domini aggiornati con successo dalla lista remota!"
                                        } else {
                                            syncMessage = "Impossibile scaricare o applicare la lista remota (verifica URL o connessione)."
                                        }
                                        isSyncingRemote = false
                                    }
                                },
                                enabled = !isSyncingRemote && remoteUrl.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceHighlight),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(56.dp)
                            ) {
                                if (isSyncingRemote) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = AccentRed)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Aggiorna Ora")
                                }
                            }
                        }

                        if (syncMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = syncMessage!!,
                                fontSize = 12.sp,
                                color = if (syncMessage!!.contains("successo")) Color(0xFF4ADE80) else AccentRed
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SurfaceHighlight))
                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Sorgenti Configurate e Domini Attivi Rilevati:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(10.dp))

                        val domainList = listOf(
                            "StreamingCommunity" to scDomain,
                            "CB01" to cb01Domain,
                            "Altadefinizione" to altaDomain,
                            "AnimeWorld" to animeWorldDomain,
                            "GuardaSerie" to guardaSerieDomain,
                            "Eurostreaming" to eurostreamingDomain
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            domainList.forEach { (prov, domain) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceHighlight)
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (domain.isNotBlank()) Color(0xFF4ADE80) else AccentRed)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(prov, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                    }
                                    Text(
                                        text = if (domain.isNotBlank()) "https://$domain" else "Non configurato",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = if (domain.isNotBlank()) AccentBlue else TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sezione DoH (Bypass Piracy Shield / AGCOM)
            item {
                Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = AccentBlue)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Bypass DNS (DNS-over-HTTPS)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Risolve i domini dei siti di streaming bypassando i blocchi imposti dai provider internet italiani.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = dohUrl,
                            onValueChange = { dohUrl = it },
                            label = { Text("URL Provider DoH") },
                            placeholder = { Text("https://cloudflare-dns.com/dns-query") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceHighlight,
                                unfocusedContainerColor = SurfaceHighlight,
                                focusedBorderColor = AccentRed,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Sezione Domini Provider Personalizzati
            item {
                Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Domini e URL delle Sorgenti di Streaming", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Puoi modificare a mano il dominio attivo se un sito cambia estensione o viene oscurato.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = scDomain,
                                onValueChange = { scDomain = it },
                                label = { Text("StreamingCommunity Dominio") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = SurfaceHighlight, unfocusedContainerColor = SurfaceHighlight)
                            )

                            OutlinedTextField(
                                value = cb01Domain,
                                onValueChange = { cb01Domain = it },
                                label = { Text("CB01 Dominio") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = SurfaceHighlight, unfocusedContainerColor = SurfaceHighlight)
                            )

                            OutlinedTextField(
                                value = altaDomain,
                                onValueChange = { altaDomain = it },
                                label = { Text("Altadefinizione Dominio") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = SurfaceHighlight, unfocusedContainerColor = SurfaceHighlight)
                            )

                            OutlinedTextField(
                                value = animeWorldDomain,
                                onValueChange = { animeWorldDomain = it },
                                label = { Text("AnimeWorld Dominio") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = SurfaceHighlight, unfocusedContainerColor = SurfaceHighlight)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Importa Lista Domini
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Importa Lista Domini (JSON)") },
            text = {
                Column {
                    Text(
                        "Incolla una mappa JSON di provider e nuovi domini, ad esempio:\n{\n  \"StreamingCommunity\": \"streamingunity.cc\",\n  \"CB01\": \"cb01uno.homes\"\n}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        placeholder = { Text("{\"StreamingCommunity\": \"...\"}") },
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = SurfaceHighlight, unfocusedContainerColor = SurfaceHighlight)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        UserPreferences.importCustomDomains(importJsonText)
                        scDomain = UserPreferences.streamingcommunityDomain
                        cb01Domain = UserPreferences.getCustomProviderDomain("CB01", "cb01uno.homes")
                        altaDomain = UserPreferences.getCustomProviderDomain("Altadefinizione01", "altadefinizione01.baby")
                        animeWorldDomain = UserPreferences.getCustomProviderDomain("AnimeWorld", "animeworld.so")
                        guardaSerieDomain = UserPreferences.getCustomProviderDomain("GuardaSerie", "guardaserie.skin")
                        eurostreamingDomain = UserPreferences.getCustomProviderDomain("Eurostreaming", "eurostreaming.quest")
                        showImportDialog = false
                        savedMessage = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Importa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Annulla", color = TextSecondary)
                }
            }
        )
    }
}
