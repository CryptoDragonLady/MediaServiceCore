package com.liskovsoft.youtubeapi.app.potokennp2.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PoTokenBindingSelectorTest {
    @Test
    fun playerAndSubtitleTokensUseVideoId() {
        val expected = PoTokenBinding("video-id", PoTokenBindingType.VIDEO_ID)

        assertEquals(expected, select(PoTokenContext.PLAYER))
        assertEquals(expected, select(PoTokenContext.SUBS))
    }

    @Test
    fun loggedOutGvsTokenUsesVisitorDataByDefault() {
        assertEquals(
            PoTokenBinding("visitor-data", PoTokenBindingType.VISITOR_DATA),
            select(PoTokenContext.GVS)
        )
    }

    @Test
    fun experimentMakesGvsTokenVideoBound() {
        assertEquals(
            PoTokenBinding("video-id", PoTokenBindingType.VIDEO_ID),
            select(PoTokenContext.GVS, gvsBindToVideoId = true)
        )
    }

    @Test
    fun authenticatedGvsTokenUsesDataSyncId() {
        assertEquals(
            PoTokenBinding("data-sync-id", PoTokenBindingType.DATA_SYNC_ID),
            select(PoTokenContext.GVS, authenticated = true)
        )
    }

    @Test
    fun missingRequiredBindingDoesNotMintWrongToken() {
        assertNull(
            PoTokenBindingSelector.select(
                context = PoTokenContext.GVS,
                videoId = "video-id",
                visitorData = null
            )
        )
        assertNull(
            PoTokenBindingSelector.select(
                context = PoTokenContext.GVS,
                videoId = "video-id",
                visitorData = "visitor-data",
                authenticated = true
            )
        )
    }

    @Test
    fun streamingTokenIsMintedBeforePlayerToken() {
        val calls = mutableListOf<String>()

        val result = PoTokenPairMinter.mint("video-id", "visitor-data") {
            calls += it
            "token-for-$it"
        }

        assertEquals(listOf("visitor-data", "video-id"), calls)
        assertEquals("token-for-video-id", result.first)
        assertEquals("token-for-visitor-data", result.second)
    }

    @Test
    fun sharedBindingIsMintedOnlyOnce() {
        val calls = mutableListOf<String>()

        val result = PoTokenPairMinter.mint("video-id", "video-id") {
            calls += it
            "shared-token"
        }

        assertEquals(listOf("video-id"), calls)
        assertEquals(Pair("shared-token", "shared-token"), result)
    }

    private fun select(
        context: PoTokenContext,
        authenticated: Boolean = false,
        gvsBindToVideoId: Boolean = false
    ) = PoTokenBindingSelector.select(
        context = context,
        videoId = "video-id",
        visitorData = "visitor-data",
        dataSyncId = "data-sync-id",
        authenticated = authenticated,
        gvsBindToVideoId = gvsBindToVideoId
    )
}
