package com.example.mykku.contract.snippet

import com.example.mykku.contract.support.ContractUpdateMode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

@DisplayName("스니펫 골든 완전성")
class SnippetGoldenCoverageTest {

    private val contractRoot = Path.of("src/test/resources/contract")

    @BeforeEach
    fun skipWhileUpdating() {
        assumeFalse(ContractUpdateMode.fromEnvironment().enabled, "갱신 모드에서는 골든을 쓰는 중이라 건너뛴다")
    }

    @Test
    fun `wire 스니펫 골든 식별자 집합은 adoc이 참조하는 스니펫 집합과 같다`() {
        assertThat(identifiers(SnippetGolden.WIRE_ROOT))
            .containsExactlyInAnyOrderElementsOf(AdocSnippetReferences().identifiers())
    }

    @Test
    fun `wire 스니펫 골든마다 http-request와 http-response만 있다`() {
        val wireRoot = contractRoot.resolve(SnippetGolden.WIRE_ROOT)

        identifiers(SnippetGolden.WIRE_ROOT).forEach { identifier ->
            assertThat(fileNames(wireRoot.resolve(identifier)).toList())
                .`as`(identifier)
                .containsExactlyInAnyOrderElementsOf(SnippetGolden.WIRE_FILES)
        }
    }

    @Test
    fun `docs 스니펫 골든은 wire 골든이 있는 식별자에만 있다`() {
        assertThat(identifiers(SnippetGolden.WIRE_ROOT)).containsAll(identifiers(SnippetGolden.DOCS_ROOT))
    }

    @Test
    fun `키 순서를 정규화하는 식별자는 모두 wire 골든에 있다`() {
        assertThat(identifiers(SnippetGolden.WIRE_ROOT)).containsAll(SnippetGolden.KEY_ORDER_NORMALIZED_IDENTIFIERS)
    }

    private fun identifiers(grade: String): Set<String> {
        val root = contractRoot.resolve(grade)
        return Files.walk(root).use { paths ->
            paths.filter { Files.isRegularFile(it) }
                .map { root.relativize(it.parent).joinToString("/") }
                .toList()
                .toSortedSet()
        }
    }

    private fun fileNames(dir: Path): Set<String> = Files.list(dir).use { paths ->
        paths.map { it.fileName.toString() }.toList().toSet()
    }
}
