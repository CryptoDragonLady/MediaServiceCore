package com.liskovsoft.youtubeapi.videoinfo.V2;

import com.liskovsoft.googlecommon.common.converters.jsonpath.WithJsonPath;
import com.liskovsoft.youtubeapi.videoinfo.models.VideoInfo;
import com.liskovsoft.youtubeapi.videoinfo.models.VideoInfoHls;
import com.liskovsoft.youtubeapi.videoinfo.models.VideoInfoReel;
import com.liskovsoft.youtubeapi.videoinfo.models.VisitorDataResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

@WithJsonPath
public interface VideoInfoApi {
    @Headers({"Content-Type: application/json", "Origin: https://www.youtube.com"})
    @POST("https://www.youtube.com/youtubei/v1/player")
    Call<VideoInfo> getVideoInfo(@Body String videoQuery, @Header("X-Goog-Visitor-Id") String visitorId, @Header("User-Agent") String userAgent,
                                 @Header("X-Youtube-Client-Name") String clientName, @Header("X-Youtube-Client-Version") String clientVersion);

    @Headers({"Content-Type: application/json", "X-Goog-Api-Format-Version: 2"})
    @POST("https://youtubei.googleapis.com/youtubei/v1/player")
    Call<VideoInfo> getVideoInfoVisionOs(@Body String videoQuery, @Header("X-Goog-Visitor-Id") String visitorId,
                                         @Header("User-Agent") String userAgent,
                                         @Header("X-Youtube-Client-Name") String clientName,
                                         @Header("X-Youtube-Client-Version") String clientVersion,
                                         @Query("t") String requestNonce, @Query("id") String videoId);

    @Headers({"Content-Type: application/json", "X-Goog-Api-Format-Version: 2"})
    @POST("https://www.youtube.com/youtubei/v1/visitor_id")
    Call<VisitorDataResponse> getVisionOsVisitorData(@Body String visitorQuery,
                                                     @Header("User-Agent") String userAgent,
                                                     @Header("X-Youtube-Client-Name") String clientName,
                                                     @Header("X-Youtube-Client-Version") String clientVersion);

    @Headers("Content-Type: application/json")
    @POST("https://youtubei.googleapis.com/youtubei/v1/reel/reel_item_watch")
    Call<VideoInfoReel> getVideoInfoReel(@Body String videoQuery, @Header("X-Goog-Visitor-Id") String visitorId, @Header("User-Agent") String userAgent,
                                 @Header("X-Youtube-Client-Name") String clientName, @Header("X-Youtube-Client-Version") String clientVersion);

    @Headers({"Content-Type: application/json", "Origin: https://www.youtube.com"})
    @POST("https://www.youtube.com/youtubei/v1/player")
    Call<VideoInfoHls> getVideoInfoHls(@Body String videoQuery, @Header("X-Goog-Visitor-Id") String visitorId, @Header("User-Agent") String userAgent,
                                       @Header("X-Youtube-Client-Name") String clientName, @Header("X-Youtube-Client-Version") String clientVersion);
}
