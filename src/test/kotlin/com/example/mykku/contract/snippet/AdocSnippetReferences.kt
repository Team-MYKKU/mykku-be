package com.example.mykku.contract.snippet

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path

class AdocSnippetReferences(private val entryPoint: Path = Path.of("src/docs/asciidoc/index.adoc")) {

    fun identifiers(): Set<String> = collect(entryPoint.normalize(), mutableSetOf()).toSortedSet()

    private fun collect(file: Path, visited: MutableSet<Path>): Set<String> {
        if (!visited.add(file)) {
            return emptySet()
        }
        require(Files.isRegularFile(file)) { "adoc include 대상이 없습니다: $file" }
        val lines = uncommentedLines(Files.readAllLines(file, UTF_8))
        val own = lines.mapNotNull { referenceOf(it) }.toSet()
        val included = lines.mapNotNull { DOCUMENT_INCLUDE.find(it)?.groupValues?.get(1) }
            .flatMap { collect(file.resolveSibling(it).normalize(), visited) }
        return own + included
    }

    private fun referenceOf(line: String): String? =
        OPERATION.find(line)?.groupValues?.get(1) ?: SNIPPET_INCLUDE.find(line)?.groupValues?.get(1)

    private fun uncommentedLines(lines: List<String>): List<String> {
        var inBlockComment = false
        return lines.filter { line ->
            if (line.trim() == BLOCK_COMMENT) {
                inBlockComment = !inBlockComment
            }
            !inBlockComment && line.trim() != BLOCK_COMMENT && !line.trimStart().startsWith(LINE_COMMENT)
        }
    }

    private companion object {
        const val BLOCK_COMMENT = "////"
        const val LINE_COMMENT = "//"
        val OPERATION = Regex("^operation::([^\\[]+)\\[")
        val SNIPPET_INCLUDE = Regex("^include::\\{snippets}/(.+)/[^/]+\\.adoc\\[")
        val DOCUMENT_INCLUDE = Regex("^include::([^{\\[][^\\[]*\\.adoc)\\[")
    }
}
