package com.liskovsoft.youtubeapi.app

import com.liskovsoft.youtubeapi.common.helpers.AppClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PoTokenFallbackChainTest {
    @Test
    fun usesFirstSuccessfulProvider() {
        val calls = mutableListOf<String>()
        val chain = PoTokenFallbackChain(
            listOf(
                { calls += "first"; null },
                { binding -> calls += "second:$binding"; "token" },
                { calls += "third"; "unused" }
            )
        )

        assertEquals("token", chain.getToken("video-id"))
        assertEquals(listOf("first", "second:video-id"), calls)
    }

    @Test
    fun providerFailureFallsThroughWithoutLeakingResult() {
        val chain = PoTokenFallbackChain(
            listOf(
                { throw IllegalStateException("unavailable") },
                { "" },
                { null }
            )
        )

        assertNull(chain.getToken("video-id"))
    }

    @Test
    fun cacheKeyIncludesActualStreamingBinding() {
        val visitorBound = PoTokenCacheKey(AppClient.TV_DOWNGRADED, "video-id", "visitor-data")
        val accountBound = PoTokenCacheKey(AppClient.TV_DOWNGRADED, "video-id", "data-sync-id")

        assertNotEquals(visitorBound, accountBound)
    }
}
