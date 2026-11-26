package com.example.mykku.contract.mvc

import com.example.mykku.contract.dump.KotlinTypes
import org.springframework.context.ApplicationContext
import org.springframework.core.MethodIntrospector
import org.springframework.core.Ordered
import org.springframework.core.ResolvableType
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.core.annotation.AnnotationAwareOrderComparator
import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.converter.ConverterFactory
import org.springframework.core.convert.converter.GenericConverter
import org.springframework.core.io.Resource
import org.springframework.format.Parser
import org.springframework.format.Printer
import org.springframework.util.ClassUtils
import org.springframework.util.ReflectionUtils
import org.springframework.web.bind.annotation.InitBinder
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.method.ControllerAdviceBean
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.servlet.function.HandlerFunction
import org.springframework.web.servlet.function.RequestPredicate
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.RouterFunctions
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.support.RouterFunctionMapping
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping
import org.springframework.web.servlet.mvc.ParameterizableViewController
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler
import java.util.Optional
import java.util.function.Function
import kotlin.reflect.full.starProjectedType

class HandlerMappingLines(private val context: ApplicationContext) {

    fun render(): List<String> = context.getBeansOfType(HandlerMapping::class.java).values
        .filterNot { it.javaClass.name.startsWith(ACTUATOR_PACKAGE) }
        .sortedWith(AnnotationAwareOrderComparator.INSTANCE)
        .flatMap { listOf("${orderOf(it)} ${it.javaClass.simpleName}") + urlLines(it) + routeLines(it) }

    private fun urlLines(mapping: HandlerMapping): List<String> {
        val urlMapping = mapping as? AbstractUrlHandlerMapping ?: return emptyList()
        val root = urlMapping.rootHandler?.let { listOf("  (root) -> ${handlerOf(it)}") }.orEmpty()
        return root + urlMapping.handlerMap
            .filterKeys { pattern -> EXCLUDED_PATTERNS.none { pattern.startsWith(it) } }
            .toSortedMap()
            .map { (pattern, handler) -> "  $pattern -> ${handlerOf(handler)}" }
    }

    private fun routeLines(mapping: HandlerMapping): List<String> {
        if (mapping !is RouterFunctionMapping) {
            return emptyList()
        }
        val routes = mutableListOf<String>()
        mapping.routerFunction?.accept(RouteCollector(routes))
        return routes.ifEmpty { listOf(NO_ROUTES) }.map { "  route $it" }
    }

    private fun handlerOf(handler: Any): String = when (handler) {
        is ResourceHttpRequestHandler -> "resources ${handler.locations.map { it.description }}"
        is ParameterizableViewController -> "view ${handler.viewName ?: handler.view} status=${handler.statusCode}"
        else -> handler.javaClass.simpleName
    }

    private fun orderOf(bean: Any): String = (bean as? Ordered)?.order?.toString() ?: "-"

    private companion object {
        const val ACTUATOR_PACKAGE = "org.springframework.boot.actuate."
        const val NO_ROUTES = "(없음)"
        val EXCLUDED_PATTERNS = listOf("/actuator", "/docs")
    }
}

private class RouteCollector(private val routes: MutableList<String>) : RouterFunctions.Visitor {

    private val nesting = ArrayDeque<String>()

    override fun startNested(predicate: RequestPredicate) {
        nesting.addLast(predicate.toString())
    }

    override fun endNested(predicate: RequestPredicate) {
        nesting.removeLast()
    }

    override fun route(predicate: RequestPredicate, handlerFunction: HandlerFunction<*>) {
        routes += (nesting + predicate.toString()).joinToString(" / ")
    }

    override fun resources(lookupFunction: Function<ServerRequest, Optional<Resource>>) {
        routes += (nesting + "resources").joinToString(" / ")
    }

    override fun attributes(attributes: MutableMap<String, Any>) = Unit

    override fun unknown(routerFunction: RouterFunction<*>) {
        routes += (nesting + "unknown").joinToString(" / ")
    }
}

class TypeConverterLines(private val context: ApplicationContext) {

    fun render(): List<String> = CONVERTER_TYPES.flatMap { context.getBeansOfType(it).values }
        .distinct()
        .filter { ClassUtils.getUserClass(it).name.startsWith(PROJECT_PACKAGE) }
        .flatMap { describe(it) }
        .sorted()
        .ifEmpty { listOf(NONE) }

    private fun describe(bean: Any): List<String> {
        val type = ClassUtils.getUserClass(bean)
        val generics = listOfNotNull(
            generics(type, Converter::class.java)?.let { "${it[0]} -> ${it[1]}" },
            generics(type, ConverterFactory::class.java)?.let { "${it[0]} -> ${it[1]}(하위 타입 포함)" },
            generics(type, Printer::class.java)?.let { "${it[0]} -> String(Printer)" },
            generics(type, Parser::class.java)?.let { "String -> ${it[0]}(Parser)" }
        )
        val pairs = (bean as? GenericConverter)?.convertibleTypes.orEmpty()
        return generics + pairs.map { "${typeName(it.sourceType)} -> ${typeName(it.targetType)}" }
    }

    private fun generics(type: Class<*>, contract: Class<*>): List<String>? {
        if (!contract.isAssignableFrom(type)) {
            return null
        }
        return ResolvableType.forClass(type).`as`(contract).resolveGenerics().map { typeName(it) }
    }

    private fun typeName(type: Class<*>?): String =
        type?.kotlin?.let { KotlinTypes.render(it.starProjectedType) } ?: "?"

    private companion object {
        const val PROJECT_PACKAGE = "com.example.mykku."
        const val NONE = "(없음)"
        val CONVERTER_TYPES = listOf(
            Converter::class.java,
            ConverterFactory::class.java,
            GenericConverter::class.java,
            Printer::class.java,
            Parser::class.java
        )
    }
}

class AdviceLines(private val context: ApplicationContext) {

    fun render(): List<String> {
        val advices = ControllerAdviceBean.findAnnotatedBeans(context).mapNotNull { it.beanType }
        return listOf(
            "request-body-advice=${advices.count { RequestBodyAdvice::class.java.isAssignableFrom(it) }}",
            "response-body-advice=${advices.count { ResponseBodyAdvice::class.java.isAssignableFrom(it) }}",
            "init-binder-methods=${advices.sumOf { methodCount(it, InitBinder::class.java) }}",
            "model-attribute-methods=${advices.sumOf { methodCount(it, ModelAttribute::class.java) }}"
        )
    }

    private fun methodCount(type: Class<*>, annotation: Class<out Annotation>): Int = MethodIntrospector.selectMethods(
        type,
        ReflectionUtils.MethodFilter { AnnotatedElementUtils.hasAnnotation(it, annotation) }
    ).size
}
