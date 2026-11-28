package com.example.mykku.contract.snippet

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("스니펫 식별자 소유: 식별자 하나는 문서 테스트 클래스 하나에서만 만든다")
class SnippetIdentifierOwnershipTest {

    private val sites = DocumentCallSites()

    @Test
    fun `같은 document 식별자를 두 문서 테스트 파일에서 만들지 않는다`() {
        assertThat(sites.ownersByIdentifier().filterValues { it.size > 1 }).`as`(SHARED_GUIDE).isEmpty()
    }

    @Test
    fun `문서 테스트 파일은 문서 클래스 하나만 두고 리터럴로 document를 부르며 다른 문서 테스트를 상속하지 않는다`() {
        assertThat(sites.filesWithSeveralDocumentClasses()).`as`(SHARED_GUIDE).isEmpty()
        assertThat(sites.filesWithNonLiteralCalls()).`as`(LITERAL_GUIDE).isEmpty()
        assertThat(sites.classesExtendingDocumentTests()).`as`(INHERITANCE_GUIDE).isEmpty()
    }

    @Test
    fun `문서 테스트에서 찾은 식별자는 adoc이 참조하는 스니펫과 같다`() {
        val scanned = sites.ownersByIdentifier().keys
        val references = AdocSnippetReferences().identifiers()

        assertThat(references - scanned).`as`(LITERAL_GUIDE).isEmpty()
        assertThat(scanned - references).`as`("adoc이 참조하지 않는 document 식별자").isEmpty()
    }

    private companion object {
        const val SHARED_GUIDE = "테스트는 fork(JVM) 여러 개에 클래스 단위로 나뉘어 돈다. 두 클래스가 같은 식별자를 쓰면 " +
            "두 fork가 build/generated-snippets/<식별자>를 동시에 지우고 쓰고 비교한다. 식별자를 나눈다"
        const val LITERAL_GUIDE = "document()는 BaseDocumentTest를 상속한 문서 테스트에서 리터럴 접두와 상태로 부른다. " +
            "변수로 만든 식별자는 식별자 중복 검사가 보지 못한다"
        const val INHERITANCE_GUIDE = "문서 테스트를 상속하면 하위 클래스마다 같은 식별자를 만들고, 하위 클래스는 서로 다른 fork에서 돈다"
    }
}
