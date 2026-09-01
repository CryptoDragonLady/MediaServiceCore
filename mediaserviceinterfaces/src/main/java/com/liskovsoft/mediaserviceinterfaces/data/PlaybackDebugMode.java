package com.liskovsoft.mediaserviceinterfaces.data;

/** One-shot transport override. Only the debug source set enables this. */
public final class PlaybackDebugMode {
    public enum Mode { NORMAL, FORCE_VISIONOS_HLS_REFERENCE }

    private static volatile Mode sMode = Mode.NORMAL;

    private PlaybackDebugMode() {
    }

    public static Mode get() {
        return sMode;
    }

    public static void setForDebug(Mode mode) {
        sMode = mode != null ? mode : Mode.NORMAL;
    }

    public static void clear() {
        sMode = Mode.NORMAL;
    }
}
