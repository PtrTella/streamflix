package com.streamflixreborn.streamflix.utils

import android.util.Log
import com.streamflixreborn.streamflix.BuildConfig
import com.streamflixreborn.streamflix.models.Provider
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object UserPreferences {

    private const val TAG = "UserPreferences"

    // Default DoH Provider URL (Cloudflare)
    private const val DEFAULT_DOH_PROVIDER_URL = "https://cloudflare-dns.com/dns-query"
    const val DOH_DISABLED_VALUE = ""
    private const val DEFAULT_SERIENSTREAM_DOMAIN = "s.to"
    private const val DEFAULT_MOFLIX_DOMAIN = "moflix-stream.xyz"
    private const val DEFAULT_STREAMINGCOMMUNITY_DOMAIN = "streamingunity.win"
    private const val DEFAULT_REMOTE_DOMAINS_URL = "https://pastebin.com/raw/KgQ4jTy6"
    private const val DEFAULT_CUEVANA_DOMAIN = "cuevana.gs"
    private const val DEFAULT_POSEIDON_DOMAIN = "www.poseidonhd2.co"

    const val PROVIDER_URL = "URL"
    const val PROVIDER_LOGO = "LOGO"
    const val PROVIDER_PORTAL_URL = "PORTAL_URL"
    const val PROVIDER_AUTOUPDATE = "AUTOUPDATE_URL"
    const val PROVIDER_NEW_INTERFACE = "NEW_INTERFACE"
    const val PROVIDER_PREFERRED_SERVER = "PREFERRED_SERVER"

    private val storage = ConcurrentHashMap<String, Any>()
    var providerCache = JSONObject()

    private val configFile: File by lazy {
        val appSupport = File(System.getProperty("user.home"), "Library/Application Support/StreamFlix")
        if (!appSupport.exists()) appSupport.mkdirs()
        File(appSupport, "preferences.json")
    }

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        try {
            if (configFile.exists()) {
                val json = JSONObject(configFile.readText(Charsets.UTF_8))
                json.keys().forEach { key ->
                    storage[key] = json.get(key)
                }
                val cached = json.optString("PROVIDER_CACHE", "{}")
                providerCache = runCatching { JSONObject(cached) }.getOrDefault(JSONObject())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading preferences: ${e.message}")
        }
    }

    private fun savePreferences() {
        try {
            val json = JSONObject(storage as Map<*, *>)
            json.put("PROVIDER_CACHE", providerCache.toString())
            configFile.writeText(json.toString(2), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving preferences: ${e.message}")
        }
    }

    fun getProviderCache(provider: Any, key: String): String {
        val name = if (provider is com.streamflixreborn.streamflix.providers.Provider) provider.name else provider.toString()
        return providerCache.optJSONObject(name)?.optString(key).orEmpty()
    }

    fun setProviderCache(provider: Any?, key: String, value: String) {
        val name = when (provider) {
            is com.streamflixreborn.streamflix.providers.Provider -> provider.name
            else -> provider?.toString() ?: return
        }
        val inner = providerCache.optJSONObject(name) ?: JSONObject().also { providerCache.put(name, it) }
        inner.put(key, value)
        savePreferences()
    }

    fun clearProviderCache(providerName: String) {
        if (providerCache.has(providerName)) {
            providerCache.remove(providerName)
            savePreferences()
        }
    }

    var currentProvider: com.streamflixreborn.streamflix.providers.Provider? = null

    var serverAutoSubtitlesDisabled: Boolean
        get() = storage["SERVER_AUTO_SUBTITLES_DISABLED"] as? Boolean ?: true
        set(value) {
            storage["SERVER_AUTO_SUBTITLES_DISABLED"] = value
            savePreferences()
        }

    var providerLanguage: String?
        get() = storage["PROVIDER_LANGUAGE"] as? String ?: "it"
        set(value) {
            if (value != null) storage["PROVIDER_LANGUAGE"] = value else storage.remove("PROVIDER_LANGUAGE")
            savePreferences()
        }

    var currentLanguage: String?
        get() = storage["CURRENT_LANGUAGE"] as? String ?: "it"
        set(value) {
            if (value != null) storage["CURRENT_LANGUAGE"] = value else storage.remove("CURRENT_LANGUAGE")
            savePreferences()
        }

    var tmdbApiKey: String
        get() = storage["TMDB_API_KEY"] as? String ?: BuildConfig.TMDB_API_KEY
        set(value) {
            storage["TMDB_API_KEY"] = value
            savePreferences()
        }

    var enableTmdb: Boolean
        get() = storage["ENABLE_TMDB"] as? Boolean ?: true
        set(value) {
            storage["ENABLE_TMDB"] = value
            savePreferences()
        }

    var subdlApiKey: String
        get() = storage["SUBDL_API_KEY"] as? String ?: BuildConfig.SUBDL_API_KEY
        set(value) {
            storage["SUBDL_API_KEY"] = value
            savePreferences()
        }

    var dohProviderUrl: String
        get() = storage["DOH_PROVIDER_URL"] as? String ?: DEFAULT_DOH_PROVIDER_URL
        set(value) {
            storage["DOH_PROVIDER_URL"] = value
            savePreferences()
            DnsResolver.setDnsUrl(value)
        }

    var streamingcommunityDomain: String
        get() = storage["STREAMINGCOMMUNITY_DOMAIN"] as? String ?: DEFAULT_STREAMINGCOMMUNITY_DOMAIN
        set(value) {
            storage["STREAMINGCOMMUNITY_DOMAIN"] = value
            clearProviderCache("StreamingCommunity")
            savePreferences()
        }

    var serienstreamDomain: String
        get() = storage["SERIENSTREAM_DOMAIN"] as? String ?: DEFAULT_SERIENSTREAM_DOMAIN
        set(value) {
            storage["SERIENSTREAM_DOMAIN"] = value
            clearProviderCache("SerienStream")
            savePreferences()
        }

    var cuevanaDomain: String
        get() = storage["CUEVANA_DOMAIN"] as? String ?: DEFAULT_CUEVANA_DOMAIN
        set(value) {
            storage["CUEVANA_DOMAIN"] = value
            clearProviderCache("Cuevana 3")
            savePreferences()
        }

    var poseidonDomain: String
        get() = storage["POSEIDON_DOMAIN"] as? String ?: DEFAULT_POSEIDON_DOMAIN
        set(value) {
            storage["POSEIDON_DOMAIN"] = value
            clearProviderCache("Poseidonhd2")
            savePreferences()
        }

    var moflixDomain: String
        get() = storage["MOFLIX_DOMAIN"] as? String ?: DEFAULT_MOFLIX_DOMAIN
        set(value) {
            storage["MOFLIX_DOMAIN"] = value
            savePreferences()
        }

    // Dynamic custom domains for any provider
    fun getCustomProviderDomain(providerName: String, defaultDomain: String): String {
        return storage["CUSTOM_DOMAIN_$providerName"] as? String ?: defaultDomain
    }

    fun setCustomProviderDomain(providerName: String, domain: String) {
        if (domain.isBlank()) {
            storage.remove("CUSTOM_DOMAIN_$providerName")
        } else {
            storage["CUSTOM_DOMAIN_$providerName"] = domain.trim()
        }
        clearProviderCache(providerName)
        savePreferences()
    }

    var remoteDomainsUrl: String
        get() = storage["REMOTE_DOMAINS_URL"] as? String ?: DEFAULT_REMOTE_DOMAINS_URL
        set(value) {
            storage["REMOTE_DOMAINS_URL"] = value.trim()
            savePreferences()
        }

    fun cleanDomain(raw: String): String {
        return raw.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore("/")
            .trim()
    }

    fun importCustomDomains(domainsJson: String): Int {
        var count = 0
        try {
            val trimmed = domainsJson.trim()
            if (trimmed.startsWith("{")) {
                val json = JSONObject(trimmed)
                json.keys().forEach { key ->
                    val rawVal = json.optString(key, "")
                    if (rawVal.isNotBlank()) {
                        val cleaned = cleanDomain(rawVal)
                        setCustomProviderDomain(key, cleaned)
                        if (key.equals("StreamingCommunity", ignoreCase = true)) {
                            streamingcommunityDomain = cleaned
                        }
                        count++
                    }
                }
            } else if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val name = obj.optString("name").ifEmpty { obj.optString("provider") }
                    val domain = obj.optString("domain").ifEmpty { obj.optString("url") }
                    if (name.isNotBlank() && domain.isNotBlank()) {
                        val cleaned = cleanDomain(domain)
                        setCustomProviderDomain(name, cleaned)
                        if (name.equals("StreamingCommunity", ignoreCase = true)) {
                            streamingcommunityDomain = cleaned
                        }
                        count++
                    }
                }
            } else {
                // Line-by-line list of URLs (e.g. Pastebin plain text list)
                val lines = trimmed.lines().map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("#") }
                lines.forEach { line ->
                    val cleaned = cleanDomain(line)
                    if (cleaned.isNotBlank()) {
                        val lower = cleaned.lowercase()
                        when {
                            lower.contains("streamingcommunity") || lower.contains("streamingunity") -> {
                                setCustomProviderDomain("StreamingCommunity", cleaned)
                                streamingcommunityDomain = cleaned
                                count++
                            }
                            lower.contains("cb01") -> {
                                setCustomProviderDomain("CB01", cleaned)
                                count++
                            }
                            lower.contains("altadefinizione") -> {
                                setCustomProviderDomain("Altadefinizione01", cleaned)
                                count++
                            }
                            lower.contains("animeworld") -> {
                                setCustomProviderDomain("AnimeWorld", cleaned)
                                count++
                            }
                            lower.contains("guardaserie") -> {
                                setCustomProviderDomain("GuardaSerie", cleaned)
                                count++
                            }
                            lower.contains("eurostreaming") -> {
                                setCustomProviderDomain("Eurostreaming", cleaned)
                                count++
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import custom domains: ${e.message}")
        }
        return count
    }

    suspend fun syncRemoteDomains(): Boolean {
        val url = remoteDomainsUrl.takeIf { it.isNotBlank() } ?: return false
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .dns(DnsResolver.doh)
                    .build()
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 StreamFlix-macOS")
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()?.trim().orEmpty()
                    if (body.isNotBlank()) {
                        val imported = importCustomDomains(body)
                        return@withContext imported > 0
                    }
                }
                false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync remote domains: ${e.message}")
                false
            }
        }
    }

    var favoriteProviders: Set<String>
        get() {
            val raw = storage["FAVORITE_PROVIDERS"] as? List<*> ?: listOf("StreamingCommunity", "CB01", "Altadefinizione01", "AnimeWorld")
            return raw.mapNotNull { it as? String }.toSet()
        }
        set(value) {
            storage["FAVORITE_PROVIDERS"] = value.toList()
            savePreferences()
        }

    var autoplay: Boolean
        get() = storage["AUTOPLAY"] as? Boolean ?: true
        set(value) {
            storage["AUTOPLAY"] = value
            savePreferences()
        }
}
