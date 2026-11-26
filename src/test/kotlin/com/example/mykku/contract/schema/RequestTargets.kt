package com.example.mykku.contract.schema

import com.example.mykku.contract.endpoint.ArgumentTokens
import com.example.mykku.contract.endpoint.EndpointKeys
import com.example.mykku.contract.support.ArgumentKind
import com.example.mykku.contract.support.Endpoint
import com.example.mykku.contract.support.HandlerArgument
import jakarta.validation.Valid
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.validation.annotation.Validated
import org.springframework.validation.annotation.ValidationAnnotationUtils
import org.springframework.web.servlet.mvc.method.annotation.HttpEntityMethodProcessor
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType

enum class RequestBinding { JSON, FORM }

data class RequestTarget(
    val header: String,
    val type: KType,
    val binding: RequestBinding,
    val validated: Boolean,
    val groups: List<Class<*>> = emptyList()
)

class RequestTargets(private val tokens: ArgumentTokens) {

    fun of(endpoint: Endpoint): List<RequestTarget> = endpoint.arguments.mapNotNull { argument ->
        bindingOf(argument)?.let { targetOf(endpoint, argument, it) }
    }

    private fun targetOf(endpoint: Endpoint, argument: HandlerArgument, binding: RequestBinding): RequestTarget {
        val validated = validated(endpoint, argument)
        return RequestTarget(headerOf(endpoint, argument), typeOf(argument), binding, validated, groupsOf(argument))
    }

    private fun headerOf(endpoint: Endpoint, argument: HandlerArgument): String =
        "${EndpointKeys.of(endpoint)} ${tokens.of(argument)}"

    private fun bindingOf(argument: HandlerArgument): RequestBinding? = when {
        argument.kind == ArgumentKind.BODY -> RequestBinding.JSON
        argument.kind == ArgumentKind.PART && !argument.file -> RequestBinding.JSON
        argument.kind == ArgumentKind.MODEL -> RequestBinding.FORM
        isHttpEntity(argument) -> RequestBinding.JSON
        else -> null
    }

    private fun validated(endpoint: Endpoint, argument: HandlerArgument): Boolean = when {
        isHttpEntity(argument) -> false
        isContainer(argument) -> hasValid(argument) && methodValidated(endpoint)
        else -> argument.validated
    }

    private fun hasValid(argument: HandlerArgument): Boolean =
        argument.parameter.hasParameterAnnotation(Valid::class.java)

    private fun methodValidated(endpoint: Endpoint): Boolean {
        val handler = endpoint.handlerMethod
        val validatedBean = AnnotatedElementUtils.hasAnnotation(handler.beanType, Validated::class.java)
        return handler.shouldValidateArguments() || validatedBean
    }

    private fun groupsOf(argument: HandlerArgument): List<Class<*>> = argument.parameter.parameterAnnotations
        .firstNotNullOfOrNull { ValidationAnnotationUtils.determineValidationHints(it) }
        .orEmpty()
        .filterIsInstance<Class<*>>()

    private fun isHttpEntity(argument: HandlerArgument): Boolean =
        argument.kind == ArgumentKind.FRAMEWORK && tokens.resolverOf(argument) is HttpEntityMethodProcessor

    private fun isContainer(argument: HandlerArgument): Boolean {
        val type = argument.parameter.nestedIfOptional().nestedParameterType
        return Collection::class.java.isAssignableFrom(type) || Map::class.java.isAssignableFrom(type) || type.isArray
    }

    private fun typeOf(argument: HandlerArgument): KType {
        val declared = argument.kotlinType ?: argument.parameter.nestedParameterType.kotlin.starProjectedType
        if (!isHttpEntity(argument)) {
            return declared
        }
        return declared.arguments.firstOrNull()?.type ?: Any::class.starProjectedType
    }
}
