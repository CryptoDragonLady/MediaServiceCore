package com.liskovsoft.youtubeapi.app.potokennp2.visitor

import com.liskovsoft.youtubeapi.app.potokennp2.visitor.data.VisitorResult
import com.liskovsoft.googlecommon.common.converters.gson.WithGson
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

@WithGson
internal interface VisitorApi {
    @Headers(
        "Content-Type: application/json",
        "Accept-Language: en-US, en;q=0.9",
        "Cookie: SOCS=CAE=",
        "Host: www.youtube.com",
        "Origin: https://www.youtube.com",
        "Referer: https://www.youtube.com"
        )
    @POST("https://www.youtube.com/youtubei/v1/visitor_id")
    fun getVisitorId(
        @Body query: String = VisitorApiHelper.getVisitorQuery(),
        @Header("User-Agent") userAgent: String = VisitorApiHelper.getUserAgent(),
        @Header("X-Youtube-Client-Name") clientName: String? = VisitorApiHelper.getClientName(),
        @Header("X-Youtube-Client-Version") clientVersion: String = VisitorApiHelper.getClientVersion()
    ): Call<VisitorResult?>
}
