package com.liskovsoft.youtubeapi.app

import org.junit.Assert.assertEquals
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
}
