package com.liskovsoft.youtubeapi.app.potokennp2.visitor

import com.liskovsoft.youtubeapi.common.helpers.AppClient
import org.junit.Assert.assertEquals
import org.junit.Test

class VisitorApiHelperTest {
    @Test
    fun visitorRequestUsesTheWebPlayerClientContext() {
        assertEquals(AppClient.WEB.userAgent, VisitorApiHelper.getUserAgent())
        assertEquals(AppClient.WEB.innerTubeName, VisitorApiHelper.getClientName())
        assertEquals(AppClient.WEB.clientVersion, VisitorApiHelper.getClientVersion())
    }
}
