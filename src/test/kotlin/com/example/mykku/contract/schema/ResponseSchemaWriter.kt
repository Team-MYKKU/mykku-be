package com.example.mykku.contract.schema

import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitable
import com.fasterxml.jackson.databind.type.TypeFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter

data class SchemaTarget(
    val path: String,
    val type: JavaType,
    val declared: KType?,
    val visitable: JsonFormatVisitable? = null,
    val property: BeanProperty? = null
)

class ResponseSchemaWriter(private val mapper: ObjectMapper) {

    private val provider: SerializerProvider = mapper.serializerProviderInstance

    fun lines(type: JavaType, declared: KType?): List<String> {
        val out = mutableListOf<String>()
        describe(SchemaTarget(ROOT, type, declared), emptyList(), out)
        return out
    }

    private fun describe(target: SchemaTarget, ancestors: List<SchemaTarget>, out: MutableList<String>) {
        val resolved = target.copy(type = runtimeType(target.type))
        val ancestor = ancestors.firstOrNull { it.type == resolved.type }
        if (ancestor != null) {
            out += "${resolved.path} recursive=${ancestor.path}"
            return
        }
        val shape = shapeOf(target, resolved.type)
        out += line(resolved, shape)
        children(resolved, shape).forEach { describe(it, ancestors + resolved, out) }
    }

    private fun shapeOf(target: SchemaTarget, type: JavaType): SchemaShape {
        val shape = SchemaShape()
        val fixedType = if (target.declared?.classifier == Nothing::class) NULL_TYPE else FIXED_TYPES[type.rawClass]
        if (fixedType != null) {
            shape.jsonType = fixedType
            return shape
        }
        serializerOf(target, type).acceptJsonFormatVisitor(SchemaShapeVisitor(shape, provider), type)
        return shape
    }

    private fun serializerOf(target: SchemaTarget, type: JavaType): JsonFormatVisitable {
        val visitable = target.visitable?.takeIf { type == target.type }
        if (visitable != null) {
            return visitable
        }
        val property = target.property ?: return provider.findValueSerializer(type)
        return provider.findPrimaryPropertySerializer(type, property)
    }

    private fun children(target: SchemaTarget, shape: SchemaShape): List<SchemaTarget> = when (shape.jsonType) {
        "object" -> properties(target, shape)
        "array" -> listOf(element(target, "[]", shape, ELEMENT_ARGUMENT))
        "map" -> listOf(element(target, ".*", shape, MAP_VALUE_ARGUMENT))
        else -> emptyList()
    }

    private fun properties(target: SchemaTarget, shape: SchemaShape): List<SchemaTarget> {
        val owner = KotlinProperties.ownerOf(target.declared, target.type)
        val children = if (isUnordered(target.type)) shape.properties.sortedBy { it.name } else shape.properties
        return children.map {
            val declared = KotlinProperties.typeOf(owner, it.member)
            SchemaTarget("${target.path}.${it.name}", it.type ?: unknownType(), declared, it.visitable, it.property)
        }
    }

    private fun element(target: SchemaTarget, suffix: String, shape: SchemaShape, argument: Int): SchemaTarget {
        val child = shape.element
        val type = child?.type ?: target.type.contentType ?: unknownType()
        val declared = target.declared?.arguments?.getOrNull(argument)?.type
        return SchemaTarget(target.path + suffix, type, declared, child?.visitable)
    }

    private fun line(target: SchemaTarget, shape: SchemaShape): String = listOfNotNull(
        target.path,
        shape.jsonType,
        nullability(target.declared),
        shape.details.joinToString(" ").ifEmpty { null },
        ORDER_UNFIXED.takeIf { isUnordered(target.type) }
    ).joinToString(" ")

    private fun nullability(declared: KType?): String? = when {
        declared == null -> null
        declared.isMarkedNullable -> "nullable"
        declared.classifier is KTypeParameter -> null
        else -> "non-null"
    }

    private fun runtimeType(type: JavaType): JavaType {
        if (type.rawClass != Page::class.java) {
            return type
        }
        return mapper.typeFactory.constructParametricType(PageImpl::class.java, type.containedTypeOrUnknown(0))
    }

    private fun isUnordered(type: JavaType): Boolean = UNORDERED.any { it.isAssignableFrom(type.rawClass) }

    private fun unknownType(): JavaType = TypeFactory.unknownType()

    companion object {
        const val ROOT = "$"
        const val ORDER_UNFIXED = "순서비고정(이름순)"
        private const val ELEMENT_ARGUMENT = 0
        private const val MAP_VALUE_ARGUMENT = 1
        private val UNORDERED = listOf(Page::class.java, Pageable::class.java, Sort::class.java)
        private const val NULL_TYPE = "null"
        private val FIXED_TYPES = mapOf(Any::class.java to "any", Void::class.java to NULL_TYPE)
    }
}
