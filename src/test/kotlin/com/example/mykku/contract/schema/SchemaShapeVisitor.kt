package com.example.mykku.contract.schema

import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.introspect.AnnotatedMember
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonAnyFormatVisitor
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonBooleanFormatVisitor
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitable
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonIntegerFormatVisitor
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonMapFormatVisitor
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonNullFormatVisitor
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonNumberFormatVisitor
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonValueFormat
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter

data class SchemaChild(
    val name: String,
    val type: JavaType?,
    val visitable: JsonFormatVisitable?,
    val property: BeanProperty?
) {
    val member: AnnotatedMember?
        get() = property?.member

    companion object {
        fun of(property: BeanProperty): SchemaChild =
            SchemaChild(property.name, property.type, (property as? BeanPropertyWriter)?.serializer, property)
    }
}

class SchemaShape {
    var jsonType: String = "unknown"
    val details: MutableList<String> = mutableListOf()
    val properties: MutableList<SchemaChild> = mutableListOf()
    var element: SchemaChild? = null
}

class SchemaShapeVisitor(
    private val shape: SchemaShape,
    provider: SerializerProvider
) : JsonFormatVisitorWrapper.Base(provider) {

    override fun expectObjectFormat(type: JavaType?): JsonObjectFormatVisitor =
        mark("object", PropertyCollector(shape, provider))

    override fun expectArrayFormat(type: JavaType?): JsonArrayFormatVisitor =
        mark("array", ElementCollector(shape, provider))

    override fun expectMapFormat(type: JavaType?): JsonMapFormatVisitor =
        mark("map", MapValueCollector(shape, provider))

    override fun expectStringFormat(type: JavaType?): JsonStringFormatVisitor = mark("string", StringDetails(shape))

    override fun expectNumberFormat(type: JavaType?): JsonNumberFormatVisitor? = mark("number", null)

    override fun expectIntegerFormat(type: JavaType?): JsonIntegerFormatVisitor? = mark("integer", null)

    override fun expectBooleanFormat(type: JavaType?): JsonBooleanFormatVisitor? = mark("boolean", null)

    override fun expectNullFormat(type: JavaType?): JsonNullFormatVisitor? = mark("null", null)

    override fun expectAnyFormat(type: JavaType?): JsonAnyFormatVisitor? = mark("any", null)

    private fun <T> mark(jsonType: String, visitor: T): T {
        shape.jsonType = jsonType
        return visitor
    }
}

private class PropertyCollector(
    private val shape: SchemaShape,
    provider: SerializerProvider
) : JsonObjectFormatVisitor.Base(provider) {

    override fun property(writer: BeanProperty) {
        shape.properties += SchemaChild.of(writer)
    }

    override fun optionalProperty(writer: BeanProperty) {
        shape.properties += SchemaChild.of(writer)
    }

    override fun property(name: String, handler: JsonFormatVisitable?, propertyTypeHint: JavaType?) {
        shape.properties += SchemaChild(name, propertyTypeHint, handler, null)
    }

    override fun optionalProperty(name: String, handler: JsonFormatVisitable?, propertyTypeHint: JavaType?) {
        shape.properties += SchemaChild(name, propertyTypeHint, handler, null)
    }
}

private class ElementCollector(
    private val shape: SchemaShape,
    provider: SerializerProvider
) : JsonArrayFormatVisitor.Base(provider) {

    override fun itemsFormat(handler: JsonFormatVisitable?, elementType: JavaType?) {
        shape.element = SchemaChild(ELEMENT, elementType, handler, null)
    }
}

private class MapValueCollector(
    private val shape: SchemaShape,
    provider: SerializerProvider
) : JsonMapFormatVisitor.Base(provider) {

    override fun valueFormat(handler: JsonFormatVisitable?, valueType: JavaType?) {
        shape.element = SchemaChild(ELEMENT, valueType, handler, null)
    }
}

private class StringDetails(private val shape: SchemaShape) : JsonStringFormatVisitor.Base() {

    override fun format(format: JsonValueFormat?) {
        shape.details += "format=$format"
    }

    override fun enumTypes(enums: MutableSet<String>?) {
        shape.details += "enum=[${enums.orEmpty().joinToString(",")}]"
    }
}

private const val ELEMENT = "element"
