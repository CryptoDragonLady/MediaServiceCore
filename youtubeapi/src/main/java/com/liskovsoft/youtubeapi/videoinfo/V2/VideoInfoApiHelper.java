package com.liskovsoft.youtubeapi.videoinfo.V2;

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
                PoTokenGate.getTokenResult(client, videoId)
        );
    }

    static String getVideoInfoQuery(AppClient client, String videoId, String clickTrackingParams,
                                    PoTokenResult poTokens) {
        return createCheckedQuery(client, videoId, clickTrackingParams, client == AppClient.GEO, poTokens);
    }

    static String resolveVisitorData(PoTokenResult poTokens, String fallbackVisitorData) {
        return poTokens != null ? poTokens.visitorData : fallbackVisitorData;
    }

    /**
     * NOTE: enableGeoFix - Should use protobuf to bypass geo blocking.
     */
    private static String createCheckedQuery(AppClient client, String videoId, String clickTrackingParams,
                                             boolean enableGeoFix, PoTokenResult poTokens) {
        return new QueryBuilder(client)
                .setVideoId(videoId)
                .setClickTrackingParams(clickTrackingParams)
                .setPoToken(poTokens != null ? poTokens.playerRequestPoToken : null)
                .setVisitorData(poTokens != null ? poTokens.visitorData : null)
                .enableGeoFix(enableGeoFix) // may broke other functionality
                .build();
    }
}
