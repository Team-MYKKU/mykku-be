package com.example.mykku.contract.endpoint

import com.example.mykku.contract.support.AuthLevel
import com.example.mykku.contract.support.Endpoint
import org.springframework.core.MethodIntrospector
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.util.ReflectionUtils
import org.springframework.validation.annotation.Validated
import org.springframework.web.method.ControllerAdviceBean
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.INIT_BINDER_METHODS
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.MODEL_ATTRIBUTE_METHODS
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice

class EndpointLines(
    private val requiresAdminSession: (Endpoint) -> Boolean,
    private val argumentTokens: ArgumentTokens,
    private val advices: List<ControllerAdviceBean> = emptyList()
) {

    fun render(endpoint: Endpoint): String =
        (listOf(EndpointKeys.of(endpoint), "auth=${auth(endpoint)}", arguments(endpoint)) + bindingTokens(endpoint))
            .joinToString(" ")

    private fun auth(endpoint: Endpoint): String {
        val levels = listOfNotNull(
            ADMIN_SESSION.takeIf { requiresAdminSession(endpoint) },
            endpoint.auth.takeUnless { it == AuthLevel.NONE }?.name?.lowercase()
        )
        return levels.ifEmpty { listOf(AuthLevel.NONE.name.lowercase()) }.joinToString("+")
    }

    private fun arguments(endpoint: Endpoint): String =
        endpoint.arguments.mapNotNull { argumentTokens.of(it) }.joinToString(", ", "(", ")")

    private fun bindingTokens(endpoint: Endpoint): List<String> {
        val handler = endpoint.handlerMethod
        val types = listOf(handler.beanType) + advices.filter { it.isApplicableToBeanType(handler.beanType) }
            .mapNotNull { it.beanType }
        return listOfNotNull(
            "method-validation".takeIf { handler.shouldValidateArguments() },
            "return-validation".takeIf { handler.shouldValidateReturnValue() },
            "validated-bean".takeIf { AnnotatedElementUtils.hasAnnotation(handler.beanType, Validated::class.java) },
            "init-binder".takeIf { types.any { hasMethods(it, INIT_BINDER_METHODS) } },
            "model-attribute-method".takeIf { types.any { hasMethods(it, MODEL_ATTRIBUTE_METHODS) } },
            "request-body-advice".takeIf { types.drop(1).any { RequestBodyAdvice::class.java.isAssignableFrom(it) } },
            "response-body-advice".takeIf { types.drop(1).any { ResponseBodyAdvice::class.java.isAssignableFrom(it) } }
        )
    }

    private fun hasMethods(type: Class<*>, filter: ReflectionUtils.MethodFilter): Boolean =
        MethodIntrospector.selectMethods(type, filter).isNotEmpty()

    private companion object {
        const val ADMIN_SESSION = "admin-session"
    }
}
