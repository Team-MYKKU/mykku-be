package com.example.mykku.contract.schema

import com.example.mykku.contract.dump.AttributeValues
import jakarta.validation.groups.Default
import jakarta.validation.metadata.ConstraintDescriptor
import jakarta.validation.metadata.ContainerElementTypeDescriptor
import jakarta.validation.metadata.PropertyDescriptor

object ConstraintLines {

    private const val INDENT = "  "
    private const val NOT_APPLIED = "미적용 "

    fun of(property: PropertyDescriptor?, applied: Boolean, groups: List<Class<*>>): List<String> {
        if (property == null) {
            return emptyList()
        }
        val direct = property.constraintDescriptors.map { "$INDENT${marked(it, applied, groups)}" }
        val elements = property.constrainedContainerElementTypes.flatMap { elementLines(it, applied, groups) }
        return (direct + elements).sorted()
    }

    fun classLine(
        prefix: String,
        descriptor: ConstraintDescriptor<*>,
        applied: Boolean,
        groups: List<Class<*>>
    ): String = "$prefix(class) ${marked(descriptor, applied, groups)}"

    fun render(descriptor: ConstraintDescriptor<*>): String {
        val name = descriptor.annotation.annotationClass.simpleName.orEmpty()
        val composing = descriptor.composingConstraints.map { "composing=${render(it)}" }.sorted()
        return (listOf(AttributeValues.attributes(name, descriptor.attributes)) + composing).joinToString(" ")
    }

    fun isCascaded(property: PropertyDescriptor?): Boolean =
        property != null && (property.isCascaded || property.constrainedContainerElementTypes.any { it.isCascaded })

    private fun marked(descriptor: ConstraintDescriptor<*>, applied: Boolean, groups: List<Class<*>>): String {
        val effective = applied && inGroups(descriptor, groups)
        return (if (effective) "" else NOT_APPLIED) + render(descriptor)
    }

    private fun inGroups(descriptor: ConstraintDescriptor<*>, groups: List<Class<*>>): Boolean {
        val requested = groups.ifEmpty { listOf(Default::class.java) }
        return descriptor.groups.any { group -> requested.any { group.isAssignableFrom(it) } }
    }

    private fun elementLines(
        element: ContainerElementTypeDescriptor,
        applied: Boolean,
        groups: List<Class<*>>
    ): List<String> {
        val prefix = "${INDENT}element[${element.typeArgumentIndex}]"
        val cascade = if (element.isCascaded) listOf("$prefix cascade") else emptyList()
        return cascade + element.constraintDescriptors.map { "$prefix ${marked(it, applied, groups)}" }
    }
}
