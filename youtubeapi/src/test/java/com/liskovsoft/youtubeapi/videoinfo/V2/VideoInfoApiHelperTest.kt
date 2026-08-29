package com.liskovsoft.youtubeapi.videoinfo.V2

import com.liskovsoft.youtubeapi.app.potokennp2.core.PoTokenResult
import org.junit.Assert.assertEquals
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
}
