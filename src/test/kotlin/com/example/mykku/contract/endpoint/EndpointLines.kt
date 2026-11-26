package com.example.mykku.contract.endpoint

import com.example.mykku.contract.support.AuthLevel
import com.example.mykku.contract.support.Endpoint
import org.springframework.core.MethodIntrospector
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.util.ReflectionUtils
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.InitBinder

class EndpointLines(
    private val requiresAdminSession: (Endpoint) -> Boolean,
    private val argumentTokens: ArgumentTokens
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
        return listOfNotNull(
            "method-validation".takeIf { handler.shouldValidateArguments() },
            "return-validation".takeIf { handler.shouldValidateReturnValue() },
            "validated-bean".takeIf { AnnotatedElementUtils.hasAnnotation(handler.beanType, Validated::class.java) },
            "init-binder".takeIf { hasInitBinder(handler.beanType) }
        )
    }

    private fun hasInitBinder(beanType: Class<*>): Boolean =
        MethodIntrospector.selectMethods(beanType, INIT_BINDER_METHODS).isNotEmpty()

    private companion object {
        const val ADMIN_SESSION = "admin-session"
        val INIT_BINDER_METHODS = ReflectionUtils.MethodFilter {
            AnnotatedElementUtils.hasAnnotation(it, InitBinder::class.java)
        }
    }
}
