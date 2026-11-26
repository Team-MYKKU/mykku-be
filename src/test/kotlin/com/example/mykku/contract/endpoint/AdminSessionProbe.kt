package com.example.mykku.contract.endpoint

import com.example.mykku.admin.config.AdminInterceptor
import com.example.mykku.contract.support.Endpoint
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import org.springframework.web.util.ServletRequestPathUtils

class AdminSessionProbe(private val handlerMapping: RequestMappingHandlerMapping) {

    fun requires(endpoint: Endpoint): Boolean {
        val chain = checkNotNull(handlerMapping.getHandler(sampleRequest(endpoint))) {
            "${endpoint.method} ${endpoint.pattern} 표본 요청이 핸들러로 가지 않습니다(${endpoint.handlerName})"
        }
        check((chain.handler as? HandlerMethod)?.method == endpoint.handlerMethod.method) {
            "${endpoint.method} ${endpoint.pattern} 표본 요청이 다른 핸들러로 갑니다: ${chain.handler}"
        }
        return chain.interceptorList.any { it is AdminInterceptor }
    }

    private fun sampleRequest(endpoint: Endpoint): MockHttpServletRequest {
        val method = endpoint.method.takeUnless { it == ANY_METHOD } ?: "GET"
        val request = MockHttpServletRequest(method, endpoint.pattern.replace(PATH_VARIABLE, SAMPLE_PATH_VALUE))
        endpoint.consumes.firstOrNull()?.let { request.contentType = it }
        request.addHeader(HttpHeaders.ACCEPT, endpoint.produces.firstOrNull() ?: MediaType.APPLICATION_JSON_VALUE)
        ServletRequestPathUtils.parseAndCache(request)
        return request
    }

    private companion object {
        const val ANY_METHOD = "*"
        const val SAMPLE_PATH_VALUE = "1"
        val PATH_VARIABLE = Regex("\\{[^}]+}")
    }
}
