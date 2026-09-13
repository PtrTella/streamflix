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
}
