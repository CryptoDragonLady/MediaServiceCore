package com.liskovsoft.youtubeapi.app.potokennp2.visitor

import com.liskovsoft.youtubeapi.common.helpers.AppClient
import com.liskovsoft.youtubeapi.common.helpers.QueryBuilder

internal object VisitorApiHelper {
    fun getVisitorQuery(): String {
        return QueryBuilder(AppClient.WEB)
            .build()
    }

    fun getUserAgent() = AppClient.WEB.userAgent
    fun getClientName() = AppClient.WEB.innerTubeName
    fun getClientVersion() = AppClient.WEB.clientVersion
}
