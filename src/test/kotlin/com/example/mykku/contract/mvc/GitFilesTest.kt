package com.example.mykku.contract.mvc

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

@DisplayName("git 추적 여부 판정")
class GitFilesTest {

    @TempDir
    private lateinit var root: Path

    @Test
    fun `git이 추적하는 파일과 추적하지 않는 파일을 구분한다`() {
        val repository = Files.createDirectory(root.resolve("repository"))
        git(repository, "init", "-q")
        Files.writeString(repository.resolve("tracked.yml"), "a: 1\n")
        Files.writeString(repository.resolve("untracked.yml"), "b: 2\n")
        git(repository, "add", "tracked.yml")

        assertThat(GitFiles.isTracked(Path.of("tracked.yml"), repository)).isTrue()
        assertThat(GitFiles.isTracked(Path.of("untracked.yml"), repository)).isFalse()
    }

    @Test
    fun `git 저장소 밖에서는 추적 여부를 정하지 못해 실패한다`() {
        val outside = Files.createDirectory(root.resolve("outside"))
        Files.writeString(outside.resolve("file.yml"), "a: 1\n")
        Files.writeString(outside.resolve(".git"), "gitdir: /nonexistent\n")

        assertThatThrownBy { GitFiles.isTracked(Path.of("file.yml"), outside) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    private fun git(directory: Path, vararg arguments: String) {
        val process = ProcessBuilder(listOf("git") + arguments).directory(directory.toFile()).inheritIO().start()
        val finished = process.waitFor(GIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        check(finished && process.exitValue() == 0) { "git ${arguments.toList()} 실패" }
    }

    private companion object {
        const val GIT_TIMEOUT_SECONDS = 10L
    }
}
