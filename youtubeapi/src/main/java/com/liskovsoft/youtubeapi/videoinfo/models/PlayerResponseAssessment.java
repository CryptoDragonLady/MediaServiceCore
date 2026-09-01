package com.liskovsoft.youtubeapi.videoinfo.models;

import com.liskovsoft.youtubeapi.videoinfo.models.formats.AdaptiveVideoFormat;
import com.liskovsoft.youtubeapi.videoinfo.models.formats.RegularVideoFormat;

import java.util.List;

/** Classifies whether a player response contains a source SmartTube can actually prepare. */
public final class PlayerResponseAssessment {
    public enum Outcome {
        USABLE_HLS_LIVE,
        USABLE_DASH_LIVE,
        USABLE_ADAPTIVE,
        USABLE_DIRECT,
        USABLE_SABR,
        UPCOMING,
        OFFLINE,
        RESTRICTED,
        LOGIN_REQUIRED,
        AGE_RESTRICTED,
        EMPTY_STREAMING_DATA,
        CLIENT_UNSUPPORTED,
        TOKEN_CONTEXT_FAILED,
        SCRIPT_CHALLENGE_FAILED,
        PLAYER_HTTP_ERROR,
        MALFORMED_RESPONSE
    }

    private static final String STATUS_OK = "OK";
    private static final String STATUS_UNPLAYABLE = "UNPLAYABLE";
    private static final String STATUS_ERROR = "ERROR";
    private static final String STATUS_OFFLINE = "LIVE_STREAM_OFFLINE";
    private static final String STATUS_LOGIN_REQUIRED = "LOGIN_REQUIRED";
    private static final String STATUS_AGE_CHECK_REQUIRED = "AGE_CHECK_REQUIRED";
    private static final String STATUS_AGE_VERIFICATION_REQUIRED = "AGE_VERIFICATION_REQUIRED";
    private static final String STATUS_CONTENT_CHECK_REQUIRED = "CONTENT_CHECK_REQUIRED";

    private final Outcome mOutcome;
    private final boolean mLive;
    private final boolean mLiveContent;
    private final int mAdaptiveVideoCount;
    private final int mAdaptiveAudioCount;
    private final int mRegularFormatCount;
    private final boolean mHlsPresent;
    private final boolean mDashPresent;
    private final boolean mSabrPresent;
    private final boolean mUstreamerPresent;

    private PlayerResponseAssessment(
            Outcome outcome,
            boolean live,
            boolean liveContent,
            int adaptiveVideoCount,
            int adaptiveAudioCount,
            int regularFormatCount,
            boolean hlsPresent,
            boolean dashPresent,
            boolean sabrPresent,
            boolean ustreamerPresent) {
        mOutcome = outcome;
        mLive = live;
        mLiveContent = liveContent;
        mAdaptiveVideoCount = adaptiveVideoCount;
        mAdaptiveAudioCount = adaptiveAudioCount;
        mRegularFormatCount = regularFormatCount;
        mHlsPresent = hlsPresent;
        mDashPresent = dashPresent;
        mSabrPresent = sabrPresent;
        mUstreamerPresent = ustreamerPresent;
    }

    public static PlayerResponseAssessment assess(VideoInfo videoInfo) {
        if (videoInfo == null) {
            return snapshot(Outcome.MALFORMED_RESPONSE, false, false, 0, 0, 0,
                    false, false, false, false);
        }

        int adaptiveVideoCount = countAdaptive(videoInfo.getAdaptiveFormats(), true);
        int adaptiveAudioCount = countAdaptive(videoInfo.getAdaptiveFormats(), false);
        int regularFormatCount = countRegular(videoInfo.getRegularFormats());
        VideoDetails details = videoInfo.getVideoDetails();

        return assessSnapshot(
                videoInfo.getRawPlayabilityStatus(),
                details != null && details.isLive(),
                details != null && details.isLiveContent(),
                videoInfo.getStartTimestamp() != null,
                adaptiveVideoCount,
                adaptiveAudioCount,
                countDirectAdaptive(videoInfo.getAdaptiveFormats(), true) > 0 &&
                        countDirectAdaptive(videoInfo.getAdaptiveFormats(), false) > 0,
                regularFormatCount,
                hasText(videoInfo.getHlsManifestUrl()),
                hasText(videoInfo.getDashManifestUrl()),
                hasText(videoInfo.getServerAbrStreamingUrl()),
                hasText(videoInfo.getVideoPlaybackUstreamerConfig()));
    }

    static PlayerResponseAssessment assessSnapshot(
            String playabilityStatus,
            boolean live,
            boolean liveContent,
            boolean scheduledStartPresent,
            int adaptiveVideoCount,
            int adaptiveAudioCount,
            boolean directAdaptivePresent,
            int regularFormatCount,
            boolean hlsPresent,
            boolean dashPresent,
            boolean sabrPresent,
            boolean ustreamerPresent) {
        Outcome restrictedOutcome = restrictedOutcome(playabilityStatus, scheduledStartPresent);
        if (restrictedOutcome != null) {
            return snapshot(restrictedOutcome, live, liveContent, adaptiveVideoCount,
                    adaptiveAudioCount, regularFormatCount, hlsPresent, dashPresent,
                    sabrPresent, ustreamerPresent);
        }

        boolean statusAcceptsPlayback = playabilityStatus == null || STATUS_OK.equals(playabilityStatus);
        if (!statusAcceptsPlayback) {
            return snapshot(Outcome.CLIENT_UNSUPPORTED, live, liveContent, adaptiveVideoCount,
                    adaptiveAudioCount, regularFormatCount, hlsPresent, dashPresent,
                    sabrPresent, ustreamerPresent);
        }

        Outcome outcome;
        if (live && hlsPresent) {
            outcome = Outcome.USABLE_HLS_LIVE;
        } else if (live && dashPresent) {
            outcome = Outcome.USABLE_DASH_LIVE;
        } else if (directAdaptivePresent) {
            outcome = Outcome.USABLE_ADAPTIVE;
        } else if (regularFormatCount > 0) {
            outcome = Outcome.USABLE_DIRECT;
        } else if (sabrPresent && ustreamerPresent &&
                adaptiveVideoCount > 0 && adaptiveAudioCount > 0) {
            outcome = Outcome.USABLE_SABR;
        } else {
            outcome = Outcome.EMPTY_STREAMING_DATA;
        }

        return snapshot(outcome, live, liveContent, adaptiveVideoCount, adaptiveAudioCount,
                regularFormatCount, hlsPresent, dashPresent, sabrPresent, ustreamerPresent);
    }

    private static Outcome restrictedOutcome(String status, boolean scheduledStartPresent) {
        if (STATUS_OFFLINE.equals(status)) {
            return scheduledStartPresent ? Outcome.UPCOMING : Outcome.OFFLINE;
        }
        if (STATUS_LOGIN_REQUIRED.equals(status)) {
            return Outcome.LOGIN_REQUIRED;
        }
        if (STATUS_AGE_CHECK_REQUIRED.equals(status) ||
                STATUS_AGE_VERIFICATION_REQUIRED.equals(status) ||
                STATUS_CONTENT_CHECK_REQUIRED.equals(status)) {
            return Outcome.AGE_RESTRICTED;
        }
        if (STATUS_UNPLAYABLE.equals(status) || STATUS_ERROR.equals(status)) {
            return Outcome.RESTRICTED;
        }
        return null;
    }

    private static PlayerResponseAssessment snapshot(
            Outcome outcome,
            boolean live,
            boolean liveContent,
            int adaptiveVideoCount,
            int adaptiveAudioCount,
            int regularFormatCount,
            boolean hlsPresent,
            boolean dashPresent,
            boolean sabrPresent,
            boolean ustreamerPresent) {
        return new PlayerResponseAssessment(outcome, live, liveContent, adaptiveVideoCount,
                adaptiveAudioCount, regularFormatCount, hlsPresent, dashPresent,
                sabrPresent, ustreamerPresent);
    }

    private static int countAdaptive(List<AdaptiveVideoFormat> formats, boolean video) {
        if (formats == null) {
            return 0;
        }
        int count = 0;
        for (AdaptiveVideoFormat format : formats) {
            String mimeType = format != null ? format.getMimeType() : null;
            if (format != null && mimeType != null &&
                    mimeType.startsWith(video ? "video/" : "audio/")) {
                count++;
            }
        }
        return count;
    }

    private static int countDirectAdaptive(List<AdaptiveVideoFormat> formats, boolean video) {
        if (formats == null) {
            return 0;
        }
        int count = 0;
        for (AdaptiveVideoFormat format : formats) {
            String mimeType = format != null ? format.getMimeType() : null;
            if (format != null && !format.isBroken() && mimeType != null &&
                    mimeType.startsWith(video ? "video/" : "audio/")) {
                count++;
            }
        }
        return count;
    }

    private static int countRegular(List<RegularVideoFormat> formats) {
        if (formats == null) {
            return 0;
        }
        int count = 0;
        for (RegularVideoFormat format : formats) {
            if (format != null && !format.isBroken()) {
                count++;
            }
        }
        return count;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public Outcome getOutcome() {
        return mOutcome;
    }

    public boolean isUsable() {
        switch (mOutcome) {
            case USABLE_HLS_LIVE:
            case USABLE_DASH_LIVE:
            case USABLE_ADAPTIVE:
            case USABLE_DIRECT:
            case USABLE_SABR:
                return true;
            default:
                return false;
        }
    }

    /** Higher values are preferred while comparing bounded active-live client responses. */
    public int getLiveTransportPriority() {
        switch (mOutcome) {
            case USABLE_HLS_LIVE: return 5;
            case USABLE_DASH_LIVE: return 4;
            case USABLE_ADAPTIVE: return 3;
            case USABLE_DIRECT: return 2;
            case USABLE_SABR: return 1;
            default: return 0;
        }
    }

    public boolean isLive() {
        return mLive;
    }

    public boolean isLiveContent() {
        return mLiveContent;
    }

    public int getAdaptiveVideoCount() {
        return mAdaptiveVideoCount;
    }

    public int getAdaptiveAudioCount() {
        return mAdaptiveAudioCount;
    }

    public int getRegularFormatCount() {
        return mRegularFormatCount;
    }

    public boolean isHlsPresent() {
        return mHlsPresent;
    }

    public boolean isDashPresent() {
        return mDashPresent;
    }

    public boolean isSabrPresent() {
        return mSabrPresent;
    }

    public boolean isUstreamerPresent() {
        return mUstreamerPresent;
    }

    @Override
    public String toString() {
        return "PlayerResponseAssessment{" +
                "outcome=" + mOutcome +
                ", live=" + mLive +
                ", liveContent=" + mLiveContent +
                ", adaptiveVideo=" + mAdaptiveVideoCount +
                ", adaptiveAudio=" + mAdaptiveAudioCount +
                ", regular=" + mRegularFormatCount +
                ", hls=" + mHlsPresent +
                ", dash=" + mDashPresent +
                ", sabr=" + mSabrPresent +
                ", ustreamer=" + mUstreamerPresent +
                '}';
    }
}
