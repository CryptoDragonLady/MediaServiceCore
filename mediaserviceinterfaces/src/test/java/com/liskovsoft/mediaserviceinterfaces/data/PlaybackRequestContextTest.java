package com.liskovsoft.mediaserviceinterfaces.data;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class PlaybackRequestContextTest {
    private static final MediaItemFormatInfo.ClientInfo CLIENT = new MediaItemFormatInfo.ClientInfo() {
        @Override
        public String getClientName() {
            return "WEB";
        }

        @Override
        public String getClientVersion() {
            return "1.2.3";
        }

        @Override
        public String getOsName() {
            return "Test";
        }

        @Override
        public String getOsVersion() {
            return "1";
        }

        @Override
        public String getUserAgent() {
            return "private-user-agent";
        }
    };

    @Test
    public void diagnosticsNeverExposeOpaqueContextValues() {
        PlaybackRequestContext context = PlaybackRequestContext.builder(7, "abcdefghijk", CLIENT)
                .setVisitorData("private-visitor")
                .setDataSyncId("private-data-sync")
                .setClientPlaybackNonce("private-cpn")
                .setPlayerScriptIdentity("https://example.invalid/s/player/script/base.js")
                .setPlayerRequestPoToken("private-player-token")
                .setStreamingDataPoToken("private-streaming-token")
                .setStreamingTokenBindingType(PlaybackRequestContext.TokenBindingType.VIDEO_ID)
                .setStreamingProofRequired(true)
                .build();

        String diagnostic = context.toString();

        assertFalse(diagnostic.contains("private-"));
        assertFalse(diagnostic.contains("example.invalid"));
        assertTrue(diagnostic.contains("streamingTokenPresent=true"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void videoBoundContextRejectsAnotherVideo() {
        PlaybackRequestContext context = PlaybackRequestContext.builder(8, "abcdefghijk", CLIENT)
                .build();

        context.requireVideo("zyxwvutsrqp");
    }

    @Test
    public void eachAttemptKeepsItsOwnGenerationAndNonce() {
        PlaybackRequestContext first = PlaybackRequestContext.builder(9, "abcdefghijk", CLIENT)
                .setClientPlaybackNonce("nonce-one")
                .build();
        PlaybackRequestContext second = PlaybackRequestContext.builder(10, "abcdefghijk", CLIENT)
                .setClientPlaybackNonce("nonce-two")
                .build();

        assertNotEquals(first.getGenerationId(), second.getGenerationId());
        assertNotEquals(first.getClientPlaybackNonce(), second.getClientPlaybackNonce());
    }
}
