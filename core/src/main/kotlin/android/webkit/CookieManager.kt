package android.webkit

import java.util.concurrent.ConcurrentHashMap

class CookieManager {
    private val cookies = ConcurrentHashMap<String, MutableMap<String, String>>()

    fun setCookie(url: String, cookieValue: String) {
        val host = try {
            java.net.URI(url).host ?: url
        } catch (_: Exception) {
            url
        }
        val map = cookies.computeIfAbsent(host) { ConcurrentHashMap() }
        val parts = cookieValue.split(";")
        if (parts.isNotEmpty()) {
            val kv = parts[0].split("=", limit = 2)
            if (kv.isNotEmpty()) {
                val k = kv[0].trim()
                val v = if (kv.size > 1) kv[1].trim() else ""
                map[k] = v
            }
        }
    }

    fun getCookie(url: String): String? {
        val host = try {
            java.net.URI(url).host ?: url
        } catch (_: Exception) {
            url
        }
        val map = cookies[host] ?: return null
        if (map.isEmpty()) return null
        return map.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    fun removeAllCookies(callback: Any?) {
        cookies.clear()
    }

    fun flush() {}

    companion object {
        private val instance = CookieManager()
        fun getInstance(): CookieManager = instance
    }
}
