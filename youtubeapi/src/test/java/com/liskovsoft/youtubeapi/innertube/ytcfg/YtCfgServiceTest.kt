package com.liskovsoft.youtubeapi.innertube.ytcfg

import com.google.gson.JsonParser
import com.liskovsoft.youtubeapi.common.helpers.AppClient
import org.junit.Assert.assertEquals
import org.junit.Test

class YtCfgServiceTest {
    private val ytCfg = JsonParser().parse(
        """
        {
          "WEB_PLAYER_CONTEXT_CONFIGS": {
            "WEB_PLAYER_CONTEXT_CONFIG_ID_KEVLAR_WATCH": {
              "jsUrl": "/s/player/web/player_ias.vflset/en_US/base.js",
              "serializedExperimentFlags": "other_flag=true&html5_generate_content_po_token=true"
            },
            "WEB_PLAYER_CONTEXT_CONFIG_ID_EMBEDDED_PLAYER": {
              "jsUrl": "/s/player/embed/player_embed.vflset/en_US/base.js"
            }
          }
        }
        """.trimIndent()
    ).asJsonObject

    @Test
    fun selectsWatchPlayerForWebClient() {
        assertEquals(
            "https://www.youtube.com/s/player/web/player_ias.vflset/en_US/base.js",
            YtCfgService.extractPlayerUrl(ytCfg, AppClient.WEB)
        )
    }

    @Test
    fun selectsEmbeddedPlayerForEmbeddedClient() {
        assertEquals(
            "https://www.youtube.com/s/player/embed/player_embed.vflset/en_US/base.js",
            YtCfgService.extractPlayerUrl(ytCfg, AppClient.WEB_EMBED)
        )
    }

    @Test
    fun detectsVideoBoundGvsTokenExperiment() {
        assertEquals(true, YtCfgService.extractGvsPoTokenBinding(ytCfg, AppClient.WEB))
        assertEquals(false, YtCfgService.extractGvsPoTokenBinding(ytCfg, AppClient.WEB_EMBED))
    }
}
