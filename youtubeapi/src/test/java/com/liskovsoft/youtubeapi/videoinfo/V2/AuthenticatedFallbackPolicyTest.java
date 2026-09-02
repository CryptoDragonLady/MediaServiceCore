package com.liskovsoft.youtubeapi.videoinfo.V2;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.liskovsoft.youtubeapi.common.helpers.AppClient;
import com.liskovsoft.youtubeapi.videoinfo.models.PlayerResponseAssessment.Outcome;

import org.junit.Test;

public class AuthenticatedFallbackPolicyTest {
    @Test
    public void preferredPlaybackCandidatesStartWithKnownGoodClients() {
        AppClient[] order = VideoInfoService.playbackClientOrder();

        assertArrayEquals(new AppClient[] {
                AppClient.WEB,
                AppClient.VISIONOS,
                AppClient.MWEB,
                AppClient.WEB_EMBED
        }, java.util.Arrays.copyOf(order, 4));
    }

    @Test
    public void signedInLoginChallengeDoesNotUseDisabledTvFallback() {
        assertFalse(VideoInfoService.shouldTryAuthenticatedFallback(
                Outcome.LOGIN_REQUIRED, true));
    }

    @Test
    public void anonymousOrNonLoginFailuresDoNotAddAuthenticationRequests() {
        assertFalse(VideoInfoService.shouldTryAuthenticatedFallback(
                Outcome.LOGIN_REQUIRED, false));
        assertFalse(VideoInfoService.shouldTryAuthenticatedFallback(
                Outcome.RESTRICTED, true));
        assertFalse(VideoInfoService.shouldTryAuthenticatedFallback(
                Outcome.EMPTY_STREAMING_DATA, true));
    }

    @Test
    public void rotatingAfterTvFailureSuppressesImmediateTvFallback() {
        assertFalse(VideoInfoService.shouldTryAuthenticatedFallback(
                Outcome.LOGIN_REQUIRED, true, true));
        assertFalse(VideoInfoService.shouldTryAuthenticatedFallback(
                Outcome.LOGIN_REQUIRED, true, false));

        assertTrue(VideoInfoService.shouldSkipAuthenticatedFallbackAfter(AppClient.TV));
        assertTrue(VideoInfoService.shouldSkipAuthenticatedFallbackAfter(AppClient.TV_DOWNGRADED));
        assertFalse(VideoInfoService.shouldSkipAuthenticatedFallbackAfter(AppClient.WEB));
        assertFalse(VideoInfoService.shouldSkipAuthenticatedFallbackAfter(null));
    }

    @Test
    public void tvPlaybackClientsAreDisabledWhileBrowserClientsRemainEnabled() {
        assertFalse(VideoInfoService.isPlaybackClientEnabled(AppClient.TV));
        assertFalse(VideoInfoService.isPlaybackClientEnabled(AppClient.TV_DOWNGRADED));
        assertTrue(VideoInfoService.isPlaybackClientEnabled(AppClient.WEB));
        assertTrue(VideoInfoService.isPlaybackClientEnabled(AppClient.VISIONOS));
        assertTrue(VideoInfoService.isPlaybackClientEnabled(AppClient.MWEB));
        assertTrue(VideoInfoService.isPlaybackClientEnabled(AppClient.WEB_EMBED));
    }

    @Test
    public void authenticatedTvFallbackDoesNotAttachWebProofTokens() {
        assertFalse(VideoInfoService.shouldPreparePoTokens(AppClient.TV));
        assertFalse(VideoInfoService.shouldPreparePoTokens(AppClient.TV_DOWNGRADED));
        assertFalse(VideoInfoService.shouldPreparePoTokens(AppClient.TV_SIMPLY));
        assertTrue(VideoInfoService.shouldPreparePoTokens(AppClient.WEB));
        assertTrue(VideoInfoService.shouldPreparePoTokens(AppClient.WEB_EMBED));
        assertFalse(VideoInfoService.shouldPreparePoTokens(AppClient.IOS));
    }
}
