package com.liskovsoft.youtubeapi.app.playerdata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerUrlCacheTest {
    @Test
    fun storesExtractorsByPlayerUrl() {
        val cache = PlayerUrlCache<String>(2)

        cache.put("web", "web extractor")
        cache.put("tv", "tv extractor")

        assertEquals("web extractor", cache["web"])
        assertEquals("tv extractor", cache["tv"])
    }

    @Test
    fun evictsLeastRecentlyUsedPlayerUrl() {
        val cache = PlayerUrlCache<String>(2)

        cache.put("web", "web extractor")
        cache.put("tv", "tv extractor")
        cache["web"]
        cache.put("embedded", "embedded extractor")

        assertEquals("web extractor", cache["web"])
        assertNull(cache["tv"])
        assertEquals("embedded extractor", cache["embedded"])
    }
}
