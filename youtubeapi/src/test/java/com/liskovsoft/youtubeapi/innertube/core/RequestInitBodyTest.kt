package com.liskovsoft.youtubeapi.innertube.core

import com.google.gson.JsonParser
import com.liskovsoft.youtubeapi.innertube.utils.toJsonString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RequestInitBodyTest {
    @Test
    fun serializesPlayerPoTokenInServiceIntegrityDimensions() {
        val body = RequestInitBody(
            videoId = "video-id",
            signatureTimestamp = "12345",
            poToken = "player-token"
        )
        val json = JsonParser().parse(toJsonString(body)).asJsonObject

        assertEquals(
            "player-token",
            json.getAsJsonObject("serviceIntegrityDimensions").get("poToken").asString
        )
    }

    @Test
    fun omitsServiceIntegrityDimensionsWithoutToken() {
        val body = RequestInitBody(
            videoId = "video-id",
            signatureTimestamp = "12345"
        )
        val json = JsonParser().parse(toJsonString(body)).asJsonObject

        assertFalse(json.has("serviceIntegrityDimensions"))
    }
}
