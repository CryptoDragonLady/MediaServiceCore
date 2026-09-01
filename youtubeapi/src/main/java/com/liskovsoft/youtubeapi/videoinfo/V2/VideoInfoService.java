package com.liskovsoft.youtubeapi.videoinfo.V2;

import androidx.annotation.Nullable;

import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.sharedutils.prefs.GlobalPreferences;
import com.liskovsoft.youtubeapi.app.AppService;
import com.liskovsoft.youtubeapi.app.PoTokenGate;
import com.liskovsoft.youtubeapi.app.potokennp2.core.PoTokenResult;
import com.liskovsoft.youtubeapi.common.helpers.AppClient;
import com.liskovsoft.googlecommon.common.helpers.RetrofitHelper;
import com.liskovsoft.googlecommon.common.helpers.YouTubeHelper;
import com.liskovsoft.mediaserviceinterfaces.data.PlaybackRequestContext;
import com.liskovsoft.mediaserviceinterfaces.data.PlaybackDebugMode;
import com.liskovsoft.youtubeapi.service.internal.MediaServiceData;
import com.liskovsoft.youtubeapi.service.YouTubeSignInService;
import com.liskovsoft.youtubeapi.innertube.initialresponse.InitialResponseService;
import com.liskovsoft.youtubeapi.innertube.ytcfg.YtCfgService;
import com.liskovsoft.youtubeapi.videoinfo.VideoInfoServiceBase;
import com.liskovsoft.youtubeapi.videoinfo.ManifestUrlTransformer;
import com.liskovsoft.youtubeapi.videoinfo.models.CaptionTrack;
import com.liskovsoft.youtubeapi.videoinfo.models.TranslationLanguage;
import com.liskovsoft.youtubeapi.videoinfo.models.VideoInfo;
import com.liskovsoft.youtubeapi.videoinfo.models.VideoInfoHls;
import com.liskovsoft.youtubeapi.videoinfo.models.VideoInfoReel;
import com.liskovsoft.youtubeapi.videoinfo.models.PlayerResponseAssessment;
import com.liskovsoft.youtubeapi.videoinfo.models.VisitorDataResponse;
import com.liskovsoft.youtubeapi.videoinfo.models.formats.VideoFormat;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import retrofit2.Call;

public class VideoInfoService extends VideoInfoServiceBase {
    private static final String TAG = VideoInfoService.class.getSimpleName();
    private static final AppClient IOS_CLIENT = AppClient.VISIONOS;
    private static final AppClient[] AUTHENTICATED_FALLBACK_CLIENTS = {
            AppClient.TV,
            AppClient.TV_DOWNGRADED
    };
    private static final AppClient WEB_CLIENT = AppClient.WEB_EMBED;
    private static final long AUTHORIZATION_INIT_WAIT_MS = 5_000;
    private static final AtomicLong NEXT_PLAYER_GENERATION = new AtomicLong();
    private static VideoInfoService sInstance;
    private final VideoInfoApi mVideoInfoApi;
    // TODO: tv clients are fully broken because of '-tcl' player
    private final static AppClient[] VIDEO_INFO_TYPE_LIST = {
            AppClient.WEB_EMBED, // Restricted (18+) videos
            AppClient.VISIONOS, // no url formats
            //AppClient.TV_DOWNGRADED, // probably unplayable (weird potoken format?)
            //AppClient.TV, // Supports auth. Fixes "please sign in" bug! (the best for Premium users)
            //AppClient.ANDROID_REEL, // doesn't require pot and cipher (hangs on all engines)
            AppClient.WEB, // Fix video clip blocked in current location
            AppClient.WEB_SAFARI,
            AppClient.IOS,
            AppClient.GEO, // Fix video clip blocked in current location
            AppClient.MWEB, // single audio language
            //AppClient.TV_LEGACY,
            //AppClient.TV_EMBED, // single audio language
            AppClient.ANDROID_VR, // doesn't require pot and cipher (often hangs?)
            //AppClient.TV_SIMPLY, // hangs?
            //AppClient.ANDROID_SDK_LESS, // doesn't require pot (hangs on Cronet!)
    };
    @Nullable
    private AppClient mActualInfoType = null;
    @Nullable
    private AppClient mNextInfoType = null;
    private boolean mUseAuth;
    private List<TranslationLanguage> mCachedTranslationLanguages;
    private boolean mIsUnplayable;

    private VideoInfoService() {
        mVideoInfoApi = RetrofitHelper.create(VideoInfoApi.class);
    }

    public static VideoInfoService instance() {
        if (sInstance == null) {
            sInstance = new VideoInfoService();
        }

        return sInstance;
    }

    public VideoInfo getVideoInfo(String videoId, String clickTrackingParams) {
        if (videoId == null) {
            return null;
        }

        //initInfoTypeIfNeeded();
        //reorderTypeListIfNeeded();

        AppService.instance().resetClientPlaybackNonce(); // unique value per each video info

        mUseAuth = true;

        VideoInfo result = firstPlayable(videoId, clickTrackingParams);

        if (result == null) {
            Log.e(TAG, "Can't get video info. videoId: %s", videoId);
            return null;
        }

        applyFixesIfNeeded(result, videoId, clickTrackingParams);

        transformFormats(result);

        persistRecentTypeIfNeeded(result);

        mIsUnplayable = !PlayerResponseAssessment.assess(result).isUsable();

        return result;
    }

    private void reorderTypeListIfNeeded() {
        if (getData().isFormatEnabled(MediaServiceData.FORMATS_EXTENDED_HLS)) {
            moveFirst(IOS_CLIENT);
        } else {
            moveFirst(WEB_CLIENT);
        }
    }

    private void moveFirst(AppClient client) {
        if (VIDEO_INFO_TYPE_LIST[0] != client) {
            Helpers.move(VIDEO_INFO_TYPE_LIST, Arrays.asList(VIDEO_INFO_TYPE_LIST).indexOf(client), 0);
        }
    }

    public VideoInfo getAuthVideoInfo(String videoId, String clickTrackingParams) {
        if (videoId == null) {
            return null;
        }

        mUseAuth = true;

        // Only the TV client supports auth features
        return getVideoInfo(AppClient.TV, videoId, clickTrackingParams);
    }

    private VideoInfo firstPlayable(String videoId, String clickTrackingParams) {
        if (PlaybackDebugMode.get() ==
                PlaybackDebugMode.Mode.FORCE_VISIONOS_HLS_REFERENCE) {
            VideoInfo forced = getVideoInfoWithRentFix(
                    AppClient.VISIONOS, videoId, clickTrackingParams);
            Log.d(TAG, "Forced reference candidate: client=VISIONOS, %s",
                    PlayerResponseAssessment.assess(forced));
            return forced;
        }
        final AppClient beginType = mNextInfoType != null ? mNextInfoType : VIDEO_INFO_TYPE_LIST[0];
        AppClient nextType = beginType;
        VideoInfo informativeFailure = null;
        VideoInfo bestLiveCandidate = null;
        int bestLivePriority = Integer.MIN_VALUE;
        boolean authenticatedFallbackAttempted = false;

        do {
            VideoInfo result = getVideoInfoWithRentFix(nextType, videoId, clickTrackingParams);

            if (result != null) {
                PlayerResponseAssessment assessment = PlayerResponseAssessment.assess(result);
                Log.d(TAG, "Player candidate: client=%s, %s", nextType.name(), assessment);
                if (!authenticatedFallbackAttempted && shouldTryAuthenticatedFallback(
                        assessment.getOutcome())) {
                    authenticatedFallbackAttempted = true;
                    VideoInfo authenticated = firstAuthenticatedPlayable(
                            videoId, clickTrackingParams);
                    PlayerResponseAssessment authenticatedAssessment =
                            PlayerResponseAssessment.assess(authenticated);
                    if (authenticatedAssessment.isUsable()) {
                        return authenticated;
                    }
                    if (preservesFailureState(authenticatedAssessment.getOutcome())) {
                        informativeFailure = authenticated;
                    }
                }
                if (assessment.isUsable()) {
                    // Preserve VOD latency. Active live playback is the only case where probing
                    // bounded clients for an HLS response is worth the additional requests.
                    if (!assessment.isLive()) {
                        return result;
                    }
                    int priority = assessment.getLiveTransportPriority();
                    if (priority > bestLivePriority) {
                        bestLiveCandidate = result;
                        bestLivePriority = priority;
                    }
                    if (assessment.getOutcome() == PlayerResponseAssessment.Outcome.USABLE_HLS_LIVE) {
                        return result;
                    }
                }
                if (informativeFailure == null && preservesFailureState(assessment.getOutcome())) {
                    informativeFailure = result;
                }
            }

            nextType = Helpers.getNextValue(VIDEO_INFO_TYPE_LIST, nextType);
        } while (nextType != beginType);

        PlayerResponseAssessment failureAssessment =
                PlayerResponseAssessment.assess(informativeFailure);
        if (bestLiveCandidate == null && !authenticatedFallbackAttempted &&
                shouldTryAuthenticatedFallback(failureAssessment.getOutcome())) {
            VideoInfo authenticated = firstAuthenticatedPlayable(videoId, clickTrackingParams);
            PlayerResponseAssessment authenticatedAssessment =
                    PlayerResponseAssessment.assess(authenticated);
            if (authenticatedAssessment.isUsable()) {
                return authenticated;
            }
            if (preservesFailureState(authenticatedAssessment.getOutcome())) {
                informativeFailure = authenticated;
            }
        }

        return bestLiveCandidate != null ? bestLiveCandidate : informativeFailure;
    }

    private VideoInfo firstAuthenticatedPlayable(String videoId, String clickTrackingParams) {
        VideoInfo informativeFailure = null;
        VideoInfo bestLiveCandidate = null;
        int bestLivePriority = Integer.MIN_VALUE;
        for (AppClient client : AUTHENTICATED_FALLBACK_CLIENTS) {
            VideoInfo result = getVideoInfoWithRentFix(client, videoId, clickTrackingParams);
            PlayerResponseAssessment assessment = PlayerResponseAssessment.assess(result);
            Log.d(TAG, "Authenticated player fallback: client=%s, %s",
                    client.name(), assessment);
            if (assessment.isUsable()) {
                if (!assessment.isLive()) {
                    return result;
                }
                int priority = assessment.getLiveTransportPriority();
                if (priority > bestLivePriority) {
                    bestLiveCandidate = result;
                    bestLivePriority = priority;
                }
                if (assessment.getOutcome() ==
                        PlayerResponseAssessment.Outcome.USABLE_HLS_LIVE) {
                    return result;
                }
            }
            if (informativeFailure == null && preservesFailureState(assessment.getOutcome())) {
                informativeFailure = result;
            }
        }
        return bestLiveCandidate != null ? bestLiveCandidate : informativeFailure;
    }

    static boolean shouldTryAuthenticatedFallback(
            PlayerResponseAssessment.Outcome outcome, boolean signedIn) {
        return signedIn && outcome == PlayerResponseAssessment.Outcome.LOGIN_REQUIRED;
    }

    private static boolean shouldTryAuthenticatedFallback(
            PlayerResponseAssessment.Outcome outcome) {
        if (outcome != PlayerResponseAssessment.Outcome.LOGIN_REQUIRED) {
            return false;
        }

        boolean playbackAuthorized = YouTubeSignInService.instance()
                .awaitPlaybackAuthorization(AUTHORIZATION_INIT_WAIT_MS);
        return shouldTryAuthenticatedFallback(outcome, playbackAuthorized);
    }

    static boolean shouldPreparePoTokens(AppClient client) {
        // Current TVHTML5 media URLs are authorized by their authenticated player response and
        // do not require WEB proof tokens. Injecting a separately minted GVS token changes the
        // signed media request and can make otherwise valid HLS, DASH, and SABR URLs return 403.
        return client != null && client.isWebPoTokenSupported() && !client.isTVClient();
    }

    private static boolean preservesFailureState(PlayerResponseAssessment.Outcome outcome) {
        return outcome == PlayerResponseAssessment.Outcome.UPCOMING ||
                outcome == PlayerResponseAssessment.Outcome.OFFLINE ||
                outcome == PlayerResponseAssessment.Outcome.RESTRICTED ||
                outcome == PlayerResponseAssessment.Outcome.LOGIN_REQUIRED ||
                outcome == PlayerResponseAssessment.Outcome.AGE_RESTRICTED;
    }

    //private void initInfoTypeIfNeeded() {
    //    if (mActualInfoType != null) {
    //        return;
    //    }
    //
    //    restoreVideoInfoType();
    //}

    public void switchNextFormat(boolean force) {
        if (force) {
            nextVideoInfoType();
            return;
        }

        //initInfoTypeIfNeeded();

        // Try to reset pot cache for the last video
        if (!mIsUnplayable && mActualInfoType != null && PoTokenGate.resetCache(mActualInfoType)) {
            return;
        }
        // The Premium is likely broken
        //if (getData().isFormatEnabled(MediaServiceData.FORMATS_EXTENDED_HLS)) {
        //    // Skip additional formats fetching that could produce an error
        //    getData().setFormatEnabled(MediaServiceData.FORMATS_EXTENDED_HLS, false);
        //    return;
        //}
        // And last, try to switch the client
        nextVideoInfoType();
        //persistVideoInfoType();
    }

    public void switchNextSubtitle() {
        CaptionTrack.sFormat = Helpers.getNextValue(CaptionTrack.CaptionFormat.values(), CaptionTrack.sFormat);
    }

    public void resetInfoType() {
        resetInfoTypeToDefault();
        PoTokenGate.resetCache();
    }

    private void nextVideoInfoType() {
        mNextInfoType = Helpers.getNextValue(VIDEO_INFO_TYPE_LIST, mActualInfoType);
    }

    private VideoInfo getVideoInfoWithRentFix(AppClient client, String videoId, String clickTrackingParams) {
        VideoInfo result = getVideoInfo(client, videoId, clickTrackingParams);

        if (result != null && result.isRent()) {
            Log.e(TAG, "Found rent content. Show trailer instead...");
            result = getVideoInfo(client, result.getTrailerVideoId(), clickTrackingParams);
        }

        return result;
    }

    private VideoInfo getVideoInfo(AppClient client, String videoId, String clickTrackingParams) {
        VideoInfo result;
        String clientPlaybackNonce = null;
        String visitorData = mAppService.getVisitorData();
        String playerScriptIdentity = null;
        String signatureTimestamp = null;
        PoTokenResult poTokens = null;
        boolean auth = client.isAuthSupported() && mUseAuth;

        if (client == AppClient.INITIAL) {
            result = InitialResponseService.getVideoInfo(videoId, auth);
        } else {
            mAppService.resetClientPlaybackNonce(); // unique value per client player request
            clientPlaybackNonce = mAppService.getClientPlaybackNonce();
            if (client == AppClient.VISIONOS) {
                visitorData = getVisionOsVisitorData(client);
                if (visitorData == null) {
                    Log.e(TAG, "VisionOS visitor bootstrap failed");
                    return null;
                }
            }
            poTokens = shouldPreparePoTokens(client) ?
                    PoTokenGate.getTokenResult(client, videoId, auth, null) : null;
            if (poTokens != null && !videoId.equals(poTokens.videoId)) {
                Log.e(TAG, "Rejecting mismatched player proof context; client=%s", client.name());
                return null;
            }
            visitorData = VideoInfoApiHelper.resolveVisitorData(poTokens, visitorData);
            playerScriptIdentity = YtCfgService.INSTANCE.getPlayerUrl(client, videoId);
            signatureTimestamp = mAppService.getSignatureTimestamp(playerScriptIdentity);
            String videoInfoQuery = VideoInfoApiHelper.getVideoInfoQuery(
                    client,
                    videoId,
                    clickTrackingParams,
                    poTokens,
                    clientPlaybackNonce,
                    VideoInfoApiHelper.resolveSignatureTimestamp(client, signatureTimestamp),
                    visitorData
            );
            result = executeVideoInfoRequest(client, videoId, videoInfoQuery, visitorData);
        }

        if (result != null) {
            result.setClient(client);
            result.setClientPlaybackNonce(clientPlaybackNonce);
            PlaybackRequestContext context = createPlaybackRequestContext(
                    videoId,
                    client,
                    auth,
                    visitorData,
                    result.getDataSyncId(),
                    clientPlaybackNonce,
                    playerScriptIdentity,
                    signatureTimestamp,
                    poTokens,
                    resolveExpiryAtEpochMs(result));
            result.setPlaybackRequestContext(context);
            result.setPoToken(context.getStreamingDataPoToken());
            result.setPlayerRequestPoToken(context.getPlayerRequestPoToken());
            Log.d(TAG, "Player response context: %s", context);
        }

        return result;
    }

    private static PlaybackRequestContext createPlaybackRequestContext(
            String videoId,
            AppClient client,
            boolean auth,
            String visitorData,
            String dataSyncId,
            String clientPlaybackNonce,
            String playerScriptIdentity,
            String signatureTimestamp,
            PoTokenResult poTokens,
            long expiresAtEpochMs) {
        PlaybackRequestContext.TokenBindingType bindingType = resolveStreamingBindingType(
                videoId, visitorData, dataSyncId, poTokens);
        return PlaybackRequestContext.builder(
                        NEXT_PLAYER_GENERATION.incrementAndGet(), videoId, client)
                .setAuthMode(auth ? PlaybackRequestContext.AuthMode.AUTHENTICATED :
                        PlaybackRequestContext.AuthMode.ANONYMOUS)
                .setVisitorData(visitorData)
                .setDataSyncId(dataSyncId)
                .setClientPlaybackNonce(clientPlaybackNonce)
                .setPlayerScriptIdentity(playerScriptIdentity)
                .setSignatureTimestamp(signatureTimestamp)
                .setSolverIdentity(playerScriptIdentity != null ? "PLAYER_SCRIPT" : null)
                .setPlayerRequestPoToken(poTokens != null ? poTokens.playerRequestPoToken : null)
                .setStreamingDataPoToken(poTokens != null ? poTokens.streamingDataPoToken : null)
                .setStreamingTokenBindingType(bindingType)
                .setStreamingProofRequired(poTokens != null &&
                        poTokens.streamingDataPoToken != null && client.isWebPoTokenSupported())
                .setExpiresAtEpochMs(expiresAtEpochMs)
                .build();
    }

    private static PlaybackRequestContext.TokenBindingType resolveStreamingBindingType(
            String videoId,
            String visitorData,
            String dataSyncId,
            PoTokenResult poTokens) {
        if (poTokens == null || poTokens.streamingDataPoToken == null) {
            return PlaybackRequestContext.TokenBindingType.NONE;
        }
        if (videoId.equals(poTokens.streamingDataContentBinding)) {
            return PlaybackRequestContext.TokenBindingType.VIDEO_ID;
        }
        if (dataSyncId != null && dataSyncId.equals(poTokens.streamingDataContentBinding)) {
            return PlaybackRequestContext.TokenBindingType.DATA_SYNC_ID;
        }
        if (visitorData != null && visitorData.equals(poTokens.streamingDataContentBinding)) {
            return PlaybackRequestContext.TokenBindingType.VISITOR_DATA;
        }
        throw new IllegalStateException("Streaming proof has an unknown binding");
    }

    private VideoInfo executeVideoInfoRequest(AppClient client, String videoId,
                                              String videoInfoQuery, String visitorData) {
        boolean auth = client.isAuthSupported() && mUseAuth;

        if (client.isReelClient()) {
            Call<VideoInfoReel> wrapper = mVideoInfoApi.getVideoInfoReel(videoInfoQuery, visitorData,
                    client.getUserAgent(), client.getInnerTubeName(), client.getClientVersion());
            return getVideoInfoReel(wrapper, auth);
        }

        if (client == AppClient.VISIONOS) {
            Call<VideoInfo> wrapper = mVideoInfoApi.getVideoInfoVisionOs(
                    videoInfoQuery, visitorData, client.getUserAgent(), client.getInnerTubeName(),
                    client.getClientVersion(), YouTubeHelper.generateTParameter(), videoId);
            return getVideoInfo(wrapper, auth);
        }

        Call<VideoInfo> wrapper = mVideoInfoApi.getVideoInfo(videoInfoQuery, visitorData,
                client.getUserAgent(), client.getInnerTubeName(), client.getClientVersion());
        return getVideoInfo(wrapper, auth);
    }

    private static long resolveExpiryAtEpochMs(VideoInfo videoInfo) {
        long expiry = minExpiry(-1, videoInfo.getHlsManifestUrl());
        expiry = minExpiry(expiry, videoInfo.getDashManifestUrl());
        expiry = minExpiry(expiry, videoInfo.getServerAbrStreamingUrl());
        expiry = minFormatExpiry(expiry, videoInfo.getAdaptiveFormats());
        return minFormatExpiry(expiry, videoInfo.getRegularFormats());
    }

    private static long minFormatExpiry(long expiry, List<? extends VideoFormat> formats) {
        if (formats != null) {
            for (VideoFormat format : formats) {
                if (format != null) {
                    expiry = minExpiry(expiry, format.getUrl());
                }
            }
        }
        return expiry;
    }

    private static long minExpiry(long current, String url) {
        long candidate = ManifestUrlTransformer.extractExpiryEpochMs(url);
        return candidate > 0 && (current <= 0 || candidate < current) ? candidate : current;
    }

    private @Nullable VideoInfo getVideoInfo(Call<VideoInfo> wrapper, boolean auth) {
        VideoInfo videoInfo = RetrofitHelper.get(wrapper, auth);

        if (videoInfo == null) {
            return null;
        }

        videoInfo.setAuth(auth);

        return videoInfo;
    }

    private @Nullable String getVisionOsVisitorData(AppClient client) {
        Call<VisitorDataResponse> wrapper = mVideoInfoApi.getVisionOsVisitorData(
                VideoInfoApiHelper.getVisitorDataQuery(client), client.getUserAgent(),
                client.getInnerTubeName(), client.getClientVersion());
        VisitorDataResponse response = RetrofitHelper.get(wrapper, false);
        String visitorData = response != null ? response.getVisitorData() : null;
        return visitorData != null && !visitorData.trim().isEmpty() ? visitorData : null;
    }

    private @Nullable VideoInfo getVideoInfoReel(Call<VideoInfoReel> wrapper, boolean auth) {
        VideoInfoReel videoInfo = RetrofitHelper.get(wrapper, auth);

        if (videoInfo == null || videoInfo.getVideoInfo() == null) {
            return null;
        }

        videoInfo.getVideoInfo().setAuth(auth);

        return videoInfo.getVideoInfo();
    }

    private VideoInfoHls getVideoInfoIOSHls(String videoId, String clickTrackingParams) {
        String videoInfoQuery = VideoInfoApiHelper.getVideoInfoQuery(IOS_CLIENT, videoId, clickTrackingParams);
        return getVideoInfoHls(IOS_CLIENT, videoInfoQuery);
    }

    private VideoInfoHls getVideoInfoHls(AppClient client, String videoInfoQuery) {
        Call<VideoInfoHls> wrapper = mVideoInfoApi.getVideoInfoHls(videoInfoQuery, mAppService.getVisitorData(),
                client.getUserAgent(), client.getInnerTubeName(), client.getClientVersion());

        return RetrofitHelper.get(wrapper, client.isAuthSupported() && mUseAuth);
    }

    private void applyFixesIfNeeded(VideoInfo result, String videoId, String clickTrackingParams) {
        if (result == null || !PlayerResponseAssessment.assess(result).isUsable()) {
            return;
        }

        boolean oldUseAuth = mUseAuth;

        if (shouldObtainExtendedFormats(result) || result.isStoryboardBroken()) {
            Log.d(TAG, "Enable high bitrate formats...");
            mUseAuth = false;
            VideoInfoHls videoInfoHls = getVideoInfoIOSHls(videoId, clickTrackingParams);
            if (videoInfoHls != null && shouldObtainExtendedFormats(result)) {
                result.setHlsManifestUrl(videoInfoHls.getHlsManifestUrl());
            }
            if (videoInfoHls != null && result.isStoryboardBroken()) {
                result.setStoryboardSpec(videoInfoHls.getStoryboardSpec());
            }
        }

        // TV and others has a limited number of auto generated subtitles
        if (needMoreSubtitles(result)) {
            Log.d(TAG, "Enable full list of auto generated subtitles...");

            if (mCachedTranslationLanguages == null || mCachedTranslationLanguages.size() < 100) {
                mUseAuth = false;
                VideoInfo webInfo = null;
                try {
                    webInfo = getVideoInfo(AppClient.WEB, videoId, clickTrackingParams);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (webInfo != null) {
                    mCachedTranslationLanguages = webInfo.getTranslationLanguages();
                }
            }

            if (mCachedTranslationLanguages != null) {
                result.setTranslationLanguages(mCachedTranslationLanguages);
            }
        }

        mUseAuth = oldUseAuth;
    }

    //private void restoreVideoInfoType() {
    //    int videoInfoType = getData().getVideoInfoType();
    //    if (videoInfoType != -1) {
    //        mActualInfoType = videoInfoType < AppClient.values().length ? AppClient.values()[videoInfoType] : null;
    //        if (!Arrays.asList(VIDEO_INFO_TYPE_LIST).contains(mActualInfoType)) {
    //            resetInfoTypeToDefault();
    //        }
    //    } else {
    //        resetInfoTypeToDefault();
    //    }
    //}

    private void resetInfoTypeToDefault() {
        mNextInfoType = null;
        mActualInfoType = VIDEO_INFO_TYPE_LIST[0];
        persistVideoInfoType();
    }

    private void persistVideoInfoType() {
        if (!GlobalPreferences.isInitialized()) {
            return;
        }

        getData().setVideoInfoType(mActualInfoType != null ? mActualInfoType.ordinal() : -1);
    }

    private void persistRecentTypeIfNeeded(VideoInfo videoInfo) {
        if (videoInfo == null || !PlayerResponseAssessment.assess(videoInfo).isUsable() ||
                videoInfo.getClient() == mActualInfoType) {
            return;
        }

        mActualInfoType = videoInfo.getClient();
        persistVideoInfoType();
    }

    private static boolean shouldObtainExtendedFormats(VideoInfo result) {
        return getData().isFormatEnabled(MediaServiceData.FORMATS_EXTENDED_HLS) && result.isExtendedHlsFormatsBroken();
    }

    private static boolean shouldUnlockMoreSubtitles(VideoInfo videoInfo) {
        return videoInfo != null && videoInfo.hasSubtitles() && getData().isMoreSubtitlesUnlocked();
    }

    private static boolean needMoreSubtitles(VideoInfo videoInfo) {
        return videoInfo != null && videoInfo.hasSubtitles() && (videoInfo.getTranslationLanguages() == null || videoInfo.getTranslationLanguages().size() < 100);
    }

    private static boolean isAuthSupported(AppClient client) {
        return client != null && client.isAuthSupported();
    }
}
