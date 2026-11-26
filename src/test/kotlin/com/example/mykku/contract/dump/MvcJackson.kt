package com.example.mykku.contract.dump

import com.example.mykku.common.dto.ApiResponse
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.http.converter.GenericHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter

object MvcJackson {

    private val PROBE_TYPE = ApiResponse::class.java

    fun converter(adapter: RequestMappingHandlerAdapter): MappingJackson2HttpMessageConverter =
        converter(adapter.messageConverters)

    fun converter(converters: List<HttpMessageConverter<*>>): MappingJackson2HttpMessageConverter {
        val writer = converters.first { canWriteJson(it) }
        val reader = converters.first { canReadJson(it) }
        check(writer === reader && writer is MappingJackson2HttpMessageConverter) {
            "MVC가 JSON을 쓰고 읽는 컨버터가 하나의 Jackson 컨버터가 아닙니다: 쓰기 $writer, 읽기 $reader"
        }
        return writer
    }

    fun objectMapper(adapter: RequestMappingHandlerAdapter): ObjectMapper = converter(adapter).objectMapper

    private fun canWriteJson(converter: HttpMessageConverter<*>): Boolean = when (converter) {
        is GenericHttpMessageConverter<*> -> converter.canWrite(PROBE_TYPE, PROBE_TYPE, MediaType.APPLICATION_JSON)
        else -> converter.canWrite(PROBE_TYPE, MediaType.APPLICATION_JSON)
    }

    private fun canReadJson(converter: HttpMessageConverter<*>): Boolean = when (converter) {
        is GenericHttpMessageConverter<*> -> converter.canRead(PROBE_TYPE, null, MediaType.APPLICATION_JSON)
        else -> converter.canRead(PROBE_TYPE, MediaType.APPLICATION_JSON)
    }
}
