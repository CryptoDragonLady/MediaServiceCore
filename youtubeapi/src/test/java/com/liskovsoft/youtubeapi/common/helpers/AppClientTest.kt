package com.liskovsoft.youtubeapi.common.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppClientTest {
    @Test
    fun webEmbedUsesEmbeddedPlayerScreen() {
        assertEquals("EMBED", AppClient.WEB_EMBED.clientScreen)
    }

    @Test
    fun invalidSignatureTimestampIsOmitted() {
        assertNull(QueryBuilder.normalizeSignatureTimestamp(null, false))
        assertNull(QueryBuilder.normalizeSignatureTimestamp("invalid", false))
        assertNull(QueryBuilder.normalizeSignatureTimestamp("-1", false))
    }

    @Test
    fun tvTimestampUsesTvSuffix() {
        assertEquals(20522001, QueryBuilder.normalizeSignatureTimestamp("20522", true))
        assertEquals(20522, QueryBuilder.normalizeSignatureTimestamp("20522", false))
    }
}
