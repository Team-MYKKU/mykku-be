package com.example.mykku.admin.config

import com.example.mykku.admin.exception.AdminException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.security.MessageDigest

@Component
class AdminInterceptor(
    @Value("\${admin.token}")
    private val adminToken: String
) : HandlerInterceptor {

    companion object {
        private const val ADMIN_SESSION_KEY = "ADMIN_AUTHENTICATED"
        private const val ADMIN_API_PREFIX = "/admin/api/"
    }

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        val session = request.getSession(false)
        val isAuthenticated = session?.getAttribute(ADMIN_SESSION_KEY) as? Boolean ?: false

        if (isAuthenticated) {
            return true
        }
        if (request.requestURI.startsWith(ADMIN_API_PREFIX)) {
            throw AdminException.unauthorized()
        }
        response.sendRedirect("/admin/login")
        return false
    }

    fun authenticate(
        token: String,
        request: HttpServletRequest
    ): Boolean {
        if (!MessageDigest.isEqual(token.toByteArray(), adminToken.toByteArray())) {
            return false
        }

        val session = request.getSession(true)
        request.changeSessionId()
        session.setAttribute(ADMIN_SESSION_KEY, true)
        session.maxInactiveInterval = 3600

        return true
    }

    fun logout(request: HttpServletRequest) {
        request.getSession(false)?.invalidate()
    }
}
