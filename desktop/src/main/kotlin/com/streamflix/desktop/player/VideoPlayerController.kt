package com.streamflix.desktop.player

import com.streamflixreborn.streamflix.models.Video
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import java.io.File

object VideoPlayerController {

    private var isVlcLibConfigured: Boolean? = null

    /**
     * Checks if /Applications/VLC.app contains native dylibs matching the current JVM architecture.
     * On Apple Silicon Macs (aarch64/arm64), an Intel (x86_64) libvlccore cannot be loaded in-process.
     */
    fun isVlcDylibCompatible(): Boolean {
        val dylib = File("/Applications/VLC.app/Contents/MacOS/lib/libvlccore.dylib")
        if (!dylib.exists()) return false

        val osArch = System.getProperty("os.arch")?.lowercase() ?: ""
        val isArmHost = osArch.contains("aarch64") || osArch.contains("arm64")

        return try {
            val proc = ProcessBuilder("file", dylib.absolutePath).start()
            val output = proc.inputStream.bufferedReader().readText()
            proc.waitFor()
            if (isArmHost) {
                output.contains("arm64")
            } else {
                output.contains("x86_64")
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun configureVlcDiscovery(): Boolean {
        if (isVlcLibConfigured != null) return isVlcLibConfigured == true
        if (!isVlcDylibCompatible()) {
            isVlcLibConfigured = false
            return false
        }
        return try {
            val vlcAppLib = File("/Applications/VLC.app/Contents/MacOS/lib")
            if (vlcAppLib.exists()) {
                System.setProperty("jna.library.path", vlcAppLib.absolutePath)
                System.setProperty("VLC_PLUGIN_PATH", "/Applications/VLC.app/Contents/MacOS/plugins")
            }
            val discovered = NativeDiscovery().discover()
            isVlcLibConfigured = discovered
            discovered
        } catch (_: Throwable) {
            isVlcLibConfigured = false
            false
        }
    }

    fun isVlcAvailable(): Boolean {
        return File("/Applications/VLC.app").exists()
    }

    fun isIinaAvailable(): Boolean {
        return File("/Applications/IINA.app").exists()
    }

    private fun resolveStreamUrl(streamUrl: String): String {
        return if (streamUrl.startsWith("data:")) {
            try {
                val base64Data = streamUrl.substringAfter("base64,")
                val bytes = java.util.Base64.getDecoder().decode(base64Data)
                val tempFile = File.createTempFile("streamflix_stream_", ".m3u8")
                tempFile.deleteOnExit()
                tempFile.writeBytes(bytes)
                tempFile.absolutePath
            } catch (_: Exception) {
                streamUrl
            }
        } else {
            streamUrl
        }
    }

    fun launchExternalPlayer(video: Video, serverName: String = "") {
        val streamUrl = resolveStreamUrl(video.source)
        val headers = video.headers ?: emptyMap()
        val referer = headers["Referer"] ?: headers["referer"] ?: ""
        val userAgent = headers["User-Agent"] ?: headers["user-agent"] ?: ""

        val effReferer = when {
            referer.isNotBlank() -> referer
            streamUrl.contains("mxcontent.net") -> "https://mixdrop.co/"
            streamUrl.contains("vixcloud.co") -> "https://vixcloud.co/"
            streamUrl.contains("maxstream.video") -> "https://maxstream.video/"
            else -> ""
        }
        val effUserAgent = when {
            userAgent.isNotBlank() -> userAgent
            else -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        }

        val vlcBinary = File("/Applications/VLC.app/Contents/MacOS/VLC")
        val iinaBinary = File("/Applications/IINA.app/Contents/MacOS/IINA")

        when {
            iinaBinary.exists() -> {
                // IINA is native Swift Apple Silicon and performs exceptionally well
                val cmd = mutableListOf("/usr/bin/open", "-a", "IINA", streamUrl)
                if (effReferer.isNotBlank()) {
                    cmd.addAll(listOf("--args", "--mpv-referrer=$effReferer"))
                }
                ProcessBuilder(cmd).start()
            }
            vlcBinary.exists() -> {
                // VLC binary runs smoothly as an external standalone process
                val cmd = mutableListOf(vlcBinary.absolutePath, streamUrl)
                if (effReferer.isNotBlank()) cmd.add(":http-referrer=$effReferer")
                if (effUserAgent.isNotBlank()) cmd.add(":http-user-agent=$effUserAgent")
                ProcessBuilder(cmd).start()
            }
            else -> {
                // Fallback to default macOS URL opener
                ProcessBuilder("/usr/bin/open", streamUrl).start()
            }
        }
    }

    fun createMediaPlayerComponent(video: Video): EmbeddedMediaPlayerComponent? {
        if (!configureVlcDiscovery()) return null
        return try {
            val component = EmbeddedMediaPlayerComponent()
            val streamUrl = resolveStreamUrl(video.source)
            val headers = video.headers ?: emptyMap()
            val referer = headers["Referer"] ?: headers["referer"] ?: ""
            val userAgent = headers["User-Agent"] ?: headers["user-agent"] ?: ""

            val effReferer = when {
                referer.isNotBlank() -> referer
                streamUrl.contains("mxcontent.net") -> "https://mixdrop.co/"
                streamUrl.contains("vixcloud.co") -> "https://vixcloud.co/"
                streamUrl.contains("maxstream.video") -> "https://maxstream.video/"
                else -> ""
            }
            val effUserAgent = when {
                userAgent.isNotBlank() -> userAgent
                else -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
            }

            val options = mutableListOf<String>()
            if (effReferer.isNotBlank()) options.add(":http-referrer=$effReferer")
            if (effUserAgent.isNotBlank()) options.add(":http-user-agent=$effUserAgent")

            component.mediaPlayer().media().play(streamUrl, *options.toTypedArray())
            component
        } catch (_: Throwable) {
            null
        }
    }
}
