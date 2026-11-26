package com.example.mykku.contract.dump

import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver

object MvcBeans {

    fun exceptionHandlerResolver(resolver: HandlerExceptionResolver): ExceptionHandlerExceptionResolver {
        val resolvers = (resolver as? HandlerExceptionResolverComposite)?.exceptionResolvers.orEmpty()
        val handlers = resolvers.filterIsInstance<ExceptionHandlerExceptionResolver>()
        check(handlers.size == 1) {
            "@ExceptionHandler 리졸버가 하나가 아닙니다. 덤프가 어느 쪽 컨버터를 볼지 정해야 합니다: ${resolvers.map { it.javaClass.name }}"
        }
        return handlers.single()
    }
}

object DumpSections {

    fun render(title: String, lines: List<String>): String =
        (listOf("# $title") + lines).joinToString("\n", postfix = "\n")
}
