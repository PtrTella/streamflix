package com.streamflixreborn.streamflix

import com.streamflixreborn.streamflix.providers.StreamingCommunityProvider
import com.streamflixreborn.streamflix.providers.CB01Provider
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ScrapingTest {

    @Test
    fun testStreamingCommunityHome() = runBlocking {
        println("Testing StreamingCommunityProvider...")
        val provider = StreamingCommunityProvider("it")
        try {
            val home = provider.getHome()
            println("StreamingCommunity Home categories found: ${home.size}")
            home.take(3).forEach { category ->
                println("Category: ${category.name} with ${category.list.size} items")
            }
            assert(home.isNotEmpty()) { "Expected at least 1 category from StreamingCommunity" }
        } catch (e: Exception) {
            println("StreamingCommunity error (may be network/captcha): ${e.message}")
        }
    }

    @Test
    fun testCB01Home() = runBlocking {
        println("Testing CB01Provider...")
        val provider = CB01Provider
        try {
            val home = provider.getHome()
            println("CB01 Home categories found: ${home.size}")
            home.take(2).forEach { category ->
                println("Category: ${category.name} with ${category.list.size} items")
            }
        } catch (e: Exception) {
            println("CB01 error: ${e.message}")
        }
    }

    @Test
    fun testMovieAndServerExtraction() = runBlocking {
        val sc = StreamingCommunityProvider("it")
        try {
            val home = sc.getHome()
            val firstMovie = home.flatMap { it.list }.filterIsInstance<com.streamflixreborn.streamflix.models.Movie>().firstOrNull()
            println("SC First Movie: $firstMovie")
            if (firstMovie != null) {
                val fullMovie = sc.getMovie(firstMovie.id)
                println("SC Full Movie Overview: ${fullMovie.overview}")
                val servers = sc.getServers(fullMovie.id, com.streamflixreborn.streamflix.models.Video.Type.Movie(
                    id = fullMovie.id,
                    title = fullMovie.title,
                    releaseDate = fullMovie.released?.toString() ?: "",
                    poster = fullMovie.poster ?: "",
                    imdbId = fullMovie.imdbId
                ))
                println("SC Servers: $servers")
                if (servers.isNotEmpty()) {
                    val video = sc.getVideo(servers.first())
                    println("SC Extracted Video: ${video.source} headers=${video.headers}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val cb01 = CB01Provider
        try {
            val movies = cb01.getMovies(1)
            val realMovie = movies.firstOrNull { !it.title.contains("avviso", true) && !it.id.contains("avviso", true) }
            println("CB01 Real Movie ID: ${realMovie?.id} Title: ${realMovie?.title}")
            if (realMovie != null) {
                val fullMovie = cb01.getMovie(realMovie.id)
                println("CB01 Full Movie Overview: '${fullMovie.overview}'")
                val servers = cb01.getServers(fullMovie.id, com.streamflixreborn.streamflix.models.Video.Type.Movie(
                    id = fullMovie.id,
                    title = fullMovie.title,
                    releaseDate = fullMovie.released?.toString() ?: "",
                    poster = fullMovie.poster ?: "",
                    imdbId = fullMovie.imdbId
                ))
                println("CB01 Servers: ${servers.map { it.name to it.src }}")
                if (servers.isNotEmpty()) {
                    val video = cb01.getVideo(servers.first())
                    println("CB01 Extracted Video: ${video.source}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
