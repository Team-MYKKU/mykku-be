package com.example.mykku.contract.dump

import org.springframework.core.annotation.AnnotationUtils

object AttributeValues {

    private val OMITTED_WHEN_EMPTY = setOf("groups", "payload")

    fun annotation(annotation: Annotation): String =
        attributes(annotation.annotationClass.simpleName.orEmpty(), AnnotationUtils.getAnnotationAttributes(annotation))

    fun attributes(name: String, attributes: Map<String, Any?>): String {
        val rendered = attributes.toSortedMap()
            .filterNot { (key, value) -> key in OMITTED_WHEN_EMPTY && value is Array<*> && value.isEmpty() }
            .map { (key, value) -> "$key=${value(value)}" }
        return "@$name(${rendered.joinToString(", ")})"
    }

    fun value(value: Any?): String = when (value) {
        null -> "null"
        is String -> quote(value)
        is Array<*> -> value.joinToString(", ", "[", "]") { value(it) }
        is Set<*> -> value.map { value(it) }.sorted().joinToString(", ", "[", "]")
        is Collection<*> -> value.joinToString(", ", "[", "]") { value(it) }
        is Map<*, *> -> mapValue(value)
        is Class<*> -> value.simpleName
        is Enum<*> -> value.name
        is Annotation -> annotation(value)
        else -> value.toString()
    }

    private fun mapValue(map: Map<*, *>): String =
        map.entries.map { "${value(it.key)}=${value(it.value)}" }.sorted().joinToString(", ", "{", "}")

    private fun quote(text: String): String =
        "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
}
