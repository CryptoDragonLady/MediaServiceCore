package com.liskovsoft.youtubeapi.videoinfo.models;

import org.junit.Test;

import com.liskovsoft.youtubeapi.videoinfo.models.PlayerResponseAssessment.Outcome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlayerResponseAssessmentTest {
    @Test
    public void okWithoutMediaIsEmptyStreamingData() {
        PlayerResponseAssessment assessment = assess(
                "OK", false, false, false, 0, 0, false, 0,
                false, false, false, false);

        assertEquals(Outcome.EMPTY_STREAMING_DATA, assessment.getOutcome());
        assertFalse(assessment.isUsable());
    }

    @Test
    public void liveHlsWinsInitialBringUpOrder() {
        PlayerResponseAssessment assessment = assess(
                "OK", true, true, false, 2, 2, true, 1,
                true, true, true, true);

        assertEquals(Outcome.USABLE_HLS_LIVE, assessment.getOutcome());
        assertTrue(assessment.isUsable());
    }

    @Test
    public void liveDashWinsWhenHlsIsMissing() {
        PlayerResponseAssessment assessment = assess(
                "OK", true, true, false, 2, 2, true, 0,
                false, true, true, true);

        assertEquals(Outcome.USABLE_DASH_LIVE, assessment.getOutcome());
    }

    @Test
    public void sabrRequiresEndpointConfigAndBothTrackTypes() {
        PlayerResponseAssessment complete = assess(
                "OK", false, false, false, 2, 1, false, 0,
                false, false, true, true);
        PlayerResponseAssessment missingAudio = assess(
                "OK", false, false, false, 2, 0, false, 0,
                false, false, true, true);

        assertEquals(Outcome.USABLE_SABR, complete.getOutcome());
        assertEquals(Outcome.EMPTY_STREAMING_DATA, missingAudio.getOutcome());
    }

    @Test
    public void offlineAndUpcomingRemainDistinct() {
        PlayerResponseAssessment offline = assess(
                "LIVE_STREAM_OFFLINE", false, true, false, 0, 0, false, 0,
                false, false, false, false);
        PlayerResponseAssessment upcoming = assess(
                "LIVE_STREAM_OFFLINE", false, true, true, 0, 0, false, 0,
                false, false, false, false);

        assertEquals(Outcome.OFFLINE, offline.getOutcome());
        assertEquals(Outcome.UPCOMING, upcoming.getOutcome());
    }

    private static PlayerResponseAssessment assess(
            String status,
            boolean live,
            boolean liveContent,
            boolean scheduled,
            int adaptiveVideo,
            int adaptiveAudio,
            boolean directAdaptive,
            int regular,
            boolean hls,
            boolean dash,
            boolean sabr,
            boolean ustreamer) {
        return PlayerResponseAssessment.assessSnapshot(
                status, live, liveContent, scheduled, adaptiveVideo, adaptiveAudio,
                directAdaptive, regular, hls, dash, sabr, ustreamer);
    }
}
