package com.example.mykku.contract.endpoint

import com.example.mykku.contract.dump.AttributeValues
import com.example.mykku.contract.dump.KotlinTypes
import com.example.mykku.contract.support.ArgumentKind
import com.example.mykku.contract.support.HandlerArgument
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class ArgumentTokens(private val resolvers: List<HandlerMethodArgumentResolver>) {

    fun of(argument: HandlerArgument): String? = when (argument.kind) {
        ArgumentKind.QUERY -> named("query", argument) + requirement(argument) + format(argument)
        ArgumentKind.PATH -> named("path", argument) + optional(argument) + format(argument)
        ArgumentKind.HEADER -> named("header", argument) + requirement(argument)
        ArgumentKind.COOKIE -> named("cookie", argument) + requirement(argument)
        ArgumentKind.BODY -> "body:json" + valid(argument) + optional(argument)
        ArgumentKind.PART -> "part ${argument.name}:${partType(argument)}" + valid(argument) + optional(argument)
        ArgumentKind.MODEL -> "model:form" + valid(argument)
        ArgumentKind.PAGEABLE, ArgumentKind.SORT -> "${argument.kind.name.lowercase()} ${defaults(argument)}"
        ArgumentKind.MEMBER -> "member"
        ArgumentKind.FRAMEWORK -> null
    }

    private fun named(prefix: String, argument: HandlerArgument): String =
        "$prefix ${argument.name}:${argument.kotlinType?.let(KotlinTypes::render) ?: javaType(argument)}"

    private fun javaType(argument: HandlerArgument): String = argument.parameter.nestedParameterType.simpleName

    private fun requirement(argument: HandlerArgument): String = when {
        argument.defaultValue != null -> " default=${AttributeValues.value(argument.defaultValue)}"
        argument.required -> " required"
        else -> " optional"
    }

    private fun optional(argument: HandlerArgument): String = if (argument.required) "" else " optional"

    private fun valid(argument: HandlerArgument): String = if (argument.validated) " valid" else ""

    private fun format(argument: HandlerArgument): String =
        argument.parameter.getParameterAnnotation(DateTimeFormat::class.java)
            ?.let { " format=${AttributeValues.annotation(it)}" }
            .orEmpty()

    private fun partType(argument: HandlerArgument): String {
        if (!argument.file) {
            return "json"
        }
        val type = argument.parameter.nestedIfOptional().nestedParameterType
        return if (Collection::class.java.isAssignableFrom(type) || type.isArray) "file[]" else "file"
    }

    private fun defaults(argument: HandlerArgument): String {
        val resolver = resolvers.first { it.supportsParameter(argument.parameter) }
        val request = ServletWebRequest(MockHttpServletRequest())
        return when (val value = resolver.resolveArgument(argument.parameter, ModelAndViewContainer(), request, null)) {
            is Pageable -> "default=page:${value.pageNumber},size:${value.pageSize},sort:${sortOf(value.sort)}"
            is Sort -> "default=sort:${sortOf(value)}"
            else -> "default=$value"
        }
    }

    private fun sortOf(sort: Sort): String =
        sort.joinToString(";") { "${it.property}:${it.direction}" }.ifEmpty { "unsorted" }
}
