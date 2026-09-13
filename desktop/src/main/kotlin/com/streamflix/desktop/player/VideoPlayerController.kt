package com.streamflix.desktop.player

import com.streamflixreborn.streamflix.models.Video
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import java.io.File

object VideoPlayerController {

    private var isVlcLibConfigured = false

    fun configureVlcDiscovery() {
        if (isVlcLibConfigured) return
        val vlcAppLib = File("/Applications/VLC.app/Contents/MacOS/lib")
        if (vlcAppLib.exists()) {
            System.setProperty("jna.library.path", vlcAppLib.absolutePath)
            System.setProperty("VLC_PLUGIN_PATH", "/Applications/VLC.app/Contents/MacOS/plugins")
        }
        NativeDiscovery().discover()
        isVlcLibConfigured = true
    }

    fun isVlcAvailable(): Boolean {
        return File("/Applications/VLC.app").exists()
    }

    fun isIinaAvailable(): Boolean {
        return File("/Applications/IINA.app").exists()
    }

    fun launchExternalPlayer(video: Video, serverName: String = "") {
        val streamUrl = video.source
        val headers = video.headers ?: emptyMap()
        val referer = headers["Referer"] ?: headers["referer"] ?: ""
        val userAgent = headers["User-Agent"] ?: headers["user-agent"] ?: ""

        val vlcBinary = File("/Applications/VLC.app/Contents/MacOS/VLC")
        val iinaBinary = File("/Applications/IINA.app/Contents/MacOS/IINA")

        when {
            iinaBinary.exists() -> {
                val cmd = mutableListOf("/usr/bin/open", "-a", "IINA", streamUrl)
                if (referer.isNotBlank()) {
                    cmd.addAll(listOf("--args", "--mpv-referrer=$referer"))
                }
                ProcessBuilder(cmd).start()
            }
            vlcBinary.exists() -> {
                val cmd = mutableListOf(vlcBinary.absolutePath)
                if (referer.isNotBlank()) cmd.add("--http-referrer=$referer")
                if (userAgent.isNotBlank()) cmd.add("--http-user-agent=$userAgent")
                cmd.add(streamUrl)
                ProcessBuilder(cmd).start()
            }
            else -> {
                // Fallback to macOS open
                ProcessBuilder("open", streamUrl).start()
            }
        }
    }

    fun createMediaPlayerComponent(video: Video): EmbeddedMediaPlayerComponent {
        configureVlcDiscovery()
        val component = EmbeddedMediaPlayerComponent()
        val streamUrl = video.source
        val headers = video.headers ?: emptyMap()
        val referer = headers["Referer"] ?: headers["referer"] ?: ""
        val userAgent = headers["User-Agent"] ?: headers["user-agent"] ?: ""

        val options = mutableListOf<String>()
        if (referer.isNotBlank()) options.add(":http-referrer=$referer")
        if (userAgent.isNotBlank()) options.add(":http-user-agent=$userAgent")

        component.mediaPlayer().media().play(streamUrl, *options.toTypedArray())
        return component
    }
}
