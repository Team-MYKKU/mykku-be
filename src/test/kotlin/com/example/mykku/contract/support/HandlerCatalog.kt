package com.example.mykku.contract.support

import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

enum class EndpointArea { APP, ADMIN, OTHER }

enum class AuthLevel { NONE, OPTIONAL, REQUIRED }

data class Endpoint(
    val method: String,
    val pattern: String,
    val consumes: List<String>,
    val produces: List<String>,
    val params: List<String>,
    val headers: List<String>,
    val arguments: List<HandlerArgument>,
    val handlerMethod: HandlerMethod
) {
    val area: EndpointArea
        get() = when {
            pattern.startsWith("/api/") -> EndpointArea.APP
            pattern == "/admin" || pattern.startsWith("/admin/") -> EndpointArea.ADMIN
            else -> EndpointArea.OTHER
        }

    val auth: AuthLevel
        get() {
            val member = arguments.firstOrNull { it.kind == ArgumentKind.MEMBER } ?: return AuthLevel.NONE
            return if (member.required) AuthLevel.REQUIRED else AuthLevel.OPTIONAL
        }

    val paged: Boolean
        get() = queryNames().containsAll(PAGE_PARAMETERS)

    val pathArguments: List<HandlerArgument>
        get() = arguments.filter { it.kind == ArgumentKind.PATH }

    val handlerName: String
        get() = "${handlerMethod.beanType.simpleName}#${handlerMethod.method.name}"

    private fun queryNames(): Set<String> = arguments.filter { it.kind == ArgumentKind.QUERY }.map { it.name }.toSet()

    private companion object {
        val PAGE_PARAMETERS = setOf("page", "size")
    }
}

class HandlerCatalog(
    handlerMapping: RequestMappingHandlerMapping,
    handlerAdapter: RequestMappingHandlerAdapter
) {

    private val argumentReader = HandlerArgumentReader(handlerAdapter.argumentResolvers.orEmpty())

    val endpoints: List<Endpoint> = handlerMapping.handlerMethods
        .flatMap { (info, handlerMethod) -> endpointsOf(info, handlerMethod) }
        .sortedWith(ENDPOINT_ORDER)

    fun find(method: String, pattern: String): Endpoint {
        val matches = endpoints.filter { it.method == method && it.pattern == pattern }
        check(matches.size == 1) { "엔드포인트 $method $pattern 를 하나로 찾지 못했습니다: ${matches.map { it.handlerName }}" }
        return matches.single()
    }

    fun inArea(area: EndpointArea): List<Endpoint> = endpoints.filter { it.area == area }

    private fun endpointsOf(info: RequestMappingInfo, handlerMethod: HandlerMethod): List<Endpoint> {
        val arguments = argumentReader.read(handlerMethod)
        val methods = info.methodsCondition.methods.map { it.name }.ifEmpty { listOf(ANY_METHOD) }
        return info.patternValues.flatMap { pattern ->
            methods.map { method -> endpoint(method, pattern, info, arguments, handlerMethod) }
        }
    }

    private fun endpoint(
        method: String,
        pattern: String,
        info: RequestMappingInfo,
        arguments: List<HandlerArgument>,
        handlerMethod: HandlerMethod
    ): Endpoint = Endpoint(
        method = method,
        pattern = pattern,
        consumes = info.consumesCondition.expressions.map { it.toString() }.sorted(),
        produces = info.producesCondition.expressions.map { it.toString() }.sorted(),
        params = info.paramsCondition.expressions.map { it.toString() }.sorted(),
        headers = info.headersCondition.expressions.map { it.toString() }.sorted(),
        arguments = arguments,
        handlerMethod = handlerMethod
    )

    private companion object {
        const val ANY_METHOD = "*"
        val ENDPOINT_ORDER: Comparator<Endpoint> = compareBy(
            { it.pattern },
            { it.method },
            { it.consumes.joinToString() },
            { it.produces.joinToString() },
            { it.params.joinToString() },
            { it.headers.joinToString() }
        )
    }
}
