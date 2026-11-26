package com.example.mykku.contract.schema

import jakarta.validation.GroupSequence
import jakarta.validation.groups.ConvertGroup
import org.hibernate.validator.group.GroupSequenceProvider
import java.lang.reflect.AnnotatedElement
import kotlin.reflect.full.allSuperclasses

object GroupFeatures {

    fun requireSupported(klass: Class<*>, groups: List<Class<*>>) {
        val found = superclasses(klass).flatMap { classFeatures(it) } + groupSequences(groups) + conversions(klass)
        check(found.isEmpty()) { "요청 DTO 덤프는 검증 그룹 순서·변환을 해석하지 않습니다: $found" }
    }

    private fun classFeatures(type: Class<*>): List<String> = listOfNotNull(
        "${type.simpleName} @GroupSequence".takeIf { type.isAnnotationPresent(GroupSequence::class.java) },
        "${type.simpleName} @GroupSequenceProvider".takeIf { type.isAnnotationPresent(SEQUENCE_PROVIDER) }
    )

    private fun groupSequences(groups: List<Class<*>>): List<String> = groups
        .filter { it.isAnnotationPresent(GroupSequence::class.java) }
        .map { "${it.simpleName} @GroupSequence" }

    private fun conversions(klass: Class<*>): List<String> = beanTypes(klass)
        .flatMap { members(it) }
        .filter { (_, element) -> CONVERSIONS.any { element.isAnnotationPresent(it) } }
        .map { (name, _) -> "${klass.simpleName}.$name @ConvertGroup" }
        .distinct()

    private fun superclasses(klass: Class<*>): List<Class<*>> =
        generateSequence(klass) { it.superclass }.takeWhile { it != Any::class.java }.toList()

    private fun beanTypes(klass: Class<*>): List<Class<*>> =
        (listOf(klass) + klass.kotlin.allSuperclasses.map { it.java }).filter { it != Any::class.java }.distinct()

    private fun members(type: Class<*>): List<Pair<String, AnnotatedElement>> =
        type.declaredFields.map { it.name to it } + type.declaredMethods.map { it.name to it }

    private val SEQUENCE_PROVIDER = GroupSequenceProvider::class.java
    private val CONVERSIONS = listOf(ConvertGroup::class.java, ConvertGroup.List::class.java)
}
