package com.liskovsoft.youtubeapi.videoinfo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class ManifestUrlTransformerTest {
    @Test
    public void extractsAndReplacesPathNChallengeWithoutTouchingQuery() {
        String source = "https://manifest.googlevideo.com/api/manifest/hls_variant/n/original/file/index.m3u8?expire=1&keep=n%2Foriginal";

        assertEquals("original", ManifestUrlTransformer.extractNChallenge(source));
        assertEquals(
                "https://manifest.googlevideo.com/api/manifest/hls_variant/n/solved/file/index.m3u8?expire=1&keep=n%2Foriginal",
                ManifestUrlTransformer.replaceNChallenge(source, "solved"));
    }

    @Test
    public void ignoresQueryNForManifestChallenge() {
        assertNull(ManifestUrlTransformer.extractNChallenge(
                "https://manifest.googlevideo.com/api/manifest/hls_variant/file/index.m3u8?n=query-only"));
    }

    @Test
    public void insertsManifestProofIntoPathBeforeFileName() {
        String transformed = ManifestUrlTransformer.applyProofToken(
                "https://manifest.googlevideo.com/api/manifest/hls_variant/n/solved/playlist/index.m3u8?expire=1",
                "proof_token-1");

        assertEquals(
                "https://manifest.googlevideo.com/api/manifest/hls_variant/n/solved/pot/proof_token-1/playlist/index.m3u8?expire=1",
                transformed);
        assertFalse(transformed.contains("?pot="));
        assertFalse(transformed.contains("&pot="));
    }

    @Test
    public void absentProofLeavesVisionOsManifestUnchanged() {
        String source = "https://manifest.googlevideo.com/api/manifest/hls_variant/file/index.m3u8?expire=1";
        assertEquals(source, ManifestUrlTransformer.applyProofToken(source, null));
    }

    @Test
    public void extractsExpiryFromManifestPathOrQuery() {
        assertEquals(1_800_000L, ManifestUrlTransformer.extractExpiryEpochMs(
                "https://manifest.googlevideo.com/api/manifest/hls_variant/expire/1800/file/index.m3u8"));
        assertEquals(2_400_000L, ManifestUrlTransformer.extractExpiryEpochMs(
                "https://example.googlevideo.com/videoplayback?foo=1&expire=2400&bar=2"));
        assertEquals(-1L, ManifestUrlTransformer.extractExpiryEpochMs(
                "https://manifest.googlevideo.com/api/manifest/hls_variant/file/index.m3u8"));
    }
}
