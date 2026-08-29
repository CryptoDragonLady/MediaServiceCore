package com.liskovsoft.youtubeapi.app

internal object PlayerScriptCandidates {
    @JvmStatic
    fun resolve(requestedPlayerUrl: String?, fallbacks: List<String?>): List<String> {
        return requestedPlayerUrl?.let(::listOf)
            ?: fallbacks.filterNotNull().distinct()
    }
}
