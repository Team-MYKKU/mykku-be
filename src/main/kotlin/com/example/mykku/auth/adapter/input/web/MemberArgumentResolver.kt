package com.example.mykku.auth.adapter.input.web

import com.example.mykku.auth.adapter.output.persistence.JwtTokenProviderAdapter
import com.example.mykku.auth.config.CurrentMember
import com.example.mykku.auth.exception.AuthException
import com.example.mykku.member.adapter.output.persistence.MemberJpaRepository
import com.example.mykku.member.domain.entity.Member
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class MemberArgumentResolver(
    private val jwtTokenProvider: JwtTokenProviderAdapter,
    private val memberJpaRepository: MemberJpaRepository
) : HandlerMethodArgumentResolver {

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(CurrentMember::class.java) &&
                Member::class.java.isAssignableFrom(parameter.parameterType)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Any? {
        val request = webRequest.getNativeRequest(HttpServletRequest::class.java)
            ?: return handleNullableParameter(parameter)

        val token = extractToken(request)
            ?: return handleNullableParameter(parameter)

        if (!jwtTokenProvider.validateToken(token) || !jwtTokenProvider.isAccessToken(token)) {
            return handleNullableParameter(parameter)
        }

        val memberId = jwtTokenProvider.getMemberIdFromToken(token)

        val memberJpaEntity = memberJpaRepository.findById(memberId).orElse(null)
            ?: return handleNullableParameter(parameter)

        return memberJpaEntity.toDomain()
    }

    private fun handleNullableParameter(parameter: MethodParameter): Any? {
        val annotation = parameter.getParameterAnnotation(CurrentMember::class.java)

        return if (annotation != null && !annotation.required) {
            null
        } else {
            throw AuthException.unauthorized()
        }
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val authHeader = request.getHeader(AUTHORIZATION_HEADER)

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length)
        }

        return null
    }
}
