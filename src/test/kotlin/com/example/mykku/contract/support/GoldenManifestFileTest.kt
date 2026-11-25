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
    fun `손으로 고친 골든은 MANIFEST 해시와 달라진다`() {
        write("wire/a.txt", "a")
        val manifest = GoldenManifest(root)
        manifest.write(BASE)

        write("wire/a.txt", "edited")

        assertThat(manifest.read()!!.entries).isNotEqualTo(manifest.current())
        assertThat(GoldenManifest.isCommit(BASE)).isTrue()
        assertThat(GoldenManifest.isCommit("2339439")).isFalse()
    }

    @Test
    fun `갱신 모드가 끝나면 기록된 기준 커밋을 지키고 파일 해시를 다시 쓴다`() {
        write("wire/a.txt", "a")
        GoldenManifest(root).write(BASE)
        write("wire/a.txt", "regenerated")

        refresh(update = true, ci = false, resolvedBase = OTHER_BASE).close()

        assertThat(GoldenManifest(root).read()).isEqualTo(ManifestContent(BASE, GoldenManifest(root).current()))
    }

    @Test
    fun `MANIFEST가 없으면 기준 커밋을 새로 정하고 갱신 모드가 아니면 쓰지 않는다`() {
        write("wire/a.txt", "a")

        refresh(update = false, ci = false, resolvedBase = OTHER_BASE).close()
        assertThat(GoldenManifest(root).read()).isNull()
        refresh(update = true, ci = false, resolvedBase = OTHER_BASE).close()
        assertThat(GoldenManifest(root).read()!!.base).isEqualTo(OTHER_BASE)
    }

    @Test
    fun `CI에서 갱신 모드를 켜면 MANIFEST를 쓰지 않고 실패한다`() {
        write("wire/a.txt", "a")

        assertThatThrownBy { refresh(update = true, ci = true, resolvedBase = OTHER_BASE).close() }
            .isInstanceOf(AssertionError::class.java)
        assertThat(GoldenManifest(root).read()).isNull()
    }

    private fun refresh(update: Boolean, ci: Boolean, resolvedBase: String) =
        ManifestRefresh(GoldenManifest(root), ContractUpdateMode(enabled = update, ci = ci)) { resolvedBase }

    private fun write(relativePath: String, text: String) {
        val path = root.resolve(relativePath)
        Files.createDirectories(path.parent)
        path.writeText(text)
    }

    private companion object {
        const val BASE = "23394390385246c946de7570d8607f67ef483c89"
        const val OTHER_BASE = "a770829df84458db82dc2fe51828224ca6754387"
    }
}
