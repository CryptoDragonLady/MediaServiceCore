package com.liskovsoft.youtubeapi.videoinfo.V2;

import com.liskovsoft.youtubeapi.app.AppService;
import com.liskovsoft.youtubeapi.app.PoTokenGate;
import com.liskovsoft.youtubeapi.app.potokennp2.core.PoTokenResult;
import com.liskovsoft.youtubeapi.common.helpers.AppClient;
import com.liskovsoft.youtubeapi.common.helpers.QueryBuilder;

public class VideoInfoApiHelper {
    public static String getVideoInfoQuery(AppClient client, String videoId, String clickTrackingParams) {
        return getVideoInfoQuery(
                client,
                videoId,
                clickTrackingParams,
                PoTokenGate.getTokenResult(client, videoId),
                AppService.instance().getClientPlaybackNonce()
        );
    }

    static String getVideoInfoQuery(AppClient client, String videoId, String clickTrackingParams,
                                    PoTokenResult poTokens) {
        return getVideoInfoQuery(
                client,
                videoId,
                clickTrackingParams,
                poTokens,
                AppService.instance().getClientPlaybackNonce()
        );
    }

    static String getVideoInfoQuery(AppClient client, String videoId, String clickTrackingParams,
                                    PoTokenResult poTokens, String clientPlaybackNonce) {
        return getVideoInfoQuery(
                client,
                videoId,
                clickTrackingParams,
                poTokens,
                clientPlaybackNonce,
                null
        );
    }

    static String getVideoInfoQuery(AppClient client, String videoId, String clickTrackingParams,
                                    PoTokenResult poTokens, String clientPlaybackNonce,
                                    Integer signatureTimestamp) {
        return createCheckedQuery(
                client,
                videoId,
                clickTrackingParams,
                client == AppClient.GEO,
                poTokens,
                clientPlaybackNonce,
                signatureTimestamp
        );
    }

    static String resolveVisitorData(PoTokenResult poTokens, String fallbackVisitorData) {
        return poTokens != null ? poTokens.visitorData : fallbackVisitorData;
    }

    /**
     * NOTE: enableGeoFix - Should use protobuf to bypass geo blocking.
     */
    private static String createCheckedQuery(AppClient client, String videoId, String clickTrackingParams,
                                             boolean enableGeoFix, PoTokenResult poTokens,
                                             String clientPlaybackNonce, Integer signatureTimestamp) {
        return new QueryBuilder(client)
                .setVideoId(videoId)
                .setClickTrackingParams(clickTrackingParams)
                .setPoToken(client.isPlayerPoTokenRequired() && poTokens != null ? poTokens.playerRequestPoToken : null)
                .setVisitorData(poTokens != null ? poTokens.visitorData : null)
                .setClientPlaybackNonce(clientPlaybackNonce)
                .setSignatureTimestamp(signatureTimestamp)
                .enableGeoFix(enableGeoFix) // may broke other functionality
                .build();
    }
}
