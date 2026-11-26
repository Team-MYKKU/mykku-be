package com.example.mykku.contract.snippet

import com.example.mykku.contract.support.ContractGolden
import com.example.mykku.contract.support.ContractUpdateMode
import com.example.mykku.contract.support.JsonRules
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

@DisplayName("스니펫 골든 정규화와 비교")
class SnippetGoldenTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `http-request는 RestAssured가 만든 따옴표 boundary만 가린다`() {
        val header = "Content-Type: multipart/form-data; boundary=\"Xy_-9z\"; boundary=$REST_DOCS_BOUNDARY"

        assertThat(SnippetNormalizer.httpRequest(header))
            .isEqualTo("Content-Type: multipart/form-data; boundary=\"<BOUNDARY>\"; boundary=$REST_DOCS_BOUNDARY")
    }

    @Test
    fun `http-response는 선언한 PageImpl 객체의 키만 이름순으로 다시 놓고 나머지 바이트는 그대로 둔다`() {
        val snippet = httpResponse("{\n  \"data\" : {\n    \"z\" : [ 1, 2 ],\n    \"a\" : { }\n  }\n}")

        assertThat(SnippetNormalizer.httpResponse(snippet, JsonRules())).isEqualTo(snippet)
        assertThat(SnippetNormalizer.httpResponse(snippet, JsonRules(sortedKeyPaths = listOf("/data"))))
            .isEqualTo(httpResponse("{\n  \"data\" : {\n    \"a\" : { },\n    \"z\" : [ 1, 2 ]\n  }\n}"))
    }

    @Test
    fun `표 스니펫은 설명 열을 빼고 이름·타입·필수만 남긴다`() {
        val table = listOf(
            ".+/api/v1/feeds/{feedId}+", "|===", "|필드|타입|필수|설명", "",
            "|`+a+`", "|`+String+`", "|O", "|설명 1", "",
            "|`+b+`", "|`+Number+`", "|X", "|여러 줄", "설명", "",
            "|===", ""
        ).joinToString("\n")

        assertThat(SnippetNormalizer.tableStructure(table)).isEqualTo(
            listOf(
                ".+/api/v1/feeds/{feedId}+", "|===", "|필드|타입|필수", "",
                "|`+a+`", "|`+String+`", "|O", "",
                "|`+b+`", "|`+Number+`", "|X", "",
                "|===", ""
            ).joinToString("\n")
        )
    }

    @Test
    fun `표 행이 셀로 시작하지 않으면 실패한다`() {
        val broken = listOf("|===", "|헤더|필수|설명", "", "|`+a+`", "|O", "|설명", "", "꼬리", "", "|===").joinToString("\n")

        assertThatThrownBy { SnippetNormalizer.tableStructure(broken) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `갱신 모드는 wire·docs 골든을 쓰고 도구·본문 스니펫은 비교하지 않는다`() {
        writeSnippets("x/list/200", "body-v1")

        snippetGolden(update = true).verify("x/list/200")

        val golden = tempDir.resolve("golden")
        assertThat(golden.resolve("wire/snippets/x/list/200/http-request.adoc").readText()).contains("<BOUNDARY>")
        assertThat(golden.resolve("docs/snippets/x/list/200/query-parameters.adoc").readText()).doesNotContain("설명")
        assertThat(golden.resolve("docs/snippets/x/list/200/curl-request.adoc")).doesNotExist()
        assertThat(golden.resolve("docs/snippets/x/list/200/response-body.adoc")).doesNotExist()
    }

    @Test
    fun `비교 모드는 바뀐 파일·사라진 파일·골든 없는 식별자를 모두 잡는다`() {
        writeSnippets("x/list/200", "body-v1")
        snippetGolden(update = true).verify("x/list/200")
        writeSnippets("x/list/200", "body-v2")
        Files.delete(tempDir.resolve("snippets/x/list/200/query-parameters.adoc"))
        writeSnippets("x/new/200", "body-v1")

        assertThat(snippetGolden(update = false).violationsOf("x/list/200")).hasSize(2)
        assertThat(snippetGolden(update = false).violationsOf("x/new/200")).allMatch { it.contains("[새 계약 대상]") }
        assertThatThrownBy { snippetGolden(update = false).verify("x/absent/200") }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("스니펫이 생성되지 않았습니다")
    }

    @Test
    fun `CRLF로 쓴 스니펫도 LF로 바꾼 뒤 정규화하고 골든 폴더의 점 파일은 무시한다`() {
        writeSnippets("x/list/200", "body-v1")
        snippetGolden(update = true).verify("x/list/200")
        val dir = tempDir.resolve("snippets/x/list/200")
        Files.list(dir).use { paths -> paths.toList() }.forEach { it.writeText(it.readText().replace("\n", "\r\n")) }
        tempDir.resolve("golden/docs/snippets/x/list/200/.DS_Store").writeText("os")

        assertThat(snippetGolden(update = false).violationsOf("x/list/200")).isEmpty()
    }

    @Test
    fun `document()가 식별자를 기록할 때 지난 실행의 스니펫 파일을 지운다`() {
        writeSnippets("x/list/200", "body-v1")

        snippetGolden(update = false).discardGenerated("x/list/200")

        assertThat(Files.list(tempDir.resolve("snippets/x/list/200")).use { it.toList() }).isEmpty()
        assertThatThrownBy { snippetGolden(update = false).discardGenerated("../escape") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun snippetGolden(update: Boolean): SnippetGolden {
        val mode = ContractUpdateMode(enabled = update, ci = false)
        val golden = ContractGolden(tempDir.resolve("golden"), tempDir.resolve("actual"), mode)
        return SnippetGolden(tempDir.resolve("snippets"), golden)
    }

    private fun writeSnippets(identifier: String, body: String) {
        val dir = tempDir.resolve("snippets/$identifier")
        Files.createDirectories(dir)
        dir.resolve("http-request.adoc").writeText("POST /x\nContent-Type: multipart/form-data; boundary=\"r4nd0m\"\n")
        dir.resolve("http-response.adoc").writeText(httpResponse("{\n  \"v\" : \"$body\"\n}"))
        dir.resolve("query-parameters.adoc").writeText("|===\n|파라미터|필수|설명\n\n|`+page+`\n|X\n|설명\n\n|===\n")
        dir.resolve("curl-request.adoc").writeText("curl $body")
        dir.resolve("response-body.adoc").writeText(body)
    }

    private fun httpResponse(body: String): String =
        "[source,http,options=\"nowrap\"]\n----\nHTTP/1.1 200 OK\nContent-Type: application/json\n\n$body\n----"

    private companion object {
        const val REST_DOCS_BOUNDARY = "6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm"
    }
}
