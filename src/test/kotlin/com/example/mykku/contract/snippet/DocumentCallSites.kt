package com.example.mykku.contract.snippet

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path

class DocumentCallSites(private val sourceRoot: Path = Path.of("src/test/kotlin")) {

    fun ownersByIdentifier(): Map<String, Set<String>> = documentTests()
        .flatMap { (file, source) -> literalIdentifiers(source).map { it to file } }
        .groupBy({ it.first }, { it.second })
        .mapValues { it.value.toSortedSet() }

    fun filesWithNonLiteralCalls(): Set<String> = documentTests()
        .filter { (_, source) -> ANY_CALL.findAll(source).count() != LITERAL_CALL.findAll(source).count() }
        .map { it.first }
        .toSortedSet()

    fun filesWithSeveralDocumentClasses(): Set<String> = documentTests()
        .filter { (_, source) -> DOCUMENT_CLASS.findAll(source).count() > 1 }
        .map { it.first }
        .toSortedSet()

    fun classesExtendingDocumentTests(): Set<String> = sources()
        .flatMap { (_, source) -> DOCUMENT_SUBCLASS.findAll(source).map { subclassOf(it) } }
        .toSortedSet()

    private fun documentTests(): List<Pair<String, String>> =
        sources().filter { (_, source) -> DOCUMENT_CLASS.containsMatchIn(source) }

    private fun sources(): List<Pair<String, String>> = Files.walk(sourceRoot).use { paths ->
        paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(KOTLIN) }.toList()
    }.map { sourceRoot.relativize(it).joinToString("/") to Files.readString(it, UTF_8) }

    private fun subclassOf(match: MatchResult): String = "${match.groupValues[1]} : ${match.groupValues[2]}"

    private fun literalIdentifiers(source: String): List<String> = LITERAL_CALL.findAll(source)
        .map { "${it.groupValues[1]}/${it.groupValues[2]}" }
        .toList()

    private companion object {
        const val KOTLIN = ".kt"
        val ANY_CALL = Regex("\\bdocument\\(")
        val LITERAL_CALL = Regex("\\bdocument\\(\\s*\"([^\"]+)\"\\s*,\\s*\"?([A-Za-z0-9_-]+)\"?\\s*\\)")
        val DOCUMENT_CLASS = Regex("\\bclass\\s+\\w+\\s*:\\s*BaseDocumentTest\\(\\)")
        val DOCUMENT_SUBCLASS = Regex("\\bclass\\s+(\\w+)[^{:]*:\\s*(?!BaseDocumentTest\\()(\\w+DocumentTest)\\(")
    }
}
