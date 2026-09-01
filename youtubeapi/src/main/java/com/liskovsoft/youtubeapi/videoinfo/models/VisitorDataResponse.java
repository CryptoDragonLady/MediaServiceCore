package com.liskovsoft.youtubeapi.videoinfo.models;

import com.liskovsoft.googlecommon.common.converters.jsonpath.JsonPath;

/** Minimal visitor response used to bind a native player request to its own session. */
public final class VisitorDataResponse {
    @JsonPath("$.responseContext.visitorData")
    private String mVisitorData;

    public String getVisitorData() {
        return mVisitorData;
    }
}
