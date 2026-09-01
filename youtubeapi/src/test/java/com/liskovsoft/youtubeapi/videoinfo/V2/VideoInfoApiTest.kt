package com.liskovsoft.youtubeapi.videoinfo.V2

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

class VideoInfoApiTest {
    @Test
    fun playerRequestsDeclareYoutubeOrigin() {
        val headers = requireNotNull(VideoInfoApi::class.java
            .getMethod(
                "getVideoInfo",
                String::class.java,
                String::class.java,
                String::class.java,
                String::class.java,
                String::class.java
            )
            .getAnnotation(Headers::class.java))
            .value

        assertTrue(headers.contains("Origin: https://www.youtube.com"))
    }

    @Test
    fun visionOsPlayerRequestUsesNativeGoogleApisContract() {
        val method = VideoInfoApi::class.java.getMethod(
            "getVideoInfoVisionOs",
            String::class.java,
            String::class.java,
            String::class.java,
            String::class.java,
            String::class.java,
            String::class.java,
            String::class.java
        )
        val headers = requireNotNull(method.getAnnotation(Headers::class.java)).value
        val post = requireNotNull(method.getAnnotation(POST::class.java)).value
        val parameterAnnotations = method.parameterAnnotations.flatten()
            .filterIsInstance<Query>()
            .map { it.value }

        assertEquals("https://youtubei.googleapis.com/youtubei/v1/player", post)
        assertTrue(headers.contains("X-Goog-Api-Format-Version: 2"))
        assertFalse(headers.contains("Origin: https://www.youtube.com"))
        assertTrue(parameterAnnotations.containsAll(listOf("t", "id")))

        val visitorMethod = VideoInfoApi::class.java.getMethod(
            "getVisionOsVisitorData",
            String::class.java,
            String::class.java,
            String::class.java,
            String::class.java
        )
        assertEquals(
            "https://www.youtube.com/youtubei/v1/visitor_id",
            requireNotNull(visitorMethod.getAnnotation(POST::class.java)).value
        )
        assertTrue(requireNotNull(visitorMethod.getAnnotation(Headers::class.java)).value
            .contains("X-Goog-Api-Format-Version: 2"))
    }
}
