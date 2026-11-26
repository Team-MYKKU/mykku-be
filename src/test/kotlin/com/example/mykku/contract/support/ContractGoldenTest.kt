package com.example.mykku.contract.support

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

@DisplayName("ContractGolden 비교·갱신")
class ContractGoldenTest {

    @TempDir
    lateinit var tempDir: Path

    private val goldenRoot: Path by lazy { tempDir.resolve("golden") }
    private val actualRoot: Path by lazy { tempDir.resolve("actual") }

    @Test
    fun `골든과 바이트가 같으면 통과하고 실제 출력을 남긴다`() {
        writeGolden("wire/a.txt", "line1\nline2\n")

        golden(update = false).verify("wire/a.txt", "line1\r\nline2\r\n")

        assertThat(actualRoot.resolve("wire/a.txt").readText()).isEqualTo("line1\nline2\n")
    }

    @Test
    fun `골든과 다르면 diff를 담아 실패하고 실제 출력을 남긴다`() {
        writeGolden("wire/a.txt", "same\nold\n")

        assertThatThrownBy { golden(update = false).verify("wire/a.txt", "same\nnew\n") }
            .isInstanceOf(ContractViolationError::class.java)
            .hasMessageContaining("[API 계약 위반] wire/a.txt")
            .hasMessageContaining(ContractGolden.UPDATE_COMMAND)
            .hasMessageContaining("-old")
            .hasMessageContaining("+new")
        assertThat(actualRoot.resolve("wire/a.txt").readText()).isEqualTo("same\nnew\n")
    }

    @Test
    fun `diff는 앞 40줄까지만 싣는다`() {
        writeGolden("wire/long.txt", (1..100).joinToString("\n") { "old$it" })

        val message = golden(update = false).violationOf("wire/long.txt", (1..100).joinToString("\n") { "new$it" })

        assertThat(message!!.substringAfter("---\n").lines()).hasSize(40)
    }

    @Test
    fun `골든이 없으면 새 계약 대상으로 실패한다`() {
        assertThatThrownBy { golden(update = false).verify("wire/new.txt", "x") }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("[새 계약 대상] wire/new.txt")
    }

    @Test
    fun `갱신 모드면 골든을 쓰고 통과한다`() {
        golden(update = true).verify("wire/dir/new.txt", "a\r\nb")

        assertThat(goldenRoot.resolve("wire/dir/new.txt").readText()).isEqualTo("a\nb")
    }

    @Test
    fun `CI에서 갱신 모드를 켜면 쓰지 않고 바로 실패한다`() {
        val golden = ContractGolden(goldenRoot, actualRoot, ContractUpdateMode(enabled = true, ci = true))

        assertThatThrownBy { golden.verify("wire/ci.txt", "x") }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("CI")
        assertThat(goldenRoot.resolve("wire/ci.txt")).doesNotExist()
    }

    @Test
    fun `계약 루트 밖 경로는 거부한다`() {
        assertThatThrownBy { golden(update = false).verify("../escape.txt", "x") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `골든 목록은 점 파일을 빼고 읽고 골든 삭제는 갱신 모드에서만 한다`() {
        writeGolden("docs/x/a.adoc", "a")
        writeGolden("docs/x/b.adoc", "b")
        writeGolden("docs/x/.DS_Store", "os")

        assertThat(golden(update = false).goldenFiles("docs/x")).containsExactlyInAnyOrder("a.adoc", "b.adoc")
        assertThatThrownBy { golden(update = false).removeGolden("docs/x/a.adoc") }
            .isInstanceOf(IllegalStateException::class.java)
        golden(update = true).removeGolden("docs/x/a.adoc")
        assertThat(golden(update = false).goldenFiles("docs/x")).containsExactly("b.adoc")
    }

    @Test
    fun `갱신 모드에서 쓰거나 지운 골든 경로를 골든 루트마다 하나로 모아 기록한다`() {
        writeGolden("docs/x/old.adoc", "old")
        golden(update = false).verify("docs/x/old.adoc", "old")
        assertThat(golden(update = false).touchedPaths()).isEmpty()

        golden(update = true).verify("wire/./y/new.txt", "new")
        golden(update = true).removeGolden("docs/x/old.adoc")
        golden(update = false).markTouched("fixtures/a.sql")

        assertThat(golden(update = false).touchedPaths())
            .containsExactlyInAnyOrder("wire/y/new.txt", "docs/x/old.adoc", "fixtures/a.sql")
        assertThat(ContractGolden(tempDir.resolve("other"), actualRoot).touchedPaths()).isEmpty()
    }

    private fun golden(update: Boolean) =
        ContractGolden(goldenRoot, actualRoot, ContractUpdateMode(enabled = update, ci = false))

    private fun writeGolden(relativePath: String, text: String) {
        val path = goldenRoot.resolve(relativePath)
        Files.createDirectories(path.parent)
        path.writeText(text)
    }
}
