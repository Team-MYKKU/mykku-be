package com.example.mykku.contract.snippet

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

@DisplayName("document 호출 위치 수집")
class DocumentCallSitesTest {

    @TempDir
    lateinit var root: Path

    @Test
    fun `BaseDocumentTest를 상속한 파일에서만 리터럴 식별자와 규칙 위반을 찾는다`() {
        FIXTURES.forEach { (path, source) -> write(path, source) }

        val sites = DocumentCallSites(root)

        assertThat(sites.ownersByIdentifier()).isEqualTo(EXPECTED_OWNERS)
        assertThat(sites.filesWithNonLiteralCalls()).containsExactly("b/EventDocumentTest.kt")
        assertThat(sites.filesWithSeveralDocumentClasses()).containsExactly("b/FeedCopyDocumentTest.kt")
        assertThat(sites.classesExtendingDocumentTests()).containsExactly("FeedReplayTest : FeedDocumentTest")
    }

    private fun write(relativePath: String, content: String) {
        val file = root.resolve(relativePath)
        Files.createDirectories(file.parent)
        file.writeText(content)
    }

    private companion object {
        const val BASE = "BaseDocumentTest"
        const val FEED = "FeedDocumentTest"
        val FIXTURES = mapOf(
            "a/FeedDocumentTest.kt" to "class $FEED : $BASE() {\n" +
                "document(\"feed/create\", 200)\ndocument(\n  \"feed/create\", \"BOARD_NOT_FOUND\")\n}",
            "b/EventDocumentTest.kt" to "class EventDocumentTest : $BASE() {\n" +
                "document(\"event-winner/my-status\", \"not-winner\")\ndocument(id, code)\n}",
            "b/FeedCopyDocumentTest.kt" to "class FeedCopyDocumentTest : $BASE() {\n" +
                "document(\"feed/create\", 200)\n}\nclass ExtraDocumentTest : $BASE()",
            "c/BaseDocumentTest.kt" to "abstract class $BASE : SnippetIdentifierSource {\n" +
                "fun document(prefix: String, status: Int) = document(prefix, status.toString())\n}",
            "c/Helper.kt" to "object Helper {\ndocument(\"helper/ignored\", 200)\n}",
            "d/FeedReplayTest.kt" to "class FeedReplayTest : $FEED()"
        )
        val EXPECTED_OWNERS = mapOf(
            "feed/create/200" to sortedSetOf("a/FeedDocumentTest.kt", "b/FeedCopyDocumentTest.kt"),
            "feed/create/BOARD_NOT_FOUND" to sortedSetOf("a/FeedDocumentTest.kt"),
            "event-winner/my-status/not-winner" to sortedSetOf("b/EventDocumentTest.kt")
        )
    }
}
