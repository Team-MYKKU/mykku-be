package com.example.mykku.contract.snippet

import com.example.mykku.contract.support.ContractJson
import com.example.mykku.contract.support.JsonRules

object SnippetNormalizer {

    private const val TABLE_DELIMITER = "|==="
    private const val LISTING_DELIMITER = "----"
    private const val BOUNDARY_TOKEN = "boundary=\"<BOUNDARY>\""
    private val QUOTED_BOUNDARY = Regex("boundary=\"[^\"]+\"")

    fun httpRequest(text: String): String = text.replace(QUOTED_BOUNDARY, BOUNDARY_TOKEN)

    fun httpResponse(text: String, rules: JsonRules): String {
        if (rules.isEmpty) {
            return text
        }
        val lines = text.split("\n")
        val headerEnd = lines.indexOf("")
        val listingEnd = lines.lastIndexOf(LISTING_DELIMITER)
        require(headerEnd > 0 && listingEnd > headerEnd) { "http-response 스니펫 형식이 아닙니다" }
        val tree = ContractJson.parse(lines.subList(headerEnd + 1, listingEnd).joinToString("\n"))
        rules.applyTo(tree)
        val body = ContractJson.pretty(tree).split("\n")
        return (lines.subList(0, headerEnd + 1) + body + lines.subList(listingEnd, lines.size)).joinToString("\n")
    }

    fun tableStructure(text: String): String {
        val lines = text.split("\n")
        val start = lines.indexOf(TABLE_DELIMITER)
        val end = lines.lastIndexOf(TABLE_DELIMITER)
        require(start >= 0 && end > start + 1) { "표 스니펫 형식이 아닙니다" }
        val header = lines[start + 1]
        val columns = header.split("|").size - 1
        val rows = rowBlocks(lines.subList(start + 2, end)).flatMap { keptCells(it, columns) + "" }
        val kept = lines.subList(0, start + 1) + header.substringBeforeLast("|") + "" + rows
        return (kept + lines.subList(end, lines.size)).joinToString("\n")
    }

    private fun rowBlocks(body: List<String>): List<List<String>> {
        val blocks = mutableListOf<MutableList<String>>()
        body.forEach { line ->
            when {
                line.isEmpty() -> blocks.add(mutableListOf())
                blocks.isEmpty() -> blocks.add(mutableListOf(line))
                else -> blocks.last().add(line)
            }
        }
        return blocks.filter { it.isNotEmpty() }
    }

    private fun keptCells(block: List<String>, columns: Int): List<String> {
        require(columns >= 2 && block.size >= columns && block.first().startsWith("|")) {
            "표 스니펫 행 형식이 아닙니다(열 $columns 개): $block"
        }
        return block.take(columns - 1)
    }
}
