package com.example.mykku.contract.support

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode

class JsonMask(
    val path: String,
    private val replacement: (JsonNode) -> String
) {
    fun replace(slot: JsonSlot) {
        slot.replace(TextNode(replacement(slot.value)))
    }
}

class JsonRules(
    private val masks: List<JsonMask> = emptyList(),
    private val sortedKeyPaths: List<String> = emptyList()
) {

    val isEmpty: Boolean
        get() = masks.isEmpty() && sortedKeyPaths.isEmpty()

    fun applyTo(tree: JsonNode) {
        masks.forEach { applyMask(tree, it) }
        sortedKeyPaths.forEach { sortKeys(tree, it) }
    }

    operator fun plus(other: JsonRules): JsonRules =
        JsonRules(masks + other.masks, sortedKeyPaths + other.sortedKeyPaths)

    private fun applyMask(tree: JsonNode, mask: JsonMask) {
        val slots = JsonPath.slots(tree, mask.path)
        if (slots.isEmpty()) {
            throw JsonPath.missing(mask.path)
        }
        slots.forEach { mask.replace(it) }
    }

    private fun sortKeys(tree: JsonNode, path: String) {
        val targets = JsonPath.select(tree, path)
        if (targets.isEmpty()) {
            throw JsonPath.missing(path)
        }
        targets.forEach { sortObject(it, path) }
    }

    private fun sortObject(node: JsonNode, path: String) {
        if (node !is ObjectNode) {
            throw ContractViolationError("[API 계약 위반] 키 순서 정규화 경로 $path 가 객체가 아닙니다: ${node.nodeType}")
        }
        val sorted = node.properties().map { it.key to it.value }.sortedBy { it.first }
        node.removeAll()
        sorted.forEach { (key, value) -> node.replace(key, value) }
    }

    companion object {
        fun pageImpl(base: String = "/data"): JsonRules = JsonRules(
            sortedKeyPaths = listOf(base, "$base/pageable", "$base/pageable/sort", "$base/sort")
        )
    }
}
