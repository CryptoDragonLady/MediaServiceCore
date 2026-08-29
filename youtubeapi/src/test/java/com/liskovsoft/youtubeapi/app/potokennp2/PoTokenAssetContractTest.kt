package com.liskovsoft.youtubeapi.app.potokennp2

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PoTokenAssetContractTest {
    @Test
    fun bundledMinterSupportsCurrentAsyncBotGuardContract() {
        val path = findAsset("youtubeapi/src/main/assets/potokennp2/po_token2.html")
        val script = String(Files.readAllBytes(path), Charsets.UTF_8)

        assertTrue(script.contains("loggerFunctions"))
        assertTrue(script.contains("Promise.resolve(botguard.vm.a"))
        assertTrue(script.contains("Promise.resolve(mintCallback(identifier))"))
        assertTrue(script.contains("Promise.resolve = function"))
    }

    private fun findAsset(relativePath: String): Path {
        var directory: Path? = Paths.get("").toAbsolutePath()

        while (directory != null) {
            val directCandidate = directory.resolve(relativePath)
            if (Files.exists(directCandidate)) {
                return directCandidate
            }

            val submoduleCandidate = directory.resolve("MediaServiceCore").resolve(relativePath)
            if (Files.exists(submoduleCandidate)) {
                return submoduleCandidate
            }

            directory = directory.parent
        }

        throw AssertionError("Could not locate $relativePath from ${Paths.get("").toAbsolutePath()}")
    }
}
