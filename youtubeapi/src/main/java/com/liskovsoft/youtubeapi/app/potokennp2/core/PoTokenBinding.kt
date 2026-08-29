package com.liskovsoft.youtubeapi.app.potokennp2.core

internal enum class PoTokenContext {
    PLAYER,
    GVS,
    SUBS
}

internal enum class PoTokenBindingType {
    VIDEO_ID,
    VISITOR_DATA,
    DATA_SYNC_ID
}

internal data class PoTokenBinding(
    val value: String,
    val type: PoTokenBindingType
)

/**
 * Selects the BotGuard content binding used by each YouTube request context.
 */
internal object PoTokenBindingSelector {
    fun select(
        context: PoTokenContext,
        videoId: String,
        visitorData: String?,
        dataSyncId: String? = null,
        authenticated: Boolean = false,
        gvsBindToVideoId: Boolean = false
    ): PoTokenBinding? {
        if (context == PoTokenContext.PLAYER || context == PoTokenContext.SUBS || gvsBindToVideoId) {
            return videoId.takeIf { it.isNotBlank() }
                ?.let { PoTokenBinding(it, PoTokenBindingType.VIDEO_ID) }
        }

        if (authenticated) {
            return dataSyncId?.takeIf { it.isNotBlank() }
                ?.let { PoTokenBinding(it, PoTokenBindingType.DATA_SYNC_ID) }
        }

        return visitorData?.takeIf { it.isNotBlank() }
            ?.let { PoTokenBinding(it, PoTokenBindingType.VISITOR_DATA) }
    }
}

internal object PoTokenPairMinter {
    fun mint(
        videoId: String,
        streamingDataContentBinding: String,
        generate: (String) -> String
    ): Pair<String, String> {
        // BgUtils requires the streaming token to be minted before any player token.
        val streamingToken = generate(streamingDataContentBinding)
        val playerToken = if (streamingDataContentBinding == videoId) {
            streamingToken
        } else {
            generate(videoId)
        }

        return Pair(playerToken, streamingToken)
    }
}
