package com.streamflixreborn.streamflix.aggregator

import com.streamflixreborn.streamflix.models.Category
import com.streamflixreborn.streamflix.models.Movie
import com.streamflixreborn.streamflix.models.TvShow
import com.streamflixreborn.streamflix.models.Video
import com.streamflixreborn.streamflix.providers.*
import com.streamflixreborn.streamflix.utils.UserPreferences
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.Locale

data class UnifiedMedia(
    val id: String,
    val title: String,
    val poster: String?,
    val banner: String?,
    val rating: Double?,
    val releaseYear: String?,
    val overview: String?,
    val isTvShow: Boolean,
    val sources: MutableList<MediaSource> = mutableListOf()
)

data class MediaSource(
    val providerName: String,
    val providerId: String,
    val isTvShow: Boolean
)

data class UnifiedCategory(
    val name: String,
    val items: List<UnifiedMedia>
)

data class ResolvedStream(
    val providerName: String,
    val serverName: String,
    val video: Video
)

object AggregatorService {

    // Default primary Italian and Anime providers
    val defaultProviders: List<Provider> = listOf(
        StreamingCommunityProvider("it"),
        CB01Provider,
        Altadefinizione01Provider,
        AnimeWorldProvider
    )

    fun getActiveProviders(): List<Provider> {
        val favNames = UserPreferences.favoriteProviders
        val active = defaultProviders.filter { favNames.contains(it.name) }
        return if (active.isEmpty()) defaultProviders else active
    }

    suspend fun getUnifiedHome(): List<UnifiedCategory> = coroutineScope {
        val providers = getActiveProviders()
        val deferredHomes = providers.map { provider ->
            async {
                runCatching {
                    provider.getHome()
                }.getOrDefault(emptyList())
            }
        }

        val allCategories = deferredHomes.awaitAll().flatten()
        val unifiedMap = linkedMapOf<String, MutableList<UnifiedMedia>>()

        for (cat in allCategories) {
            val catName = cat.name.trim().ifEmpty { "In Evidenza" }
            val list = unifiedMap.computeIfAbsent(catName) { mutableListOf() }
            for (item in cat.list) {
                when (item) {
                    is Movie -> {
                        val unified = toUnified(item, item.providerName ?: "StreamingCommunity")
                        mergeOrAdd(list, unified)
                    }
                    is TvShow -> {
                        val unified = toUnified(item, item.providerName ?: "StreamingCommunity")
                        mergeOrAdd(list, unified)
                    }
                }
            }
        }

        unifiedMap.map { (name, items) ->
            UnifiedCategory(name = name, items = items)
        }.filter { it.items.isNotEmpty() }
    }

    suspend fun searchGlobal(query: String): List<UnifiedMedia> = coroutineScope {
        if (query.isBlank()) return@coroutineScope emptyList()
        val providers = getActiveProviders()
        val results = providers.map { provider ->
            async {
                runCatching {
                    val res = provider.search(query)
                    res.map { item ->
                        when (item) {
                            is Movie -> {
                                if (item.providerName == null) item.providerName = provider.name
                                item
                            }
                            is TvShow -> {
                                if (item.providerName == null) item.providerName = provider.name
                                item
                            }
                            else -> item
                        }
                    }
                }.getOrDefault(emptyList())
            }
        }.awaitAll().flatten()

        val unifiedList = mutableListOf<UnifiedMedia>()
        for (item in results) {
            when (item) {
                is Movie -> mergeOrAdd(unifiedList, toUnified(item, item.providerName ?: "StreamingCommunity"))
                is TvShow -> mergeOrAdd(unifiedList, toUnified(item, item.providerName ?: "StreamingCommunity"))
            }
        }
        unifiedList
    }

    fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    fun isSimilarTitle(t1: String, t2: String): Boolean {
        val n1 = normalizeTitle(t1)
        val n2 = normalizeTitle(t2)
        if (n1 == n2) return true
        if (n1.contains(n2) || n2.contains(n1)) return true
        val maxLen = maxOf(n1.length, n2.length)
        if (maxLen <= 3) return n1 == n2
        val dist = levenshteinDistance(n1, n2)
        // Permette 1 errore per parole corte, 2 per medie, 3 per lunghe
        val allowedDist = when {
            maxLen <= 6 -> 1
            maxLen <= 12 -> 2
            else -> 3
        }
        return dist <= allowedDist
    }

    private fun normalizeTitle(title: String): String {
        return title.lowercase(Locale.ROOT)
            .replace(Regex("""\((19|20)\d{2}\)"""), "")
            .replace(Regex("""[^a-z0-9]"""), "")
            .trim()
    }

    private fun mergeOrAdd(list: MutableList<UnifiedMedia>, item: UnifiedMedia) {
        val existing = list.find { 
            it.isTvShow == item.isTvShow && isSimilarTitle(it.title, item.title) 
        }
        if (existing != null) {
            // Unisci le sinossi o dati mancanti se uno dei due ne ha di migliori
            item.sources.forEach { source ->
                if (existing.sources.none { it.providerName == source.providerName }) {
                    existing.sources.add(source)
                }
            }
        } else {
            list.add(item)
        }
    }

    private fun toUnified(movie: Movie, defaultProviderName: String = "StreamingCommunity"): UnifiedMedia {
        val year = movie.released?.get(java.util.Calendar.YEAR)?.toString()
        val providerName = movie.providerName ?: defaultProviderName
        return UnifiedMedia(
            id = movie.id,
            title = movie.title,
            poster = movie.poster,
            banner = movie.banner,
            rating = movie.rating,
            releaseYear = year,
            overview = movie.overview,
            isTvShow = false,
            sources = mutableListOf(MediaSource(providerName = providerName, providerId = movie.id, isTvShow = false))
        )
    }

    private fun toUnified(tvShow: TvShow, defaultProviderName: String = "StreamingCommunity"): UnifiedMedia {
        val year = tvShow.released?.get(java.util.Calendar.YEAR)?.toString()
        val providerName = tvShow.providerName ?: defaultProviderName
        return UnifiedMedia(
            id = tvShow.id,
            title = tvShow.title,
            poster = tvShow.poster,
            banner = tvShow.banner,
            rating = tvShow.rating,
            releaseYear = year,
            overview = tvShow.overview,
            isTvShow = true,
            sources = mutableListOf(MediaSource(providerName = providerName, providerId = tvShow.id, isTvShow = true))
        )
    }
}
