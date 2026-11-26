package com.example.mykku.contract.schema

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase
import com.fasterxml.jackson.databind.deser.CreatorProperty
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext
import com.fasterxml.jackson.databind.deser.SettableBeanProperty
import com.fasterxml.jackson.databind.deser.impl.FieldProperty
import com.fasterxml.jackson.databind.deser.impl.MethodProperty
import com.fasterxml.jackson.databind.deser.impl.SetterlessProperty

class JacksonSettables(private val mapper: ObjectMapper) {

    fun outsideConstructor(type: Class<*>): Map<String, String> = deserializer(type).properties().asSequence()
        .filterNot { it is CreatorProperty }
        .associate { it.name to "${kindOf(it)} type=${it.type.rawClass.simpleName}" }

    private fun deserializer(type: Class<*>): BeanDeserializerBase {
        val blueprint = mapper.deserializationContext as DefaultDeserializationContext
        val context = blueprint.createDummyInstance(mapper.deserializationConfig)
        val deserializer = context.findRootValueDeserializer(mapper.constructType(type))
        return checkNotNull(deserializer as? BeanDeserializerBase) {
            "${type.name}의 Jackson 역직렬화기 ${deserializer.javaClass.name}는 요청 DTO 덤프가 해석하지 않습니다"
        }
    }

    private fun kindOf(property: SettableBeanProperty): String = when (property) {
        is MethodProperty -> "setter"
        is FieldProperty -> "field"
        is SetterlessProperty -> "getter-as-setter"
        else -> property.javaClass.simpleName
    }
}
