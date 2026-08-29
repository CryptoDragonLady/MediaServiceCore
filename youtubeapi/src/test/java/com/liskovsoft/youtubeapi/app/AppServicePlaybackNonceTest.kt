package com.liskovsoft.youtubeapi.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppServicePlaybackNonceTest {
    @Test
    fun generatesFallbackNonceWithoutPlayerExtractor() {
        val nonce = AppService.createClientPlaybackNonce(null)

        assertEquals(16, nonce.length)
        assertTrue(nonce.matches(Regex("[A-Za-z0-9_-]{16}")))
    }
}
