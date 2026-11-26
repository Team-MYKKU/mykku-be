package com.example.mykku.contract.support

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat
import java.util.concurrent.TimeUnit

data class ManifestContent(val base: String, val entries: Map<String, String>)

class GoldenManifest(private val root: Path = ContractGolden.CONTRACT_ROOT) {

    fun current(): Map<String, String> = Files.walk(root).use { paths ->
        paths.filter { ContractGolden.isGoldenFile(it) && it != manifestPath() }
            .map { root.relativize(it).joinToString("/") to sha256(it) }
            .toList()
            .toMap(sortedMapOf())
    }

    fun refreshed(recorded: Map<String, String>, touched: Set<String>): Map<String, String> =
        current().mapValues { (path, hash) -> if (path in touched) hash else recorded[path] ?: hash }.toSortedMap()

    fun read(): ManifestContent? {
        if (Files.notExists(manifestPath())) {
            return null
        }
        val lines = Files.readAllLines(manifestPath(), UTF_8).filter { it.isNotEmpty() }
        val base = lines.firstOrNull()?.removePrefix(BASE_PREFIX).orEmpty()
        val entries = lines.drop(1)
            .associate { it.substringAfter(ENTRY_SEPARATOR) to it.substringBefore(ENTRY_SEPARATOR) }
        return ManifestContent(base, entries)
    }

    fun write(base: String, entries: Map<String, String> = current()) {
        val lines = listOf(BASE_PREFIX + base) +
            entries.toSortedMap().map { (path, hash) -> "$hash$ENTRY_SEPARATOR$path" }
        Files.write(manifestPath(), lines.joinToString("\n", postfix = "\n").toByteArray(UTF_8))
    }

    private fun manifestPath(): Path = root.resolve(FILE_NAME)

    private fun sha256(path: Path): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)))

    companion object {
        const val FILE_NAME = "MANIFEST.sha256"
        private const val BASE_PREFIX = "base "
        private const val ENTRY_SEPARATOR = "  "
        private const val GIT_TIMEOUT_SECONDS = 10L
        private val COMMIT = Regex("[0-9a-f]{40}")

        fun isCommit(value: String): Boolean = COMMIT.matches(value)

        fun mergeBaseWithDevelop(): String {
            val process = ProcessBuilder("git", "merge-base", "HEAD", "origin/develop")
                .redirectErrorStream(true)
                .start()
            if (!process.waitFor(GIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                error("MANIFEST 기준 커밋을 정하지 못했습니다: git merge-base가 ${GIT_TIMEOUT_SECONDS}초 안에 끝나지 않았습니다")
            }
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            check(process.exitValue() == 0 && isCommit(output)) {
                "MANIFEST 기준 커밋을 정하지 못했습니다(git merge-base HEAD origin/develop): $output"
            }
            return output
        }
    }
}
