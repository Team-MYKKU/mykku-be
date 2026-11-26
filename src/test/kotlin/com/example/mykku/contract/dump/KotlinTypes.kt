package com.example.mykku.contract.dump

import org.springframework.web.multipart.MultipartFile
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.withNullability

object KotlinTypes {

    private const val PROJECT_PACKAGE = "com.example.mykku."

    fun render(type: KType): String {
        val base = when (val classifier = type.classifier) {
            is KClass<*> -> renderClass(classifier, type.arguments)
            is KTypeParameter -> classifier.name
            else -> type.toString()
        }
        return if (type.isMarkedNullable) "$base?" else base
    }

    fun enumConstants(klass: KClass<*>): List<String>? = klass.java.enumConstants?.map { (it as Enum<*>).name }

    fun isProjectDto(klass: KClass<*>): Boolean =
        klass.qualifiedName.orEmpty().startsWith(PROJECT_PACKAGE) && !klass.java.isEnum && isKotlin(klass)

    fun isKotlin(klass: KClass<*>): Boolean = klass.java.isAnnotationPresent(Metadata::class.java)

    fun isFile(klass: KClass<*>): Boolean = MultipartFile::class.java.isAssignableFrom(klass.java)

    fun isCollection(klass: KClass<*>): Boolean =
        Collection::class.java.isAssignableFrom(klass.java) || klass.java.isArray

    fun substitute(type: KType, bindings: Map<KTypeParameter, KType>): KType {
        val classifier = type.classifier
        if (classifier is KTypeParameter) {
            val bound = bindings[classifier] ?: return type
            return bound.withNullability(type.isMarkedNullable || bound.isMarkedNullable)
        }
        if (classifier !is KClass<*> || type.arguments.isEmpty()) {
            return type
        }
        return classifier.createType(type.arguments.map { substitute(it, bindings) }, type.isMarkedNullable)
    }

    fun bindingsOf(type: KType): Map<KTypeParameter, KType> {
        val klass = type.classifier as? KClass<*> ?: return emptyMap()
        return klass.typeParameters.zip(type.arguments)
            .mapNotNull { (parameter, argument) -> argument.type?.let { parameter to it } }
            .toMap()
    }

    private fun substitute(projection: KTypeProjection, bindings: Map<KTypeParameter, KType>): KTypeProjection {
        val type = projection.type ?: return projection
        return KTypeProjection(projection.variance, substitute(type, bindings))
    }

    private fun renderClass(klass: KClass<*>, arguments: List<KTypeProjection>): String {
        enumConstants(klass)?.let { return "enum[${it.joinToString(",")}]" }
        if (isFile(klass)) {
            return "file"
        }
        if (isProjectDto(klass)) {
            return "object"
        }
        val name = klass.simpleName ?: klass.java.name
        if (arguments.isEmpty()) {
            return name
        }
        return "$name<${arguments.joinToString(", ") { argument -> argument.type?.let(::render) ?: "*" }}>"
    }
}
