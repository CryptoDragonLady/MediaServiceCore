package com.liskovsoft.youtubeapi.videoinfo.V2;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.liskovsoft.youtubeapi.common.helpers.AppClient;
import com.liskovsoft.youtubeapi.videoinfo.models.PlayerResponseAssessment.Outcome;

import org.junit.Test;

public class AuthenticatedFallbackPolicyTest {
    @Test
    public void signedInLoginChallengeUsesAuthenticatedFallback() {
        assertTrue(VideoInfoService.shouldTryAuthenticatedFallback(
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
    public void authenticatedTvFallbackDoesNotAttachWebProofTokens() {
        assertFalse(VideoInfoService.shouldPreparePoTokens(AppClient.TV));
        assertFalse(VideoInfoService.shouldPreparePoTokens(AppClient.TV_DOWNGRADED));
        assertFalse(VideoInfoService.shouldPreparePoTokens(AppClient.TV_SIMPLY));
        assertTrue(VideoInfoService.shouldPreparePoTokens(AppClient.WEB));
        assertTrue(VideoInfoService.shouldPreparePoTokens(AppClient.WEB_EMBED));
        assertFalse(VideoInfoService.shouldPreparePoTokens(AppClient.IOS));
    }
}
