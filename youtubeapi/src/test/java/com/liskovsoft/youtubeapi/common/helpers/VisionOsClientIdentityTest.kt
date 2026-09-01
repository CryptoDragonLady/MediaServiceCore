package com.liskovsoft.youtubeapi.common.helpers

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VisionOsClientIdentityTest {
    @Test
    fun nativeIdentityMatchesCurrentVisionOsPlayerClient() {
        assertEquals("VISIONOS", AppClient.VISIONOS.clientName)
        assertEquals("1.04", AppClient.VISIONOS.clientVersion)
        assertEquals("visionOS", AppClient.VISIONOS.osName)
        assertEquals("26.6.0.23O770", AppClient.VISIONOS.osVersion)
        assertEquals(
            "com.google.visionos.youtube/1.04(RealityDevice17,1; U; CPU visionOS 26_6_0 like Mac OS X; US)",
            AppClient.VISIONOS.userAgent
        )
    }

    @Test
    fun playerBodyAndHeaderUseOneVisionOsIdentity() {
        val json = JsonParser().parse(
            QueryBuilder(AppClient.VISIONOS)
                .setLanguage("en")
                .setCountry("US")
                .setUtcOffsetMinutes(0)
                .setVisitorData("visitor")
                .setVideoId("video-id")
                .setClientPlaybackNonce("request-cpn")
                .setSignatureTimestamp(12345)
                .build()
        ).asJsonObject
        val client = json.getAsJsonObject("context").getAsJsonObject("client")

        assertEquals(AppClient.VISIONOS.userAgent, client.get("userAgent").asString)
        assertEquals("Apple", client.get("deviceMake").asString)
        assertEquals("RealityDevice17,1", client.get("deviceModel").asString)
        assertEquals("visionOS", client.get("osName").asString)
        assertEquals("26.6.0.23O770", client.get("osVersion").asString)
        assertEquals("MOBILE", client.get("platform").asString)
        assertFalse(json.has("serviceIntegrityDimensions"))
    }
}
