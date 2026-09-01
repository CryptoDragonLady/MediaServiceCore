package com.liskovsoft.mediaserviceinterfaces.data;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Immutable provenance for one YouTube player request attempt.
 *
 * <p>Tokens and session identifiers are intentionally available only through explicit getters;
 * {@link #toString()} emits presence flags and short hashes, never their values.</p>
 */
public final class PlaybackRequestContext {
    public enum AuthMode {
        ANONYMOUS,
        AUTHENTICATED
    }

    public enum TokenBindingType {
        NONE,
        VIDEO_ID,
        VISITOR_DATA,
        DATA_SYNC_ID
    }

    private final long mGenerationId;
    private final String mVideoId;
    private final ClientIdentity mRequestClient;
    private final AuthMode mAuthMode;
    private final String mVisitorData;
    private final String mDataSyncId;
    private final String mClientPlaybackNonce;
    private final String mPlayerScriptIdentity;
    private final String mSignatureTimestamp;
    private final String mSolverIdentity;
    private final String mPlayerRequestPoToken;
    private final String mStreamingDataPoToken;
    private final TokenBindingType mStreamingTokenBindingType;
    private final boolean mStreamingProofRequired;
    private final long mCreatedAtNanos;
    private final long mExpiresAtEpochMs;

    private PlaybackRequestContext(Builder builder) {
        mGenerationId = builder.mGenerationId;
        mVideoId = requireText(builder.mVideoId, "videoId");
        mRequestClient = new ClientIdentity(Objects.requireNonNull(builder.mRequestClient, "requestClient"));
        mAuthMode = Objects.requireNonNull(builder.mAuthMode, "authMode");
        mVisitorData = builder.mVisitorData;
        mDataSyncId = builder.mDataSyncId;
        mClientPlaybackNonce = builder.mClientPlaybackNonce;
        mPlayerScriptIdentity = builder.mPlayerScriptIdentity;
        mSignatureTimestamp = builder.mSignatureTimestamp;
        mSolverIdentity = builder.mSolverIdentity;
        mPlayerRequestPoToken = builder.mPlayerRequestPoToken;
        mStreamingDataPoToken = builder.mStreamingDataPoToken;
        mStreamingTokenBindingType = Objects.requireNonNull(
                builder.mStreamingTokenBindingType,
                "streamingTokenBindingType");
        mStreamingProofRequired = builder.mStreamingProofRequired;
        mCreatedAtNanos = builder.mCreatedAtNanos;
        mExpiresAtEpochMs = builder.mExpiresAtEpochMs;

        if (mGenerationId <= 0) {
            throw new IllegalArgumentException("generationId must be positive");
        }
        if (mStreamingDataPoToken == null && mStreamingTokenBindingType != TokenBindingType.NONE) {
            throw new IllegalArgumentException("A streaming token binding requires a streaming token");
        }
        if (mStreamingProofRequired && mStreamingDataPoToken == null) {
            throw new IllegalArgumentException("A required streaming proof cannot be absent");
        }
    }

    public static Builder builder(long generationId, String videoId, MediaItemFormatInfo.ClientInfo requestClient) {
        return new Builder(generationId, videoId, requestClient);
    }

    public long getGenerationId() {
        return mGenerationId;
    }

    public String getVideoId() {
        return mVideoId;
    }

    public MediaItemFormatInfo.ClientInfo getRequestClient() {
        return mRequestClient;
    }

    public AuthMode getAuthMode() {
        return mAuthMode;
    }

    public String getVisitorData() {
        return mVisitorData;
    }

    public String getDataSyncId() {
        return mDataSyncId;
    }

    public String getClientPlaybackNonce() {
        return mClientPlaybackNonce;
    }

    public String getPlayerScriptIdentity() {
        return mPlayerScriptIdentity;
    }

    public String getSignatureTimestamp() {
        return mSignatureTimestamp;
    }

    public String getSolverIdentity() {
        return mSolverIdentity;
    }

    public String getPlayerRequestPoToken() {
        return mPlayerRequestPoToken;
    }

    public String getStreamingDataPoToken() {
        return mStreamingDataPoToken;
    }

    public TokenBindingType getStreamingTokenBindingType() {
        return mStreamingTokenBindingType;
    }

    public boolean isStreamingProofRequired() {
        return mStreamingProofRequired;
    }

    public long getCreatedAtNanos() {
        return mCreatedAtNanos;
    }

    public long getExpiresAtEpochMs() {
        return mExpiresAtEpochMs;
    }

    public boolean isExpired(long nowEpochMs) {
        return mExpiresAtEpochMs > 0 && nowEpochMs >= mExpiresAtEpochMs;
    }

    public void requireVideo(String videoId) {
        if (!mVideoId.equals(videoId)) {
            throw new IllegalArgumentException("Playback context belongs to another video");
        }
    }

    public void requireGeneration(long generationId) {
        if (mGenerationId != generationId) {
            throw new IllegalStateException("Playback context belongs to a retired generation");
        }
    }

    @Override
    public String toString() {
        return "PlaybackRequestContext{" +
                "generationId=" + mGenerationId +
                ", videoIdSuffix=" + suffix(mVideoId) +
                ", client=" + safeLabel(mRequestClient.getClientName()) +
                ", clientVersion=" + safeLabel(mRequestClient.getClientVersion()) +
                ", userAgentHash=" + shortHash(mRequestClient.getUserAgent()) +
                ", authMode=" + mAuthMode +
                ", visitorHash=" + shortHash(mVisitorData) +
                ", dataSyncHash=" + shortHash(mDataSyncId) +
                ", cpnHash=" + shortHash(mClientPlaybackNonce) +
                ", playerScriptHash=" + shortHash(mPlayerScriptIdentity) +
                ", signatureTimestampPresent=" + (mSignatureTimestamp != null) +
                ", playerTokenPresent=" + (mPlayerRequestPoToken != null) +
                ", streamingTokenPresent=" + (mStreamingDataPoToken != null) +
                ", streamingBinding=" + mStreamingTokenBindingType +
                ", streamingProofRequired=" + mStreamingProofRequired +
                '}';
    }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static String suffix(String value) {
        if (value == null) {
            return "none";
        }
        return value.substring(Math.max(0, value.length() - 4));
    }

    private static String safeLabel(String value) {
        return value != null && value.matches("[A-Za-z0-9._-]{1,64}") ? value : "unknown";
    }

    private static String shortHash(String value) {
        if (value == null || value.isEmpty()) {
            return "none";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(12);
            for (int i = 0; i < 6; i++) {
                result.append(String.format("%02x", bytes[i]));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException error) {
            return "unavailable";
        }
    }

    public static final class Builder {
        private final long mGenerationId;
        private final String mVideoId;
        private final MediaItemFormatInfo.ClientInfo mRequestClient;
        private AuthMode mAuthMode = AuthMode.ANONYMOUS;
        private String mVisitorData;
        private String mDataSyncId;
        private String mClientPlaybackNonce;
        private String mPlayerScriptIdentity;
        private String mSignatureTimestamp;
        private String mSolverIdentity;
        private String mPlayerRequestPoToken;
        private String mStreamingDataPoToken;
        private TokenBindingType mStreamingTokenBindingType = TokenBindingType.NONE;
        private boolean mStreamingProofRequired;
        private long mCreatedAtNanos = System.nanoTime();
        private long mExpiresAtEpochMs = -1;

        private Builder(long generationId, String videoId, MediaItemFormatInfo.ClientInfo requestClient) {
            mGenerationId = generationId;
            mVideoId = videoId;
            mRequestClient = requestClient;
        }

        public Builder setAuthMode(AuthMode authMode) {
            mAuthMode = authMode;
            return this;
        }

        public Builder setVisitorData(String visitorData) {
            mVisitorData = visitorData;
            return this;
        }

        public Builder setDataSyncId(String dataSyncId) {
            mDataSyncId = dataSyncId;
            return this;
        }

        public Builder setClientPlaybackNonce(String clientPlaybackNonce) {
            mClientPlaybackNonce = clientPlaybackNonce;
            return this;
        }

        public Builder setPlayerScriptIdentity(String playerScriptIdentity) {
            mPlayerScriptIdentity = playerScriptIdentity;
            return this;
        }

        public Builder setSignatureTimestamp(String signatureTimestamp) {
            mSignatureTimestamp = signatureTimestamp;
            return this;
        }

        public Builder setSolverIdentity(String solverIdentity) {
            mSolverIdentity = solverIdentity;
            return this;
        }

        public Builder setPlayerRequestPoToken(String playerRequestPoToken) {
            mPlayerRequestPoToken = playerRequestPoToken;
            return this;
        }

        public Builder setStreamingDataPoToken(String streamingDataPoToken) {
            mStreamingDataPoToken = streamingDataPoToken;
            return this;
        }

        public Builder setStreamingTokenBindingType(TokenBindingType tokenBindingType) {
            mStreamingTokenBindingType = tokenBindingType;
            return this;
        }

        public Builder setStreamingProofRequired(boolean streamingProofRequired) {
            mStreamingProofRequired = streamingProofRequired;
            return this;
        }

        public Builder setCreatedAtNanos(long createdAtNanos) {
            mCreatedAtNanos = createdAtNanos;
            return this;
        }

        public Builder setExpiresAtEpochMs(long expiresAtEpochMs) {
            mExpiresAtEpochMs = expiresAtEpochMs;
            return this;
        }

        public PlaybackRequestContext build() {
            return new PlaybackRequestContext(this);
        }
    }

    private static final class ClientIdentity implements MediaItemFormatInfo.ClientInfo {
        private final String mClientName;
        private final String mClientVersion;
        private final String mOsName;
        private final String mOsVersion;
        private final String mUserAgent;

        private ClientIdentity(MediaItemFormatInfo.ClientInfo source) {
            mClientName = source.getClientName();
            mClientVersion = source.getClientVersion();
            mOsName = source.getOsName();
            mOsVersion = source.getOsVersion();
            mUserAgent = source.getUserAgent();
        }

        @Override
        public String getClientName() {
            return mClientName;
        }

        @Override
        public String getClientVersion() {
            return mClientVersion;
        }

        @Override
        public String getOsName() {
            return mOsName;
        }

        @Override
        public String getOsVersion() {
            return mOsVersion;
        }

        @Override
        public String getUserAgent() {
            return mUserAgent;
        }
    }
}
