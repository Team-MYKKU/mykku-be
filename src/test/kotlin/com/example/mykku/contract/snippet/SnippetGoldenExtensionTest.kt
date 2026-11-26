package com.example.mykku.contract.snippet

import com.example.mykku.contract.support.ContractGolden
import com.example.mykku.contract.support.ContractUpdateMode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestInstances
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.nio.file.Path
import java.util.Optional

@DisplayName("스니펫 골든 확장")
class SnippetGoldenExtensionTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `테스트가 실패했으면 기록만 비우고 비교하지 않는다`() {
        val source = RecordingSource(mutableListOf("x/absent/200"))

        extension().afterTestExecution(context(source, failure = AssertionError("본문 실패")))

        assertThat(source.drainSnippetIdentifiers()).isEmpty()
    }

    @Test
    fun `테스트가 성공했으면 바깥 인스턴스에 기록된 식별자를 골든과 비교한다`() {
        val source = RecordingSource(mutableListOf("x/absent/200"))

        assertThatThrownBy { extension().afterTestExecution(context(source, failure = null)) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("x/absent/200")
        assertThat(source.drainSnippetIdentifiers()).isEmpty()
    }

    private fun extension(): SnippetGoldenExtension {
        val mode = ContractUpdateMode(enabled = false, ci = false)
        val golden = ContractGolden(tempDir.resolve("golden"), tempDir.resolve("actual"), mode)
        return SnippetGoldenExtension(SnippetGolden(tempDir.resolve("snippets"), golden))
    }

    private fun context(source: SnippetIdentifierSource, failure: Throwable?): ExtensionContext {
        val instances = mock<TestInstances> { on { allInstances } doReturn listOf(source, Any()) }
        return mock {
            on { requiredTestInstances } doReturn instances
            on { executionException } doReturn Optional.ofNullable(failure)
        }
    }

    private class RecordingSource(private val identifiers: MutableList<String>) : SnippetIdentifierSource {
        override fun drainSnippetIdentifiers(): List<String> = identifiers.toList().also { identifiers.clear() }
    }
}
