package com.example.mykku.contract.schema

import jakarta.validation.GroupSequence
import jakarta.validation.groups.ConvertGroup
import org.hibernate.validator.group.GroupSequenceProvider
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Member
import java.lang.reflect.Parameter

object GroupFeatures {

    fun requireSupported(klass: Class<*>, groups: List<Class<*>>) {
        val found = classFeatures(klass) + groupSequences(groups) + conversions(klass)
        check(found.isEmpty()) { "요청 DTO 덤프는 검증 그룹 순서·변환을 해석하지 않습니다: $found" }
    }

    private fun classFeatures(klass: Class<*>): List<String> = listOfNotNull(
        "${klass.simpleName} @GroupSequence".takeIf { klass.isAnnotationPresent(GroupSequence::class.java) },
        "${klass.simpleName} @GroupSequenceProvider".takeIf { klass.isAnnotationPresent(SEQUENCE_PROVIDER) }
    )

    private fun groupSequences(groups: List<Class<*>>): List<String> = groups
        .filter { it.isAnnotationPresent(GroupSequence::class.java) }
        .map { "${it.simpleName} @GroupSequence" }

    private fun conversions(klass: Class<*>): List<String> = hierarchy(klass)
        .flatMap { members(it) }
        .filter { element -> CONVERSIONS.any { element.isAnnotationPresent(it) } }
        .map { "${klass.simpleName}.${nameOf(it)} @ConvertGroup" }
        .distinct()

    private fun hierarchy(klass: Class<*>): List<Class<*>> =
        generateSequence(klass) { it.superclass }.takeWhile { it != Any::class.java }.toList()

    private fun members(type: Class<*>): List<AnnotatedElement> {
        val parameters = type.declaredConstructors.flatMap { it.parameters.toList() }
        return type.declaredFields.toList() + type.declaredMethods + parameters
    }

    private fun nameOf(element: AnnotatedElement): String = (element as? Member)?.name ?: (element as Parameter).name

    private val SEQUENCE_PROVIDER = GroupSequenceProvider::class.java
    private val CONVERSIONS = listOf(ConvertGroup::class.java, ConvertGroup.List::class.java)
}
