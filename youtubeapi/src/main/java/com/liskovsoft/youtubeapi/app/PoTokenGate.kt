package com.liskovsoft.youtubeapi.app

import com.liskovsoft.sharedutils.mylogger.Log
import com.liskovsoft.youtubeapi.app.potoken.PoTokenService
import com.liskovsoft.youtubeapi.app.potokennp2.PoTokenProviderImpl
import com.liskovsoft.youtubeapi.app.potokennp2.core.PoTokenBindingSelector
import com.liskovsoft.youtubeapi.app.potokennp2.core.PoTokenContext
import com.liskovsoft.youtubeapi.app.potokennp2.core.PoTokenResult
import com.liskovsoft.youtubeapi.app.potokennp2.misc.selectFactory
import com.liskovsoft.youtubeapi.app.potokennp2.visitor.VisitorService
import com.liskovsoft.youtubeapi.common.helpers.AppClient
import com.liskovsoft.youtubeapi.innertube.ytcfg.YtCfgService
import com.liskovsoft.youtubeapi.app.potokencloud.PoTokenCloudService as LegacyPoTokenCloudService
import com.liskovsoft.youtubeapi.app.potokencloud2.PoTokenCloudService as BgUtilsPoTokenCloudService

/** Coordinates client-specific player, GVS, and subtitle proof-of-origin tokens. */
internal object PoTokenGate {
    private const val FALLBACK_TOKEN_LIFETIME_MS = 6 * 60 * 60 * 1_000L
    private val TAG = PoTokenGate::class.simpleName
    private var webPoToken: PoTokenResult? = null
    private var cachedClient: AppClient? = null
    private var cacheUsesLocalGenerator = false
    private var cacheExpiresAtMs: Long = -1
    private var cacheResetTimeMs: Long = -1
    private val fallbackChain = PoTokenFallbackChain(
        listOf(
            BgUtilsPoTokenCloudService::getPoToken,
            LegacyPoTokenCloudService::getPoToken
        )
    )

    init {
        PoTokenProviderImpl.poTokenFactory = selectFactory()
    }

    @JvmStatic
    @Synchronized
    fun getTokenResult(client: AppClient, videoId: String): PoTokenResult? {
        if (!client.isWebPoTokenSupported || videoId.isBlank()) {
            return null
        }

        webPoToken?.let {
            if (cachedClient == client && it.videoId == videoId && !isWebPotExpired()) {
                return it
            }
        }

        val visitorData = PoTokenProviderImpl.getOrCreateWebVisitorData()
            ?: VisitorService.getVisitorData()
            ?: AppService.instance().visitorData
            ?: return null
        val gvsBindToVideoId = YtCfgService.isGvsPoTokenContentBound(client, videoId)
        val playerBinding = PoTokenBindingSelector.select(
            PoTokenContext.PLAYER,
            videoId,
            visitorData
        ) ?: return null
        val streamingBinding = PoTokenBindingSelector.select(
            PoTokenContext.GVS,
            videoId,
            visitorData,
            gvsBindToVideoId = gvsBindToVideoId
        ) ?: return null

        val localResult = try {
            PoTokenProviderImpl.getWebClientPoToken(
                videoId,
                visitorData,
                streamingBinding.value
            )
        } catch (error: RuntimeException) {
            Log.e(TAG, "Local poToken generation failed; trying configured fallbacks (${error.javaClass.simpleName})")
            null
        }

        val result = localResult ?: createFallbackResult(
            videoId,
            visitorData,
            playerBinding.value,
            streamingBinding.value
        ) ?: return null

        webPoToken = result
        cachedClient = client
        cacheUsesLocalGenerator = localResult != null
        cacheExpiresAtMs = System.currentTimeMillis() + FALLBACK_TOKEN_LIFETIME_MS
        Log.d(
            TAG,
            "Prepared WEB poTokens; client=${client.name}, GVS binding=${streamingBinding.type.name}, " +
                    "local=${localResult != null}"
        )
        return result
    }

    private fun createFallbackResult(
        videoId: String,
        visitorData: String,
        playerBinding: String,
        streamingBinding: String
    ): PoTokenResult? {
        val playerToken = fallbackChain.getToken(playerBinding) ?: return null
        val streamingToken = if (streamingBinding == playerBinding) {
            playerToken
        } else {
            fallbackChain.getToken(streamingBinding) ?: return null
        }

        return PoTokenResult(videoId, visitorData, playerToken, streamingToken, streamingBinding)
    }

    @JvmStatic
    fun getPlayerRequestPoToken(client: AppClient, videoId: String): String? =
        if (client.isPlayerPoTokenRequired) getTokenResult(client, videoId)?.playerRequestPoToken else null

    @JvmStatic
    fun getStreamingDataPoToken(client: AppClient, videoId: String): String? =
        getTokenResult(client, videoId)?.streamingDataPoToken

    /** Compatibility API: a video ID always denotes a player-request token. */
    @JvmStatic
    @JvmOverloads
    fun getPoToken(client: AppClient, videoId: String? = null): String? =
        if (videoId == null) webPoToken?.streamingDataPoToken
        else getPlayerRequestPoToken(client, videoId)

    @JvmStatic
    fun getColdStartPoToken(client: AppClient, videoId: String): String? =
        if (client.isPlayerPoTokenRequired) PoTokenService.generateColdStartToken(videoId) else null

    @JvmStatic
    fun getVisitorData(client: AppClient): String? =
        if (client.isWebPoTokenSupported) webPoToken?.visitorData else null

    @JvmStatic
    fun isWebPotSupported() = PoTokenProviderImpl.isWebPotSupported

    @JvmStatic
    fun isWebPotExpired(): Boolean {
        val result = webPoToken ?: return true
        return result.streamingDataPoToken == null ||
                System.currentTimeMillis() >= cacheExpiresAtMs ||
                (cacheUsesLocalGenerator && PoTokenProviderImpl.isWebPotExpired)
    }

    @JvmStatic
    fun resetCache(client: AppClient): Boolean =
        if (client.isWebPoTokenSupported) resetWebCache() else false

    @JvmStatic
    fun resetCache() {
        resetWebCache()
    }

    private fun resetWebCache(): Boolean {
        val currentTimeMs = System.currentTimeMillis()
        if (currentTimeMs < cacheResetTimeMs) {
            return false
        }

        webPoToken = null
        cachedClient = null
        cacheUsesLocalGenerator = false
        cacheExpiresAtMs = -1
        PoTokenProviderImpl.resetCache()
        BgUtilsPoTokenCloudService.resetCache()
        LegacyPoTokenCloudService.resetCache()
        cacheResetTimeMs = currentTimeMs + 60_000
        return true
    }
}
