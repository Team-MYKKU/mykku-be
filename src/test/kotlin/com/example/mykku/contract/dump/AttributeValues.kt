package com.example.mykku.contract.dump

import org.springframework.core.annotation.AnnotationUtils
import java.lang.reflect.Array as ReflectArray

object AttributeValues {

    private val OMITTED_WHEN_EMPTY = setOf("groups", "payload")

    fun annotation(annotation: Annotation): String =
        attributes(annotation.annotationClass.simpleName.orEmpty(), AnnotationUtils.getAnnotationAttributes(annotation))

    fun attributes(name: String, attributes: Map<String, Any?>): String {
        val rendered = attributes.toSortedMap()
            .filterNot { (key, value) -> key in OMITTED_WHEN_EMPTY && isEmptyArray(value) }
            .map { (key, value) -> "$key=${value(value)}" }
        return "@$name(${rendered.joinToString(", ")})"
    }

    fun value(value: Any?): String = when {
        value == null -> "null"
        value is String -> quote(value)
        value is Set<*> -> value.map { value(it) }.sorted().joinToString(", ", "[", "]")
        value is Collection<*> -> value.joinToString(", ", "[", "]") { value(it) }
        value is Map<*, *> -> mapValue(value)
        value.javaClass.isArray -> arrayValue(value)
        value is Class<*> -> value.simpleName
        value is Enum<*> -> value.name
        value is Annotation -> annotation(value)
        else -> value.toString()
    }

    private fun isEmptyArray(value: Any?): Boolean =
        value != null && value.javaClass.isArray && ReflectArray.getLength(value) == 0

    private fun arrayValue(array: Any): String =
        (0 until ReflectArray.getLength(array)).joinToString(", ", "[", "]") { value(ReflectArray.get(array, it)) }

    private fun mapValue(map: Map<*, *>): String =
        map.entries.map { "${value(it.key)}=${value(it.value)}" }.sorted().joinToString(", ", "{", "}")

    private fun quote(text: String): String =
        "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
}
