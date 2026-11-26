package com.example.mykku.contract.endpoint

import com.example.mykku.contract.dump.AttributeValues
import com.example.mykku.contract.dump.KotlinTypes
import com.example.mykku.contract.support.ArgumentKind
import com.example.mykku.contract.support.HandlerArgument
import jakarta.validation.Constraint
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.validation.annotation.ValidationAnnotationUtils
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.method.annotation.ErrorsMethodArgumentResolver
import org.springframework.web.method.annotation.ExpressionValueMethodArgumentResolver
import org.springframework.web.method.annotation.MapMethodProcessor
import org.springframework.web.method.annotation.ModelMethodProcessor
import org.springframework.web.method.annotation.SessionStatusMethodArgumentResolver
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.mvc.method.annotation.ContinuationHandlerMethodArgumentResolver
import org.springframework.web.servlet.mvc.method.annotation.HttpEntityMethodProcessor
import org.springframework.web.servlet.mvc.method.annotation.PrincipalMethodArgumentResolver
import org.springframework.web.servlet.mvc.method.annotation.RedirectAttributesMethodArgumentResolver
import org.springframework.web.servlet.mvc.method.annotation.ServletRequestMethodArgumentResolver
import org.springframework.web.servlet.mvc.method.annotation.ServletResponseMethodArgumentResolver
import org.springframework.web.servlet.mvc.method.annotation.UriComponentsBuilderMethodArgumentResolver

class ArgumentTokens(private val resolvers: List<HandlerMethodArgumentResolver>) {

    fun of(argument: HandlerArgument): String? = baseToken(argument)?.let { it + constraints(argument) }

    fun resolverOf(argument: HandlerArgument): HandlerMethodArgumentResolver? =
        resolvers.firstOrNull { it.supportsParameter(argument.parameter) }

    private fun baseToken(argument: HandlerArgument): String? = when (argument.kind) {
        ArgumentKind.QUERY -> named("query", argument) + requirement(argument) + format(argument)
        ArgumentKind.PATH -> named("path", argument) + optional(argument) + format(argument)
        ArgumentKind.HEADER -> named("header", argument) + requirement(argument)
        ArgumentKind.COOKIE -> named("cookie", argument) + requirement(argument)
        ArgumentKind.BODY -> "body:json" + valid(argument) + optional(argument)
        ArgumentKind.PART -> "part ${argument.name}:${partType(argument)}" + valid(argument) + optional(argument)
        ArgumentKind.MODEL -> "model:form" + valid(argument)
        ArgumentKind.PAGEABLE, ArgumentKind.SORT -> "${argument.kind.name.lowercase()} ${defaults(argument)}"
        ArgumentKind.MEMBER -> "member"
        ArgumentKind.FRAMEWORK -> frameworkToken(argument)
    }

    private fun frameworkToken(argument: HandlerArgument): String? = when (val resolver = resolverOf(argument)) {
        is ErrorsMethodArgumentResolver -> "errors"
        is HttpEntityMethodProcessor -> "body:http-entity" + (if (argument.validated) " valid(무시됨)" else "")
        null -> named("framework:unresolved", argument)
        else -> if (BINDING_AGNOSTIC.any { it.isInstance(resolver) }) null else frameworkNamed(resolver, argument)
    }

    private fun frameworkNamed(resolver: HandlerMethodArgumentResolver, argument: HandlerArgument): String =
        named("framework:${resolver.javaClass.simpleName}", argument)

    private fun named(prefix: String, argument: HandlerArgument): String =
        "$prefix ${argument.name}:${argument.kotlinType?.let(KotlinTypes::render) ?: javaType(argument)}"

    private fun javaType(argument: HandlerArgument): String = argument.parameter.nestedParameterType.simpleName

    private fun requirement(argument: HandlerArgument): String = when {
        argument.defaultValue != null -> " default=${AttributeValues.value(argument.defaultValue)}"
        argument.required -> " required"
        else -> " optional"
    }

    private fun optional(argument: HandlerArgument): String = if (argument.required) "" else " optional"

    private fun valid(argument: HandlerArgument): String {
        if (!argument.validated) {
            return ""
        }
        val groups = argument.parameter.parameterAnnotations
            .firstNotNullOfOrNull { ValidationAnnotationUtils.determineValidationHints(it) }
            .orEmpty()
        return if (groups.isEmpty()) " valid" else " valid(groups=${AttributeValues.value(groups)})"
    }

    private fun constraints(argument: HandlerArgument): String = argument.parameter.parameterAnnotations
        .filter { it.annotationClass.java.isAnnotationPresent(Constraint::class.java) }
        .map { " ${AttributeValues.annotation(it)}" }
        .sorted()
        .joinToString("")

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
        val resolver = checkNotNull(resolverOf(argument)) { "${argument.parameter} 를 해석하는 리졸버가 없습니다" }
        val request = ServletWebRequest(MockHttpServletRequest())
        return when (val value = resolver.resolveArgument(argument.parameter, ModelAndViewContainer(), request, null)) {
            is Pageable -> "default=page:${value.pageNumber},size:${value.pageSize},sort:${sortOf(value.sort)}"
            is Sort -> "default=sort:${sortOf(value)}"
            else -> "default=$value"
        }
    }

    private fun sortOf(sort: Sort): String =
        sort.joinToString(";") { "${it.property}:${it.direction}" }.ifEmpty { "unsorted" }

    private companion object {
        val BINDING_AGNOSTIC = listOf(
            ServletRequestMethodArgumentResolver::class.java,
            ServletResponseMethodArgumentResolver::class.java,
            ModelMethodProcessor::class.java,
            MapMethodProcessor::class.java,
            RedirectAttributesMethodArgumentResolver::class.java,
            SessionStatusMethodArgumentResolver::class.java,
            UriComponentsBuilderMethodArgumentResolver::class.java,
            PrincipalMethodArgumentResolver::class.java,
            ExpressionValueMethodArgumentResolver::class.java,
            ContinuationHandlerMethodArgumentResolver::class.java
        )
    }
}
