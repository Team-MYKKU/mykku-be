package com.example.mykku.contract.snippet

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

@DisplayName("adoc 스니펫 참조 수집")
class AdocSnippetReferencesTest {

    @TempDir
    lateinit var root: Path

    @Test
    fun `index에서 include를 따라가며 주석 밖의 operation과 스니펫 include만 모은다`() {
        write("index.adoc", "include::{snippets}/common/A/http-response.adoc[]", "include::api/feed.adoc[]")
        write(
            "api/feed.adoc",
            "operation::feed/list/200[snippets='http-request']",
            "// operation::feed/commented/200[]",
            "//////",
            "include::{snippets}/feed/blocked/404/http-response.adoc[]",
            "//////",
            "include::../index.adoc[]"
        )

        assertThat(AdocSnippetReferences(root.resolve("index.adoc")).identifiers())
            .containsExactly("common/A", "feed/list/200")
    }

    @Test
    fun `include 대상 adoc이 없으면 실패한다`() {
        write("index.adoc", "include::api/missing.adoc[]")

        assertThatThrownBy { AdocSnippetReferences(root.resolve("index.adoc")).identifiers() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("missing.adoc")
    }

    private fun write(relativePath: String, vararg lines: String) {
        val path = root.resolve(relativePath)
        Files.createDirectories(path.parent)
        path.writeText(lines.joinToString("\n", postfix = "\n"))
    }
}
