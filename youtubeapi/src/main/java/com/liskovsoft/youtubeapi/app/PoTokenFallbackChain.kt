package com.liskovsoft.youtubeapi.app

internal class PoTokenFallbackChain(
    private val providers: List<(String) -> String?>
) {
    fun getToken(contentBinding: String): String? {
        for (provider in providers) {
            try {
                provider(contentBinding)?.takeIf { it.isNotBlank() }?.let { return it }
            } catch (_: RuntimeException) {
                // Try the next configured provider.
            }
        }

        return null
    }
}
