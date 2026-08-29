package com.liskovsoft.youtubeapi.videoinfo.V2

import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.Headers

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
}
