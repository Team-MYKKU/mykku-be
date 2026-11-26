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
            "${endpoint.method} ${endpoint.pattern} 표본 요청이 핸들러로 가지 않습니다(기대: ${endpoint.handlerName})"
        }
        check((chain.handler as? HandlerMethod)?.method == endpoint.handlerMethod.method) {
            "${endpoint.method} ${endpoint.pattern} 표본 요청이 다른 핸들러로 갑니다(기대: ${endpoint.handlerName}): ${chain.handler}"
        }
        return chain.interceptorList.any { it is AdminInterceptor }
    }

    private fun sampleRequest(endpoint: Endpoint): MockHttpServletRequest {
        val method = endpoint.method.takeUnless { it == ANY_METHOD } ?: "GET"
        val request = MockHttpServletRequest(method, endpoint.pattern.replace(PATH_VARIABLE, SAMPLE_VALUE))
        positive(endpoint.consumes)?.let { request.contentType = it }
        request.addHeader(HttpHeaders.ACCEPT, positive(endpoint.produces) ?: MediaType.APPLICATION_JSON_VALUE)
        samples(endpoint.params).forEach { (name, value) -> request.addParameter(name, value) }
        samples(endpoint.headers).forEach { (name, value) -> request.addHeader(name, value) }
        ServletRequestPathUtils.parseAndCache(request)
        return request
    }

    private fun positive(expressions: List<String>): String? = expressions.firstOrNull { !it.startsWith(NEGATION) }

    private fun samples(expressions: List<String>): List<Pair<String, String>> = expressions
        .filterNot { it.startsWith(NEGATION) || it.contains("$NEGATION=") }
        .map { it.substringBefore('=') to it.substringAfter('=', SAMPLE_VALUE) }

    private companion object {
        const val ANY_METHOD = "*"
        const val NEGATION = "!"
        const val SAMPLE_VALUE = "1"
        val PATH_VARIABLE = Regex("\\{[^}]+}")
    }
}
