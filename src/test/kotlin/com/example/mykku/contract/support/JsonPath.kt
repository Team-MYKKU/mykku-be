package com.example.mykku.contract.support

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

class JsonSlot(private val container: JsonNode, private val key: String) {

    val value: JsonNode
        get() = if (container is ArrayNode) container.get(key.toInt()) else container.get(key)

    fun replace(newValue: JsonNode) {
        if (container is ArrayNode) {
            container.set(key.toInt(), newValue)
            return
        }
        (container as ObjectNode).replace(key, newValue)
    }
}

object JsonPath {

    private const val WILDCARD = "*"

    fun select(root: JsonNode, path: String): List<JsonNode> =
        segments(path).fold(listOf(root)) { nodes, segment -> nodes.flatMap { children(it, segment, path) } }

    fun slots(root: JsonNode, path: String): List<JsonSlot> {
        val segments = segments(path)
        require(segments.isNotEmpty()) { "JSON 루트 전체는 슬롯으로 고를 수 없습니다: '$path'" }
        val parentPath = segments.dropLast(1).joinToString("") { "/$it" }
        return select(root, parentPath).flatMap { keys(it, segments.last(), path).map { key -> JsonSlot(it, key) } }
    }

    fun missing(path: String): AssertionError =
        AssertionError("[API 계약 위반] 선언한 JSON 경로 $path 가 응답에 없습니다.")

    private fun segments(path: String): List<String> {
        require(path.isEmpty() || (path.startsWith("/") && !path.endsWith("/"))) {
            "JSON 경로는 '' 또는 /a/b 형식이어야 합니다: '$path'"
        }
        return if (path.isEmpty()) emptyList() else path.substring(1).split("/")
    }

    private fun children(node: JsonNode, segment: String, path: String): List<JsonNode> =
        keys(node, segment, path).map { if (node.isArray) node.get(it.toInt()) else node.get(it) }

    private fun keys(node: JsonNode, segment: String, path: String): List<String> = when {
        segment == WILDCARD && node.isArray -> node.indices().map { it.toString() }
        segment == WILDCARD && node.isObject -> node.fieldNames().asSequence().toList()
        node.isArray && segment.toIntOrNull() in node.indices() -> listOf(segment)
        node.isObject && node.has(segment) -> listOf(segment)
        else -> throw missing(path)
    }

    private fun JsonNode.indices(): IntRange = 0 until size()
}
