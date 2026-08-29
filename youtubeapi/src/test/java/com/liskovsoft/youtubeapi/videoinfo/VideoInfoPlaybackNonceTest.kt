package com.liskovsoft.youtubeapi.videoinfo

import com.liskovsoft.youtubeapi.videoinfo.models.formats.AdaptiveVideoFormat
import com.liskovsoft.youtubeapi.videoinfo.models.VideoInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoInfoPlaybackNonceTest {
    @Test
    fun appendsPlayerRequestNonceToDirectMediaUrls() {
        val playbackNonce = "request-nonce-01"
        val format = AdaptiveVideoFormat().apply {
            url = "https://rr.example.googlevideo.com/videoplayback?itag=248&c=VISIONOS&cpn=stale"
        }

        VideoInfoServiceBase.applyPlaybackNonce(listOf(format.urlHolder), playbackNonce)

        assertEquals(playbackNonce, format.urlHolder.getParam("cpn"))
    }

    @Test
    fun selectedResponseNonceWinsOverLaterRequestNonce() {
        val videoInfo = VideoInfo().apply {
            clientPlaybackNonce = "selected-response"
        }
        val resolved = VideoInfoServiceBase.resolvePlaybackNonce(
            videoInfo,
            "later-metadata-request"
        )

        assertEquals("selected-response", resolved)
    }
}
