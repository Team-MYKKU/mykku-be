package com.example.mykku.contract.mvc

import java.nio.file.Path
import java.util.concurrent.TimeUnit

object GitFiles {

    private const val TIMEOUT_SECONDS = 10L
    private const val UNTRACKED_EXIT_CODE = 1

    fun isTracked(path: Path): Boolean {
        val process = ProcessBuilder("git", "ls-files", "--error-unmatch", "--", path.toString())
            .redirectErrorStream(true)
            .start()
        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            error("git ls-files가 ${TIMEOUT_SECONDS}초 안에 끝나지 않았습니다: $path")
        }
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        return when (process.exitValue()) {
            0 -> true
            UNTRACKED_EXIT_CODE -> false
            else -> error("git으로 추적 여부를 확인하지 못했습니다($path): $output")
        }
    }
}
