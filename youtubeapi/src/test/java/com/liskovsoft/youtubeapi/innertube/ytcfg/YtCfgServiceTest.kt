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

    @Test
    fun detectsTvLivingRoomGvsTokenExperiment() {
        val tvYtCfg = JsonParser().parse(
            """
            {
              "WEB_PLAYER_CONTEXT_CONFIGS": {
                "WEB_PLAYER_CONTEXT_CONFIG_ID_LIVING_ROOM_WATCH": {
                  "jsUrl": "/s/player/tv/tv-player-ias.js",
                  "serializedExperimentFlags": "html5_generate_content_po_token=true"
                }
              }
            }
            """.trimIndent()
        ).asJsonObject

        assertEquals(true, YtCfgService.extractGvsPoTokenBinding(tvYtCfg, AppClient.TV_DOWNGRADED))
    }

    @Test
    fun clientsWithoutAPlayerPageUseWebWatchContextForChallenges() {
        assertEquals(AppClient.WEB, YtCfgService.selectPlayerConfigClient(AppClient.VISIONOS, "video-id"))
        assertEquals(AppClient.WEB, YtCfgService.selectPlayerConfigClient(AppClient.IOS, "video-id"))
    }

    @Test
    fun clientsWithAPlayerPageKeepTheirOwnContext() {
        assertEquals(AppClient.WEB_EMBED, YtCfgService.selectPlayerConfigClient(AppClient.WEB_EMBED, "video-id"))
        assertEquals(AppClient.WEB, YtCfgService.selectPlayerConfigClient(AppClient.WEB, "video-id"))
    }

    @Test
    fun authenticatedTvClientsUseTheirTclPlayerForChallenges() {
        val genericTvPlayer =
            "https://www.youtube.com/s/player/e937390a/tv-player-es6.vflset/tv-player-es6.js"
        val tclTvPlayer =
            "https://www.youtube.com/s/player/e937390a/tv-player-es6-tcl.vflset/tv-player-es6-tcl.js"

        assertEquals(
            tclTvPlayer,
            YtCfgService.selectPlayerScriptIdentity(AppClient.TV, genericTvPlayer)
        )
        assertEquals(
            tclTvPlayer,
            YtCfgService.selectPlayerScriptIdentity(AppClient.TV_DOWNGRADED, genericTvPlayer)
        )
    }

    @Test
    fun browserAndNativeClientsKeepTheirDiscoveredPlayerForChallenges() {
        val webPlayer =
            "https://www.youtube.com/s/player/web/player_ias.vflset/en_US/base.js"

        assertEquals(
            webPlayer,
            YtCfgService.selectPlayerScriptIdentity(AppClient.WEB, webPlayer)
        )
        assertEquals(
            webPlayer,
            YtCfgService.selectPlayerScriptIdentity(AppClient.WEB_EMBED, webPlayer)
        )
        assertEquals(
            webPlayer,
            YtCfgService.selectPlayerScriptIdentity(AppClient.MWEB, webPlayer)
        )
        assertEquals(
            webPlayer,
            YtCfgService.selectPlayerScriptIdentity(AppClient.VISIONOS, webPlayer)
        )
    }
}
