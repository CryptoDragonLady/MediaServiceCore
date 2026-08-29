package com.liskovsoft.youtubeapi.innertube

import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo
import com.liskovsoft.youtubeapi.innertube.core.HTTPClient
import com.liskovsoft.youtubeapi.innertube.core.RequestInit
import com.liskovsoft.youtubeapi.innertube.core.RequestInitBody
import com.liskovsoft.youtubeapi.innertube.core.Session
import com.liskovsoft.youtubeapi.innertube.impl.MediaItemFormatInfoImpl
import com.liskovsoft.youtubeapi.innertube.initialresponse.InitialResponseService
import com.liskovsoft.youtubeapi.app.PoTokenGate
import com.liskovsoft.youtubeapi.common.helpers.AppClient

internal object InnertubeService {
    @JvmStatic
    fun createFormatInfo(videoId: String): MediaItemFormatInfo? = createFormatInfoV1(videoId)

    /**
     * LuanRT variation
     */
    internal fun createFormatInfoV1(videoId: String): MediaItemFormatInfo? {
        val client = AppClient.WEB
        val poTokens = PoTokenGate.getTokenResult(client, videoId)
        val session = Session.create() ?: return null
        poTokens?.visitorData?.let { session.context.client.visitorData = it }
        val httpClient = HTTPClient(session)
        val playerResult = httpClient.fetch(
            "/player",
            RequestInit(
                body = RequestInitBody(
                    videoId,
                    session = session,
                    poToken = poTokens?.playerRequestPoToken
                )
            )
        )
            ?: return null

        val formatInfo = MediaItemFormatInfoImpl(
            playerResult,
            client,
            poTokens?.playerRequestPoToken,
            poTokens?.streamingDataPoToken
        )
        session.player.decipher(formatInfo)

        if (formatInfo.isUnplayable) {
            Session.invalidate()
        }

        return formatInfo
    }

    /**
     * yt-dlp variation
     */
    internal fun createFormatInfoV2(videoId: String): MediaItemFormatInfo? {
        val client = AppClient.WEB
        val poTokens = PoTokenGate.getTokenResult(client, videoId)
        val session = Session.create() ?: return null

        val playerResult = InitialResponseService.getPlayerResult(videoId) ?: return null

        val formatInfo = MediaItemFormatInfoImpl(
            playerResult,
            client,
            poTokens?.playerRequestPoToken,
            poTokens?.streamingDataPoToken
        )
        session.player.decipher(formatInfo)

        return formatInfo
    }
}
