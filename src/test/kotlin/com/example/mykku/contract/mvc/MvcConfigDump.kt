package com.example.mykku.contract.mvc

import jakarta.servlet.MultipartConfigElement
import org.apache.catalina.util.ServerInfo
import org.hibernate.validator.HibernateValidator
import org.springframework.boot.SpringBootVersion
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties
import org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties
import org.springframework.context.ApplicationContext
import org.springframework.core.Ordered
import org.springframework.core.SpringVersion
import org.springframework.core.annotation.AnnotationAwareOrderComparator
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.web.accept.ContentNegotiationManager
import org.springframework.web.accept.ContentNegotiationStrategy
import org.springframework.web.accept.FixedContentNegotiationStrategy
import org.springframework.web.accept.ParameterContentNegotiationStrategy
import org.springframework.web.filter.CharacterEncodingFilter
import org.springframework.web.multipart.MultipartResolver
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite
import org.springframework.web.servlet.mvc.ParameterizableViewController
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler

class MvcConfigDump(private val context: ApplicationContext) {

    fun render(): String = listOf(
        section("라이브러리 버전", versions()),
        section("메시지 컨버터(핸들러 어댑터)", converters(bean<RequestMappingHandlerAdapter>().messageConverters)),
        section("메시지 컨버터(@ExceptionHandler)", converters(exceptionHandlerResolver().messageConverters)),
        section("콘텐츠 협상", contentNegotiation()),
        section("경로 매칭", pathMatching()),
        section("핸들러 매핑(순서·URL 매핑, /actuator·/docs 제외)", handlerMappings()),
        section("예외 리졸버", exceptionResolvers()),
        section("multipart", multipart()),
        section("CharacterEncodingFilter", characterEncoding()),
        section("ServerProperties", PropertyPaths.render("server", bean<ServerProperties>(), SERVER_PATHS)),
        section("WebMvcProperties", PropertyPaths.render("spring.mvc", bean<WebMvcProperties>(), MVC_PATHS)),
        section("WebProperties", PropertyPaths.render("spring.web", bean<WebProperties>(), WEB_PATHS))
    ).joinToString("\n")

    private fun versions(): List<String> = listOf(
        "spring-boot ${SpringBootVersion.getVersion()}",
        "spring-framework ${SpringVersion.getVersion()}",
        "tomcat ${ServerInfo.getServerInfo()}",
        "hibernate-validator ${HibernateValidator::class.java.`package`.implementationVersion}"
    )

    private fun converters(converters: List<HttpMessageConverter<*>>): List<String> =
        converters.mapIndexed { index, converter ->
            val charset = (converter as? AbstractHttpMessageConverter<*>)?.defaultCharset?.name() ?: "-"
            "${index + 1} ${converter.javaClass.simpleName} types=${converter.supportedMediaTypes} charset=$charset"
        }

    private fun exceptionHandlerResolver(): ExceptionHandlerExceptionResolver =
        context.getBean("handlerExceptionResolver", HandlerExceptionResolverComposite::class.java).exceptionResolvers
            .filterIsInstance<ExceptionHandlerExceptionResolver>()
            .single()

    private fun contentNegotiation(): List<String> {
        val manager = context.getBean("mvcContentNegotiationManager", ContentNegotiationManager::class.java)
        return manager.strategies.map { strategyOf(it) } + listOf(
            "file-extensions=${manager.allFileExtensions.sorted()}",
            "media-type-mappings=${manager.mediaTypeMappings.toSortedMap()}"
        )
    }

    private fun strategyOf(strategy: ContentNegotiationStrategy): String = when (strategy) {
        is ParameterContentNegotiationStrategy ->
            "strategy ParameterContentNegotiationStrategy parameter=${strategy.parameterName} " +
                "media-types=${strategy.mediaTypes.toSortedMap()}"
        is FixedContentNegotiationStrategy -> "strategy FixedContentNegotiationStrategy ${strategy.contentTypes}"
        else -> "strategy ${strategy.javaClass.simpleName}"
    }

    @Suppress("DEPRECATION")
    private fun pathMatching(): List<String> {
        val mapping = context.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping::class.java)
        return listOf(
            "uses-path-patterns=${mapping.usesPathPatterns()}",
            "case-sensitive=${mapping.patternParser?.isCaseSensitive}",
            "match-optional-trailing-separator=${mapping.patternParser?.isMatchOptionalTrailingSeparator}",
            "trailing-slash-match=${mapping.useTrailingSlashMatch()}",
            "path-prefixes=${mapping.pathPrefixes.keys.sorted()}"
        )
    }

    private fun handlerMappings(): List<String> = context.getBeansOfType(HandlerMapping::class.java).values
        .filterNot { it.javaClass.name.startsWith(ACTUATOR_PACKAGE) }
        .sortedWith(AnnotationAwareOrderComparator.INSTANCE)
        .flatMap { mappingLines(it) }

    private fun mappingLines(mapping: HandlerMapping): List<String> {
        val urlMapping = mapping as? AbstractUrlHandlerMapping
        val root = urlMapping?.rootHandler?.let { listOf("  (root) -> ${handlerOf(it)}") }.orEmpty()
        val urls = urlMapping?.handlerMap.orEmpty()
            .filterKeys { pattern -> EXCLUDED_PATTERNS.none { pattern.startsWith(it) } }
            .toSortedMap()
            .map { (pattern, handler) -> "  $pattern -> ${handlerOf(handler)}" }
        return listOf("${orderOf(mapping)} ${mapping.javaClass.simpleName}") + root + urls
    }

    private fun handlerOf(handler: Any): String = when (handler) {
        is ResourceHttpRequestHandler -> "resources ${handler.locations.map { it.description }}"
        is ParameterizableViewController -> "view ${handler.viewName ?: handler.view} status=${handler.statusCode}"
        else -> handler.javaClass.simpleName
    }

    private fun exceptionResolvers(): List<String> = context.getBeansOfType(HandlerExceptionResolver::class.java).values
        .sortedWith(AnnotationAwareOrderComparator.INSTANCE)
        .flatMap { resolver ->
            val nested = (resolver as? HandlerExceptionResolverComposite)?.exceptionResolvers.orEmpty()
            val head = "${orderOf(resolver)} ${resolver.javaClass.simpleName}"
            listOf(head) + nested.map { "  ${it.javaClass.simpleName}" }
        }

    private fun multipart(): List<String> {
        val element = bean<MultipartConfigElement>()
        return listOf(
            "config-element.max-file-size=${element.maxFileSize}",
            "config-element.max-request-size=${element.maxRequestSize}",
            "config-element.file-size-threshold=${element.fileSizeThreshold}",
            "config-element.location=${if (element.location.isNullOrEmpty()) "(없음)" else "(설정됨, 값 생략)"}",
            "resolver=${bean<MultipartResolver>().javaClass.simpleName}"
        ) + PropertyPaths.render("spring.servlet.multipart", bean<MultipartProperties>(), MULTIPART_PATHS)
    }

    private fun characterEncoding(): List<String> {
        val filter = bean<CharacterEncodingFilter>()
        return listOf(
            "encoding=${filter.encoding}",
            "force-request-encoding=${filter.isForceRequestEncoding}",
            "force-response-encoding=${filter.isForceResponseEncoding}"
        )
    }

    private fun orderOf(bean: Any): String = (bean as? Ordered)?.order?.toString() ?: "-"

    private inline fun <reified T : Any> bean(): T = context.getBean(T::class.java)

    private fun section(title: String, lines: List<String>): String =
        (listOf("# $title") + lines).joinToString("\n", postfix = "\n")

    private companion object {
        const val ACTUATOR_PACKAGE = "org.springframework.boot.actuate."
        val EXCLUDED_PATTERNS = listOf("/actuator", "/docs")
        val SERVER_PATHS = listOf(
            "servlet.context-path", "servlet.encoding.charset", "servlet.encoding.force",
            "servlet.encoding.force-request", "servlet.encoding.force-response", "servlet.encoding.mapping",
            "servlet.session.timeout", "servlet.session.tracking-modes", "servlet.session.cookie.name",
            "servlet.session.cookie.domain", "servlet.session.cookie.path", "servlet.session.cookie.http-only",
            "servlet.session.cookie.secure", "servlet.session.cookie.max-age", "servlet.session.cookie.same-site",
            "servlet.session.cookie.partitioned", "error.path", "error.include-exception", "error.include-stacktrace",
            "error.include-message", "error.include-binding-errors", "error.include-path", "error.whitelabel.enabled",
            "compression.enabled", "compression.mime-types", "compression.excluded-user-agents",
            "compression.min-response-size", "http2.enabled", "max-http-request-header-size",
            "forward-headers-strategy", "server-header", "tomcat.uri-encoding", "tomcat.max-http-form-post-size",
            "tomcat.max-swallow-size", "tomcat.max-http-response-header-size", "tomcat.relaxed-path-chars",
            "tomcat.relaxed-query-chars", "tomcat.redirect-context-root", "tomcat.use-relative-redirects",
            "tomcat.remoteip.internal-proxies", "tomcat.remoteip.protocol-header",
            "tomcat.remoteip.protocol-header-https-value", "tomcat.remoteip.host-header",
            "tomcat.remoteip.port-header", "tomcat.remoteip.remote-ip-header", "tomcat.remoteip.trusted-proxies"
        )
        val MVC_PATHS = listOf(
            "servlet.path", "static-path-pattern", "webjars-path-pattern", "dispatch-options-request",
            "dispatch-trace-request", "message-codes-resolver-format", "format.date", "format.time", "format.date-time",
            "contentnegotiation.favor-parameter", "contentnegotiation.parameter-name", "contentnegotiation.media-types",
            "pathmatch.matching-strategy", "problemdetails.enabled"
        )
        val WEB_PATHS = listOf("locale", "locale-resolver", "resources.add-mappings", "resources.static-locations")
        val MULTIPART_PATHS = listOf(
            "enabled", "max-file-size", "max-request-size", "file-size-threshold", "resolve-lazily",
            "strict-servlet-compliance"
        )
    }
}
