package com.liskovsoft.youtubeapi.innertube.ytcfg

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.liskovsoft.googlecommon.common.helpers.RetrofitHelper
import com.liskovsoft.youtubeapi.common.helpers.AppClient
import com.liskovsoft.youtubeapi.innertube.utils.URLS
import com.liskovsoft.youtubeapi.innertube.utils.traverseObj

internal object YtCfgService {
    private const val PLAYER_URL_CACHE_LIFETIME_MS = 10 * 60 * 60 * 1_000L
    private const val EMBEDDED_CONTEXT = "WEB_PLAYER_CONTEXT_CONFIG_ID_EMBEDDED_PLAYER"
    private const val WATCH_CONTEXT = "WEB_PLAYER_CONTEXT_CONFIG_ID_KEVLAR_WATCH"
    private val api by lazy { RetrofitHelper.create(YtCfgApi::class.java) }
    private var cachedEncryptedHostFlags: String? = null
    private val cachedPlayerConfigs = mutableMapOf<AppClient, CachedPlayerConfig>()

    fun getCachedEncryptedHostFlags(videoId: String?): String? {
        return cachedEncryptedHostFlags ?: getEncryptedHostFlags(videoId)?.also { cachedEncryptedHostFlags = it }
    }

    fun getEncryptedHostFlags(videoId: String?): String? {
        if (videoId == null)
            return null

        val ytCfg = downloadYtCfg(AppClient.WEB_EMBED, videoId)

        return traverseObj(
            ytCfg,
            "WEB_PLAYER_CONTEXT_CONFIGS",
            "WEB_PLAYER_CONTEXT_CONFIG_ID_EMBEDDED_PLAYER",
            "encryptedHostFlags"
        )?.asString
    }

    @Synchronized
    fun getPlayerUrl(client: AppClient, videoId: String?): String? {
        return getPlayerConfig(client, videoId)?.playerUrl
    }

    @Synchronized
    fun isGvsPoTokenContentBound(client: AppClient, videoId: String?): Boolean {
        return getPlayerConfig(client, videoId)?.gvsPoTokenContentBound ?: false
    }

    internal fun extractPlayerUrl(ytCfg: JsonObject?, client: AppClient): String? {
        val preferredContext = if (client.isEmbedded) EMBEDDED_CONTEXT else WATCH_CONTEXT
        val jsUrl = traverseObj(
            ytCfg,
            "WEB_PLAYER_CONTEXT_CONFIGS",
            preferredContext,
            "jsUrl"
        )?.asString ?: traverseObj(ytCfg, "...", "jsUrl")?.asString

        return when {
            jsUrl == null -> null
            jsUrl.startsWith("//") -> "https:$jsUrl"
            jsUrl.startsWith("/") -> "${URLS.YT_BASE}$jsUrl"
            jsUrl.startsWith("http://") || jsUrl.startsWith("https://") -> jsUrl
            else -> "${URLS.YT_BASE}/$jsUrl"
        }
    }

    internal fun extractGvsPoTokenBinding(ytCfg: JsonObject?, client: AppClient): Boolean {
        val preferredContext = if (client.isEmbedded) EMBEDDED_CONTEXT else WATCH_CONTEXT
        val serializedFlags = traverseObj(
            ytCfg,
            "WEB_PLAYER_CONTEXT_CONFIGS",
            preferredContext,
            "serializedExperimentFlags"
        )?.asString ?: return false

        return serializedFlags
            .split('&')
            .any { it == "html5_generate_content_po_token=true" }
    }

    private fun getPlayerConfig(client: AppClient, videoId: String?): CachedPlayerConfig? {
        val cached = cachedPlayerConfigs[client]
        if (cached != null && System.currentTimeMillis() - cached.timestampMs < PLAYER_URL_CACHE_LIFETIME_MS) {
            return cached
        }

        val ytCfg = downloadYtCfg(client, videoId) ?: return null
        val config = CachedPlayerConfig(
            timestampMs = System.currentTimeMillis(),
            playerUrl = extractPlayerUrl(ytCfg, client),
            gvsPoTokenContentBound = extractGvsPoTokenBinding(ytCfg, client)
        )
        if (config.playerUrl != null) {
            cachedPlayerConfigs[client] = config
        }
        return config
    }

    /**
     * https://github.com/yt-dlp/yt-dlp/blob/48a61d0f38b156785d24df628d42892441e008c4/yt_dlp/extractor/youtube/_base.py#L956
     *
     * https://github.com/yt-dlp/yt-dlp/blob/48a61d0f38b156785d24df628d42892441e008c4/yt_dlp/extractor/youtube/_video.py#L3876
     */
    private fun downloadYtCfg(client: AppClient, videoId: String?): JsonObject? {
        val configUrl = client.getRefererUrl(videoId) ?: return null
        val wrapper = api.getYtCfg(configUrl, client.userAgent)
        val ytCfgStr = RetrofitHelper.get(wrapper)

        val config = ytCfgStr?.ytCfg ?: return null
        return try {
            JsonParser().parse(config).asJsonObject
        } catch (_: RuntimeException) {
            null
        }
    }

    private data class CachedPlayerConfig(
        val timestampMs: Long,
        val playerUrl: String?,
        val gvsPoTokenContentBound: Boolean
    )
}
