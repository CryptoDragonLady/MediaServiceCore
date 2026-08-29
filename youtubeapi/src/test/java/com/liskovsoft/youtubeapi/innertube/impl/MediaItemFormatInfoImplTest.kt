package com.liskovsoft.youtubeapi.innertube.impl

import com.google.gson.Gson
import com.liskovsoft.mediaserviceinterfaces.data.MediaFormat
import com.liskovsoft.sharedutils.TestHelpers
import com.liskovsoft.youtubeapi.innertube.models.PlayerResult
import com.liskovsoft.youtubeapi.common.helpers.AppClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaItemFormatInfoImplTest {
    @Test
    fun sabrResponsePreservesFormatMetadataAndPrefersSabr() {
        val formatInfo = MediaItemFormatInfoImpl(
            parsePlayerResponse("player/web/2025.11.10_player_sabr_only.json")
        )
        val audioFormat = formatInfo.getAdaptiveFormats()
            ?.firstOrNull { it.getAudioTrackId() != null }

        assertTrue(formatInfo.containsSabrFormats())
        assertFalse(formatInfo.containsDashFormats())
        assertNotNull(audioFormat)
        assertNotNull(audioFormat?.getXtags())
        assertNotNull(audioFormat?.getAudioTrackId())
    }

    @Test
    fun responseWithoutSabrEndpointRetainsDashFallback() {
        val response = parsePlayerResponse("player/web/2025.11.10_player_regular.json")
        val dashFormat = response.streamingData?.adaptiveFormats
            ?.first { it.mimeType?.startsWith("video/") == true }
            ?.copy(
                url = "https://example.com/videoplayback",
                cipher = null,
                signatureCipher = null
            )
        val withoutSabrEndpoint = response.copy(
            streamingData = response.streamingData?.copy(
                adaptiveFormats = listOfNotNull(dashFormat),
                serverAbrStreamingUrl = null
            )
        )
        val formatInfo = MediaItemFormatInfoImpl(withoutSabrEndpoint)

        assertFalse(formatInfo.containsSabrFormats())
        assertTrue(formatInfo.containsDashFormats())
        assertEquals(MediaFormat.FORMAT_TYPE_DASH, formatInfo.getAdaptiveFormats()?.first()?.getFormatType())
    }

    @Test
    fun responseWithoutUstreamerConfigRetainsDashFallback() {
        val response = parsePlayerResponse("player/web/2025.11.10_player_regular.json")
        val dashFormat = response.streamingData?.adaptiveFormats
            ?.first { it.mimeType?.startsWith("video/") == true }
            ?.copy(
                url = "https://example.com/videoplayback",
                cipher = null,
                signatureCipher = null
            )
        val withoutUstreamerConfig = response.copy(
            streamingData = response.streamingData?.copy(
                adaptiveFormats = listOfNotNull(dashFormat),
                serverAbrStreamingUrl = "https://example.com/sabr"
            ),
            playerConfig = response.playerConfig?.copy(
                mediaCommonConfig = response.playerConfig.mediaCommonConfig?.copy(
                    mediaUstreamerRequestConfig = null
                )
            )
        )
        val formatInfo = MediaItemFormatInfoImpl(withoutUstreamerConfig)

        assertFalse(formatInfo.containsSabrFormats())
        assertTrue(formatInfo.containsDashFormats())
        assertEquals(MediaFormat.FORMAT_TYPE_DASH, formatInfo.getAdaptiveFormats()?.first()?.getFormatType())
    }

    @Test
    fun preservesRequestClientAndUsesContextSpecificTokens() {
        val response = parsePlayerResponse("player/web/2025.11.10_player_regular.json")
        val withManifests = response.copy(
            streamingData = response.streamingData?.copy(
                hlsManifestUrl = "https://example.com/manifest.m3u8?foo=bar",
                dashManifestUrl = "https://example.com/manifest.mpd?foo=bar"
            )
        )
        val formatInfo = MediaItemFormatInfoImpl(
            withManifests,
            AppClient.MWEB,
            playerRequestPoToken = "player-token",
            streamingDataPoToken = "streaming-token"
        )

        assertEquals(AppClient.MWEB, formatInfo.clientInfo)
        assertEquals("streaming-token", formatInfo.poToken)
        assertTrue(formatInfo.hlsManifestUrl?.contains("pot=streaming-token") == true)
        assertTrue(formatInfo.dashManifestUrl?.contains("pot=streaming-token") == true)
        assertTrue(formatInfo.subtitles?.firstOrNull()?.baseUrl?.contains("pot=player-token") == true)
    }

    private fun parsePlayerResponse(path: String) =
        Gson().fromJson(TestHelpers.readResource(path), PlayerResult::class.java)
}
