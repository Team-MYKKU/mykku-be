package com.example.mykku.contract.snippet

import com.example.mykku.contract.support.ContractGolden
import com.example.mykku.contract.support.ContractJson
import com.example.mykku.contract.support.JsonRules
import java.nio.file.Files
import java.nio.file.Path

class SnippetGolden(
    private val snippetRoot: Path = Path.of("build/generated-snippets"),
    private val golden: ContractGolden = ContractGolden.DEFAULT
) {

    fun verify(identifier: String) {
        val violations = violationsOf(identifier)
        if (violations.isNotEmpty()) {
            throw AssertionError("[스니펫 $identifier]\n" + violations.joinToString("\n\n"))
        }
    }

    fun violationsOf(identifier: String): List<String> {
        val generated = generatedFiles(identifier)
        val wire = generated.filterKeys { it in WIRE_FILES }
            .mapValues { (name, text) -> normalizedWire(identifier, name, text) }
        val docs = generated.filterKeys { isDocsFile(it) }
            .mapValues { (_, text) -> SnippetNormalizer.tableStructure(text) }
        return compareGrade("$WIRE_ROOT/$identifier", wire) + compareGrade("$DOCS_ROOT/$identifier", docs)
    }

    private fun generatedFiles(identifier: String): Map<String, String> {
        val dir = snippetRoot.resolve(identifier)
        if (!Files.isDirectory(dir)) {
            throw AssertionError("[스니펫 $identifier] document()로 기록했지만 스니펫이 생성되지 않았습니다: $dir")
        }
        return Files.list(dir).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(ADOC) }
                .toList()
                .associate { it.fileName.toString() to ContractJson.decodeUtf8(Files.readAllBytes(it)) }
        }
    }

    private fun normalizedWire(identifier: String, name: String, text: String): String = when (name) {
        HTTP_REQUEST -> SnippetNormalizer.httpRequest(text)
        else -> SnippetNormalizer.httpResponse(text, KEY_ORDER_NORMALIZED[identifier] ?: JsonRules())
    }

    private fun compareGrade(goldenDir: String, files: Map<String, String>): List<String> {
        val stale = golden.goldenFiles(goldenDir) - files.keys
        if (golden.updating) {
            stale.forEach { golden.removeGolden("$goldenDir/$it") }
        }
        val missing = stale.takeUnless { golden.updating }.orEmpty().sorted()
            .map { "[API 계약 위반] $goldenDir/$it 스니펫이 더 이상 생성되지 않습니다." }
        val changed = files.toSortedMap().mapNotNull { (name, text) -> golden.violationOf("$goldenDir/$name", text) }
        return missing + changed
    }

    private fun isDocsFile(name: String): Boolean =
        name !in WIRE_FILES && name !in TOOL_FILES && !name.endsWith(BODY_SUFFIX)

    companion object {
        const val WIRE_ROOT = "wire/snippets"
        const val DOCS_ROOT = "docs/snippets"
        private const val ADOC = ".adoc"
        private const val HTTP_REQUEST = "http-request.adoc"
        private const val HTTP_RESPONSE = "http-response.adoc"
        private const val BODY_SUFFIX = "-body.adoc"
        val WIRE_FILES = setOf(HTTP_REQUEST, HTTP_RESPONSE)
        private val TOOL_FILES = setOf("curl-request.adoc", "httpie-request.adoc")
        private val KEY_ORDER_NORMALIZED: Map<String, JsonRules> = listOf(
            "daily-message/list/200",
            "fan-note/list/200",
            "like/board-list/200",
            "notification/list/200",
            "notification/unread-list/200"
        ).associateWith { JsonRules.pageImpl() }
        val KEY_ORDER_NORMALIZED_IDENTIFIERS: Set<String> = KEY_ORDER_NORMALIZED.keys
    }
}
