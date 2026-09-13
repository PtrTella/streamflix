package android.net

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class Uri private constructor(private val rawString: String, private val uri: URI?) {

    val scheme: String? get() = uri?.scheme
    val host: String? get() = uri?.host
    val path: String? get() = uri?.path
    val query: String? get() = uri?.query
    val encodedQuery: String? get() = uri?.rawQuery
    val port: Int get() = uri?.port ?: -1
    val pathSegments: List<String> get() = path?.split("/")?.filter { it.isNotEmpty() } ?: emptyList()

    fun resolve(relative: String): Uri? {
        return try {
            if (uri != null) {
                val resolved = uri.resolve(relative)
                Uri(resolved.toString(), resolved)
            } else {
                parse(relative)
            }
        } catch (_: Exception) {
            parse(relative)
        }
    }

    fun getQueryParameter(key: String): String? {
        val q = encodedQuery ?: return null
        val pairs = q.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            val k = if (idx > 0) decode(pair.substring(0, idx)) else decode(pair)
            if (k == key) {
                return if (idx > 0 && pair.length > idx + 1) decode(pair.substring(idx + 1)) else ""
            }
        }
        return null
    }

    override fun toString(): String = rawString

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Uri) return false
        return rawString == other.rawString
    }

    override fun hashCode(): Int = rawString.hashCode()

    companion object {
        fun parse(uriString: String): Uri {
            val parsedUri = try {
                URI(uriString)
            } catch (_: Exception) {
                try {
                    val sanitized = uriString.replace(" ", "%20")
                    URI(sanitized)
                } catch (_: Exception) {
                    null
                }
            }
            return Uri(uriString, parsedUri)
        }

        fun encode(s: String?): String {
            if (s == null) return ""
            return URLEncoder.encode(s, StandardCharsets.UTF_8.name())
        }

        fun decode(s: String?): String {
            if (s == null) return ""
            return URLDecoder.decode(s, StandardCharsets.UTF_8.name())
        }
    }
}
