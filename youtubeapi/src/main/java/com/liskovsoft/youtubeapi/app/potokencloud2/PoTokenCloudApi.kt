package com.liskovsoft.youtubeapi.app.potokencloud2

import com.liskovsoft.googlecommon.common.converters.gson.WithGson
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

@WithGson
internal interface PoTokenCloudApi {
    @GET
    fun getPoToken(@Url url: String, @Query("content_binding") contentBinding: String): Call<PoTokenResponse?>

    @Headers("Content-Type: application/json")
    @POST
    fun getPoTokenV1(@Url url: String, @Body request: PoTokenRequest): Call<PoTokenResponse?>

    // /health-check
    @GET
    fun healthCheck(@Url url: String): Call<Void>
}

internal data class PoTokenRequest(val content_binding: String)
