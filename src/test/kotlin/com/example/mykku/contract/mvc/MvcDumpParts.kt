package com.example.mykku.contract.mvc

import com.example.mykku.contract.dump.KotlinTypes
import org.springframework.context.ApplicationContext
import org.springframework.core.Ordered
import org.springframework.core.ResolvableType
import org.springframework.core.annotation.AnnotationAwareOrderComparator
import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.converter.ConverterFactory
import org.springframework.core.convert.converter.GenericConverter
import org.springframework.core.io.Resource
import org.springframework.format.Parser
import org.springframework.format.Printer
import org.springframework.util.ClassUtils
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.servlet.function.HandlerFunction
import org.springframework.web.servlet.function.RequestPredicate
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.RouterFunctions
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.support.RouterFunctionMapping
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping
import org.springframework.web.servlet.mvc.ParameterizableViewController
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
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

class TypeConverterLines(private val context: ApplicationContext, private val boundTypes: Set<Class<*>>) {

    fun render(): List<String> = converters().ifEmpty { listOf(NONE) } + listOf(addFormattersLine())

    private fun converters(): List<String> = CONVERTER_TYPES.flatMap { context.getBeansOfType(it).values }
        .distinct()
        .filter { isProject(ClassUtils.getUserClass(it)) }
        .flatMap { describe(it) }
        .filter { (_, target) -> target == null || boundTypes.any { target.isAssignableFrom(it) } }
        .map { (line, _) -> line }
        .sorted()

    private fun describe(bean: Any): List<Pair<String, Class<*>?>> {
        val type = ClassUtils.getUserClass(bean)
        val generics = listOfNotNull(
            generics(type, Converter::class.java)?.let { converterLine(it, "") },
            generics(type, ConverterFactory::class.java)?.let { converterLine(it, "(하위 타입)") },
            generics(type, Printer::class.java)?.let { "${typeName(it[0])} -> String(Printer)" to it[0] },
            generics(type, Parser::class.java)?.let { "String -> ${typeName(it[0])}(Parser)" to it[0] }
        )
        val pairs = (bean as? GenericConverter)?.convertibleTypes.orEmpty()
        return generics + pairs.map { "${typeName(it.sourceType)} -> ${typeName(it.targetType)}" to it.targetType }
    }

    private fun converterLine(generics: List<Class<*>?>, suffix: String): Pair<String, Class<*>?> =
        "${typeName(generics[0])} -> ${typeName(generics[1])}$suffix" to generics[1]

    private fun addFormattersLine(): String {
        val overriding = context.getBeansOfType(WebMvcConfigurer::class.java).values
            .map { ClassUtils.getUserClass(it) }
            .filter { isProject(it) && it.declaredMethods.any { method -> method.name == ADD_FORMATTERS } }
        return "add-formatters-overrides=${overriding.size}"
    }

    private fun generics(type: Class<*>, contract: Class<*>): List<Class<*>?>? {
        if (!contract.isAssignableFrom(type)) {
            return null
        }
        return ResolvableType.forClass(type).`as`(contract).resolveGenerics().toList()
    }

    private fun typeName(type: Class<*>?): String =
        type?.kotlin?.let { KotlinTypes.render(it.starProjectedType) } ?: "?"

    private fun isProject(type: Class<*>): Boolean = type.name.startsWith(PROJECT_PACKAGE)

    private companion object {
        const val PROJECT_PACKAGE = "com.example.mykku."
        const val NONE = "(핸들러 인자 타입에 닿는 프로젝트 변환기 없음)"
        const val ADD_FORMATTERS = "addFormatters"
        val CONVERTER_TYPES = listOf(
            Converter::class.java,
            ConverterFactory::class.java,
            GenericConverter::class.java,
            Printer::class.java,
            Parser::class.java
        )
    }
}
