package com.example.mykku.contract.mvc

import com.example.mykku.contract.dump.KotlinTypes
import com.example.mykku.contract.support.ArgumentKind
import com.example.mykku.contract.support.HandlerArgument
import com.example.mykku.contract.support.HandlerCatalog
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

object BoundTypes {

    private val CONVERTED_KINDS = setOf(ArgumentKind.QUERY, ArgumentKind.PATH, ArgumentKind.HEADER, ArgumentKind.COOKIE)

    fun of(catalog: HandlerCatalog): Set<Class<*>> = catalog.endpoints.flatMap { it.arguments }
        .flatMap { typesOf(it) }
        .toSet()

    private fun typesOf(argument: HandlerArgument): List<Class<*>> = when (argument.kind) {
        in CONVERTED_KINDS -> argument.kotlinType?.let { classesOf(it) }
            ?: listOf(argument.parameter.nestedParameterType)
        ArgumentKind.MODEL -> argument.kotlinType?.let { formFieldTypes(it, emptySet()) }.orEmpty()
        else -> emptyList()
    }

    private fun formFieldTypes(type: KType, visiting: Set<KClass<*>>): List<Class<*>> {
        val klass = type.classifier as? KClass<*> ?: return emptyList()
        if (klass in visiting) {
            return emptyList()
        }
        val fieldTypes = klass.primaryConstructor?.parameters.orEmpty().map { it.type } +
            klass.memberProperties.map { it.returnType }
        return fieldTypes.flatMap { classesOf(it) + nestedFieldTypes(it, visiting + klass) }
    }

    private fun nestedFieldTypes(type: KType, visiting: Set<KClass<*>>): List<Class<*>> {
        val klass = type.classifier as? KClass<*> ?: return emptyList()
        return if (KotlinTypes.isProjectDto(klass)) formFieldTypes(type, visiting) else emptyList()
    }

    private fun classesOf(type: KType): List<Class<*>> {
        val own = listOfNotNull((type.classifier as? KClass<*>)?.java)
        return own + type.arguments.mapNotNull { it.type }.flatMap { classesOf(it) }
    }
}
