package com.example.mykku.contract.mvc

import java.nio.file.Path
import java.util.concurrent.TimeUnit

object GitFiles {

    private const val TIMEOUT_SECONDS = 10L
    private const val UNTRACKED_EXIT_CODE = 1

    fun isTracked(path: Path, directory: Path = Path.of("")): Boolean {
        val process = ProcessBuilder("git", "ls-files", "--error-unmatch", "--", path.toString())
            .directory(directory.toAbsolutePath().toFile())
            .redirectErrorStream(true)
            .start()
        awaitExit(process, path)
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        return when (process.exitValue()) {
            0 -> true
            UNTRACKED_EXIT_CODE -> false
            else -> error("git으로 추적 여부를 확인하지 못했습니다($path): $output")
        }
    }

    private fun awaitExit(process: Process, path: Path) {
        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            error("git ls-files가 ${TIMEOUT_SECONDS}초 안에 끝나지 않았습니다: $path")
        }
    }
}
