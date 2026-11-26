package com.example.mykku.contract.schema

import com.example.mykku.contract.dump.KotlinTypes
import com.example.mykku.contract.support.Endpoint
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.MethodParameter
import org.springframework.core.ResolvableType
import org.springframework.web.method.support.HandlerMethodReturnValueHandler
import org.springframework.web.servlet.mvc.method.annotation.HttpEntityMethodProcessor
import org.springframework.web.servlet.mvc.method.annotation.ModelAndViewMethodReturnValueHandler
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor
import org.springframework.web.servlet.mvc.method.annotation.ViewNameMethodReturnValueHandler
import kotlin.reflect.KType
import kotlin.reflect.jvm.kotlinFunction

class ResponseBodies(
    private val mapper: ObjectMapper,
    private val returnValueHandlers: List<HandlerMethodReturnValueHandler>
) {

    private val writer = ResponseSchemaWriter(mapper)

    fun describe(endpoint: Endpoint): List<String> {
        val returnType = endpoint.handlerMethod.returnType
        val declared = endpoint.handlerMethod.method.takeIf { KotlinTypes.isKotlin(it.declaringClass.kotlin) }
            ?.kotlinFunction
            ?.returnType
        return when (val handler = returnValueHandlers.firstOrNull { it.supportsReturnType(returnType) }) {
            is HttpEntityMethodProcessor -> body(entityBody(returnType), declared?.arguments?.firstOrNull()?.type)
            is RequestResponseBodyMethodProcessor -> body(ResolvableType.forMethodParameter(returnType), declared)
            is ViewNameMethodReturnValueHandler, is ModelAndViewMethodReturnValueHandler -> listOf("$ROOT view")
            else -> listOf("$ROOT ${handler?.javaClass?.simpleName ?: "unhandled"}")
        }
    }

    private fun entityBody(returnType: MethodParameter): ResolvableType =
        ResolvableType.forType(returnType.genericParameterType).getGeneric(0)

    private fun body(type: ResolvableType, declared: KType?): List<String> {
        val raw = type.resolve() ?: Any::class.java
        if (raw in EMPTY_BODIES) {
            return listOf("$ROOT empty")
        }
        return writer.lines(mapper.typeFactory.constructType(type.type), declared)
    }

    private companion object {
        const val ROOT = ResponseSchemaWriter.ROOT
        val EMPTY_BODIES = setOf(Unit::class.java, Void::class.java, Void.TYPE)
    }
}
