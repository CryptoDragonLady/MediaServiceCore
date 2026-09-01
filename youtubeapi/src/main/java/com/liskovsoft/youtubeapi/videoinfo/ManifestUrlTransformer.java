package com.liskovsoft.youtubeapi.videoinfo;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Protocol-aware transformations for YouTube HLS and DASH manifest URLs. */
public final class ManifestUrlTransformer {
    private static final Pattern N_PATH = Pattern.compile("/n/([^/?#]+)(?=/)");
    private static final Pattern MANIFEST_FILE =
            Pattern.compile("/(file|playlist)/index\\.m3u8(?=\\?|#|$)");
    private static final Pattern EXISTING_PROOF = Pattern.compile("/pot/[^/?#]+/");
    private static final Pattern EXPIRE_QUERY =
            Pattern.compile("(?:^|[?&])expire=([0-9]+)(?:&|$)");
    private static final Pattern EXPIRE_PATH =
            Pattern.compile("/expire/([0-9]+)(?=/)");

    private ManifestUrlTransformer() {
    }

    static String extractNChallenge(String url) {
        if (url == null) {
            return null;
        }
        Matcher matcher = N_PATH.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    static String replaceNChallenge(String url, String solvedChallenge) {
        if (url == null || solvedChallenge == null || solvedChallenge.isEmpty()) {
            return url;
        }
        Matcher matcher = N_PATH.matcher(url);
        return matcher.find() ? matcher.replaceFirst(
                Matcher.quoteReplacement("/n/" + solvedChallenge)) : url;
    }

    static String applyProofToken(String url, String proofToken) {
        if (url == null || proofToken == null || proofToken.isEmpty() ||
                EXISTING_PROOF.matcher(url).find()) {
            return url;
        }
        Matcher matcher = MANIFEST_FILE.matcher(url);
        if (!matcher.find()) {
            return url;
        }
        String encodedToken = encodePathSegment(proofToken);
        return matcher.replaceFirst(Matcher.quoteReplacement(
                "/pot/" + encodedToken + "/" + matcher.group(1) + "/index.m3u8"));
    }

    public static long extractExpiryEpochMs(String url) {
        if (url == null) {
            return -1;
        }
        Matcher matcher = EXPIRE_QUERY.matcher(url);
        if (!matcher.find()) {
            matcher = EXPIRE_PATH.matcher(url);
            if (!matcher.find()) {
                return -1;
            }
        }
        try {
            return Math.multiplyExact(Long.parseLong(matcher.group(1)), 1_000L);
        } catch (ArithmeticException | NumberFormatException error) {
            return -1;
        }
    }

    private static String encodePathSegment(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (UnsupportedEncodingException impossible) {
            throw new IllegalStateException("UTF-8 is unavailable", impossible);
        }
    }
}
