package com.example.mykku.contract.schema

import com.example.mykku.contract.dump.AttributeValues
import jakarta.validation.metadata.ConstraintDescriptor
import jakarta.validation.metadata.ContainerElementTypeDescriptor
import jakarta.validation.metadata.PropertyDescriptor

object ConstraintLines {

    private const val INDENT = "  "
    private const val NOT_APPLIED = "미적용 "

    fun of(property: PropertyDescriptor?, applied: Boolean): List<String> {
        if (property == null) {
            return emptyList()
        }
        val marker = marker(applied)
        val direct = property.constraintDescriptors.map { "$INDENT$marker${render(it)}" }
        return (direct + property.constrainedContainerElementTypes.flatMap { elementLines(it, marker) }).sorted()
    }

    fun marker(applied: Boolean): String = if (applied) "" else NOT_APPLIED

    fun render(descriptor: ConstraintDescriptor<*>): String {
        val name = descriptor.annotation.annotationClass.simpleName.orEmpty()
        val composing = descriptor.composingConstraints.map { "composing=${render(it)}" }.sorted()
        return (listOf(AttributeValues.attributes(name, descriptor.attributes)) + composing).joinToString(" ")
    }

    fun isCascaded(property: PropertyDescriptor?): Boolean =
        property != null && (property.isCascaded || property.constrainedContainerElementTypes.any { it.isCascaded })

    private fun elementLines(element: ContainerElementTypeDescriptor, marker: String): List<String> {
        val prefix = "$INDENT${marker}element[${element.typeArgumentIndex}]"
        val cascade = if (element.isCascaded) listOf("$prefix cascade") else emptyList()
        return cascade + element.constraintDescriptors.map { "$prefix ${render(it)}" }
    }
}
