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

@DisplayName("골든 MANIFEST 파일")
class GoldenManifestFileTest {

    @TempDir
    lateinit var root: Path

    @Test
    fun `기준 커밋과 파일별 sha256을 경로 순으로 쓰고 다시 읽는다`() {
        write("wire/b.txt", "b")
        write("docs/a.adoc", "a")
        write(".DS_Store", "ignored")
        val manifest = GoldenManifest(root)

        manifest.write(BASE)

        assertThat(root.resolve(GoldenManifest.FILE_NAME).readText()).isEqualTo(
            "base $BASE\n" +
                "ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb  docs/a.adoc\n" +
                "3e23e8160039594a33894f6564e1b1348bbd7a0088d42c4acb73eeaed59c009d  wire/b.txt\n"
        )
        assertThat(manifest.read()).isEqualTo(ManifestContent(BASE, manifest.current()))
    }

    @Test
    fun `기준 커밋은 40자리 소문자 16진수 해시만 받는다`() {
        assertThat(GoldenManifest.isCommit(BASE)).isTrue()
        assertThat(GoldenManifest.isCommit("2339439")).isFalse()
        assertThat(GoldenManifest.isCommit(BASE.uppercase())).isFalse()
    }

    @Test
    fun `갱신 모드는 이번에 쓴 골든과 새 파일만 해시를 바꾸고 기준 커밋을 지킨다`() {
        listOf("wire/regenerated.txt", "wire/hand-edited.txt", "wire/removed.txt").forEach { write(it, "v1") }
        GoldenManifest(root).write(BASE)
        write("wire/regenerated.txt", "v2")
        write("wire/hand-edited.txt", "v2")
        write("fixtures/new.sql", "SELECT 1;")
        Files.delete(root.resolve("wire/removed.txt"))

        refresh(update = true, ci = false, touched = setOf("wire/regenerated.txt")).close()

        val recorded = GoldenManifest(root).read()!!
        val current = GoldenManifest(root).current()
        assertThat(recorded.base).isEqualTo(BASE)
        assertThat(recorded.entries.keys).containsExactlyElementsOf(current.keys)
        assertThat(recorded.entries.filterValues { it !in current.values }.keys).containsExactly(HAND_EDITED)
    }

    @Test
    fun `MANIFEST가 없으면 기준 커밋을 새로 정하고 갱신 모드가 아니면 쓰지 않는다`() {
        write("wire/a.txt", "a")

        refresh(update = false, ci = false, touched = emptySet()).close()
        assertThat(GoldenManifest(root).read()).isNull()
        refresh(update = true, ci = false, touched = emptySet()).close()
        assertThat(GoldenManifest(root).read()).isEqualTo(ManifestContent(OTHER_BASE, GoldenManifest(root).current()))
    }

    @Test
    fun `CI에서 갱신 모드를 켜면 MANIFEST를 쓰지 않고 실패한다`() {
        write("wire/a.txt", "a")

        assertThatThrownBy { refresh(update = true, ci = true, touched = emptySet()).close() }
            .isInstanceOf(ContractViolationError::class.java)
        assertThat(GoldenManifest(root).read()).isNull()
    }

    private fun refresh(update: Boolean, ci: Boolean, touched: Set<String>): ManifestRefresh {
        val mode = ContractUpdateMode(enabled = update, ci = ci)
        return ManifestRefresh(GoldenManifest(root), mode, { touched }, { OTHER_BASE })
    }

    private fun write(relativePath: String, text: String) {
        val path = root.resolve(relativePath)
        Files.createDirectories(path.parent)
        path.writeText(text)
    }

    private companion object {
        const val BASE = "23394390385246c946de7570d8607f67ef483c89"
        const val OTHER_BASE = "a770829df84458db82dc2fe51828224ca6754387"
        const val HAND_EDITED = "wire/hand-edited.txt"
    }
}
