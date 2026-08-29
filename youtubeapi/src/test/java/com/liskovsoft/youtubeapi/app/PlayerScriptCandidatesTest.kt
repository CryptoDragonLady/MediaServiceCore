package com.liskovsoft.youtubeapi.app

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerScriptCandidatesTest {
    @Test
    fun explicitPlayerUrlDoesNotUseAnotherClientsFallback() {
        val webPlayerUrl = "https://www.youtube.com/s/player/web/player_ias.vflset/en_US/base.js"
        val tvPlayerUrl = "https://www.youtube.com/s/player/tv/tv-player-es6.vflset/tv-player-es6.js"

        assertEquals(
            listOf(webPlayerUrl),
            PlayerScriptCandidates.resolve(webPlayerUrl, listOf(tvPlayerUrl))
        )
    }

    @Test
    fun missingPlayerUrlUsesDistinctFallbacksInOrder() {
        val first = "https://www.youtube.com/s/player/first/base.js"
        val second = "https://www.youtube.com/s/player/second/base.js"

        assertEquals(
            listOf(first, second),
            PlayerScriptCandidates.resolve(null, listOf(first, null, first, second))
        )
    }
}
