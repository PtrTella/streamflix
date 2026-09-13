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
                        val unified = toUnified(item)
                        mergeOrAdd(list, unified)
                    }
                    is TvShow -> {
                        val unified = toUnified(item)
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
                    provider.search(query)
                }.getOrDefault(emptyList())
            }
        }.awaitAll().flatten()

        val unifiedList = mutableListOf<UnifiedMedia>()
        for (item in results) {
            when (item) {
                is Movie -> mergeOrAdd(unifiedList, toUnified(item))
                is TvShow -> mergeOrAdd(unifiedList, toUnified(item))
            }
        }
        unifiedList
    }

    private fun normalizeTitle(title: String): String {
        return title.lowercase(Locale.ROOT)
            .replace(Regex("""\((19|20)\d{2}\)"""), "")
            .replace(Regex("""[^a-z0-9]"""), "")
            .trim()
    }

    private fun mergeOrAdd(list: MutableList<UnifiedMedia>, item: UnifiedMedia) {
        val normalized = normalizeTitle(item.title)
        val existing = list.find { normalizeTitle(it.title) == normalized && it.isTvShow == item.isTvShow }
        if (existing != null) {
            item.sources.forEach { source ->
                if (existing.sources.none { it.providerName == source.providerName }) {
                    existing.sources.add(source)
                }
            }
        } else {
            list.add(item)
        }
    }

    private fun toUnified(movie: Movie): UnifiedMedia {
        val year = movie.released?.get(java.util.Calendar.YEAR)?.toString()
        val providerName = movie.providerName ?: "StreamingCommunity"
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

    private fun toUnified(tvShow: TvShow): UnifiedMedia {
        val year = tvShow.released?.get(java.util.Calendar.YEAR)?.toString()
        val providerName = tvShow.providerName ?: "StreamingCommunity"
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
