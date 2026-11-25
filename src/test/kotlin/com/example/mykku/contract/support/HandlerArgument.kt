package com.example.mykku.contract.support

import com.example.mykku.auth.config.CurrentMember
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.core.MethodParameter
import org.springframework.validation.annotation.ValidationAnnotationUtils
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ValueConstants
import org.springframework.web.method.HandlerMethod
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.multipart.support.MultipartResolutionDelegate
import org.springframework.web.servlet.mvc.method.annotation.PathVariableMethodArgumentResolver
import org.springframework.web.servlet.mvc.method.annotation.RequestPartMethodArgumentResolver
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor
import org.springframework.web.servlet.mvc.method.annotation.ServletModelAttributeMethodProcessor
import kotlin.reflect.KType
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.kotlinFunction

enum class ArgumentKind { QUERY, PATH, BODY, PART, MODEL, MEMBER, FRAMEWORK }

data class HandlerArgument(
    val kind: ArgumentKind,
    val name: String,
    val kotlinType: KType?,
    val required: Boolean,
    val defaultValue: String?,
    val validated: Boolean,
    val file: Boolean,
    val parameter: MethodParameter
)

class HandlerArgumentReader(private val resolvers: List<HandlerMethodArgumentResolver>) {

    private val nameDiscoverer = DefaultParameterNameDiscoverer()

    fun read(handlerMethod: HandlerMethod): List<HandlerArgument> {
        val kotlinParameters = handlerMethod.method.kotlinFunction?.valueParameters
        return handlerMethod.methodParameters.map { original ->
            val parameter = original.clone().apply { initParameterNameDiscovery(nameDiscoverer) }
            argument(parameter, kotlinParameters?.getOrNull(parameter.parameterIndex)?.type)
        }
    }

    private fun argument(parameter: MethodParameter, kotlinType: KType?): HandlerArgument {
        val kind = kindOf(parameter)
        val defaultValue = defaultValue(parameter)
        return HandlerArgument(
            kind = kind,
            name = bindingName(kind, parameter),
            kotlinType = kotlinType,
            required = declaredRequired(kind, parameter) && !optional(kind, parameter) && defaultValue == null,
            defaultValue = defaultValue,
            validated = isValidated(parameter),
            file = isFile(parameter),
            parameter = parameter
        )
    }

    private fun kindOf(parameter: MethodParameter): ArgumentKind {
        if (parameter.hasParameterAnnotation(CurrentMember::class.java)) {
            return ArgumentKind.MEMBER
        }
        return when (resolvers.firstOrNull { it.supportsParameter(parameter) }) {
            is RequestParamMethodArgumentResolver -> ArgumentKind.QUERY
            is PathVariableMethodArgumentResolver -> ArgumentKind.PATH
            is RequestResponseBodyMethodProcessor -> ArgumentKind.BODY
            is RequestPartMethodArgumentResolver -> ArgumentKind.PART
            is ServletModelAttributeMethodProcessor -> ArgumentKind.MODEL
            else -> ArgumentKind.FRAMEWORK
        }
    }

    private fun bindingName(kind: ArgumentKind, parameter: MethodParameter): String {
        val declared = when (kind) {
            ArgumentKind.QUERY -> parameter.getParameterAnnotation(RequestParam::class.java)?.name
            ArgumentKind.PATH -> parameter.getParameterAnnotation(PathVariable::class.java)?.name
            ArgumentKind.PART -> parameter.getParameterAnnotation(RequestPart::class.java)?.name
            else -> null
        }
        return declared?.takeIf { it.isNotEmpty() } ?: parameter.parameterName.orEmpty()
    }

    private fun declaredRequired(kind: ArgumentKind, parameter: MethodParameter): Boolean = when (kind) {
        ArgumentKind.QUERY -> parameter.getParameterAnnotation(RequestParam::class.java)?.required ?: false
        ArgumentKind.PATH -> parameter.getParameterAnnotation(PathVariable::class.java)?.required ?: true
        ArgumentKind.BODY -> parameter.getParameterAnnotation(RequestBody::class.java)?.required ?: true
        ArgumentKind.PART -> parameter.getParameterAnnotation(RequestPart::class.java)?.required ?: true
        ArgumentKind.MEMBER -> parameter.getParameterAnnotation(CurrentMember::class.java)?.required ?: true
        ArgumentKind.MODEL, ArgumentKind.FRAMEWORK -> false
    }

    private fun optional(kind: ArgumentKind, parameter: MethodParameter): Boolean =
        kind != ArgumentKind.MEMBER && parameter.isOptional

    private fun defaultValue(parameter: MethodParameter): String? =
        parameter.getParameterAnnotation(RequestParam::class.java)?.defaultValue
            ?.takeIf { it != ValueConstants.DEFAULT_NONE }

    private fun isValidated(parameter: MethodParameter): Boolean =
        parameter.parameterAnnotations.any { ValidationAnnotationUtils.determineValidationHints(it) != null }

    private fun isFile(parameter: MethodParameter): Boolean = MultipartResolutionDelegate.isMultipartArgument(parameter)
}
