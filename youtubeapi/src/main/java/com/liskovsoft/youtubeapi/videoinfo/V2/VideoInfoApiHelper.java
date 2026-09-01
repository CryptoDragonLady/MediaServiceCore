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
        return getVideoInfoQuery(client, videoId, clickTrackingParams, poTokens,
                clientPlaybackNonce, signatureTimestamp,
                poTokens != null ? poTokens.visitorData : null);
    }

    static String getVideoInfoQuery(AppClient client, String videoId, String clickTrackingParams,
                                    PoTokenResult poTokens, String clientPlaybackNonce,
                                    Integer signatureTimestamp, String visitorData) {
        return createCheckedQuery(
                client,
                videoId,
                clickTrackingParams,
                client == AppClient.GEO,
                poTokens,
                clientPlaybackNonce,
                signatureTimestamp,
                visitorData
        );
    }

    static String getVisitorDataQuery(AppClient client) {
        return new QueryBuilder(client).build();
    }

    static String resolveVisitorData(PoTokenResult poTokens, String fallbackVisitorData) {
        return poTokens != null ? poTokens.visitorData : fallbackVisitorData;
    }

    static Integer resolveSignatureTimestamp(AppClient client, String signatureTimestamp) {
        if (signatureTimestamp == null || signatureTimestamp.trim().isEmpty()) {
            return null;
        }

        String normalized = client.isTVClient() && signatureTimestamp.length() == 5 ?
                signatureTimestamp + "001" : signatureTimestamp;
        try {
            int result = Integer.parseInt(normalized);
            return result >= 0 ? result : null;
        } catch (NumberFormatException error) {
            return null;
        }
    }

    /** NOTE: enableGeoFix uses protobuf to bypass geo blocking. */
    private static String createCheckedQuery(AppClient client, String videoId, String clickTrackingParams,
                                             boolean enableGeoFix, PoTokenResult poTokens,
                                             String clientPlaybackNonce, Integer signatureTimestamp,
                                             String visitorData) {
        return new QueryBuilder(client)
                .setVideoId(videoId)
                .setClickTrackingParams(clickTrackingParams)
                .setPoToken(client.isPlayerPoTokenRequired() && poTokens != null ? poTokens.playerRequestPoToken : null)
                .setVisitorData(visitorData)
                .setClientPlaybackNonce(clientPlaybackNonce)
                .setSignatureTimestamp(signatureTimestamp)
                .enableGeoFix(enableGeoFix) // may broke other functionality
                .build();
    }
}
