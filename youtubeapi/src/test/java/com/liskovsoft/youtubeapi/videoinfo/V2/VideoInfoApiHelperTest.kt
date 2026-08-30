package com.liskovsoft.youtubeapi.videoinfo.V2

import com.google.gson.JsonParser
import com.liskovsoft.youtubeapi.app.potokennp2.core.PoTokenResult
import com.liskovsoft.youtubeapi.common.helpers.AppClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VideoInfoApiHelperTest {
    @Test
    fun tokenVisitorDataTakesPrecedenceOverGlobalVisitorData() {
        val tokens = PoTokenResult(
            "video-id",
            "token-visitor",
            "player-token",
            "streaming-token",
            "token-visitor"
        )

        assertEquals(
            "token-visitor",
            VideoInfoApiHelper.resolveVisitorData(tokens, "global-visitor")
        )
    }

    @Test
    fun globalVisitorDataIsUsedWithoutTokens() {
        assertEquals(
            "global-visitor",
            VideoInfoApiHelper.resolveVisitorData(null, "global-visitor")
        )
    }

    @Test
    fun playerQueryUsesExactProvidedPlaybackNonce() {
        val tokens = PoTokenResult(
            "video-id",
            "visitor-data",
            "player-token",
            "streaming-token",
            "visitor-data"
        )

        val query = VideoInfoApiHelper.getVideoInfoQuery(
            AppClient.VISIONOS,
            "video-id",
            null,
            tokens,
            "request-nonce-01",
            12345
        )
        val json = JsonParser().parse(query).asJsonObject

        assertEquals("request-nonce-01", json.get("cpn").asString)
    }

    @Test
    fun tvStreamingProofDoesNotBecomeAPlayerRequestProof() {
        val tokens = PoTokenResult(
            "video-id",
            "token-visitor",
            "player-token",
            "streaming-token",
            "video-id"
        )

        val query = VideoInfoApiHelper.getVideoInfoQuery(
            AppClient.TV_DOWNGRADED,
            "video-id",
            null,
            tokens,
            "request-nonce-01",
            12345
        )
        val json = JsonParser().parse(query).asJsonObject

        assertFalse(json.has("serviceIntegrityDimensions"))
        assertEquals(
            "token-visitor",
            json.getAsJsonObject("context").getAsJsonObject("client").get("visitorData").asString
        )
    }
}
