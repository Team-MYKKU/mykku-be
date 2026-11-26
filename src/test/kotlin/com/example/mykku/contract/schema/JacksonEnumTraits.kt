package com.example.mykku.contract.schema

import com.example.mykku.contract.dump.AttributeValues
import com.example.mykku.contract.dump.KotlinTypes
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.util.EnumResolver
import kotlin.reflect.KClass
import kotlin.reflect.KType

class JacksonEnumTraits(private val mapper: ObjectMapper) {

    fun of(type: KType): String? {
        val enumClass = enumClassOf(type) ?: return null
        return listOfNotNull(ids(enumClass), lowercase(enumClass), creator(enumClass), jsonValue(enumClass))
            .joinToString(" ")
            .ifEmpty { null }
    }

    private fun enumClassOf(type: KType): Class<*>? {
        val klass = type.classifier as? KClass<*> ?: return null
        if (klass.java.isEnum) {
            return klass.java
        }
        val element = (type.arguments.firstOrNull()?.type?.classifier as? KClass<*>)?.java
        return element?.takeIf { it.isEnum && KotlinTypes.isCollection(klass) }
    }

    private fun ids(enumClass: Class<*>): String? {
        val classInfo = mapper.deserializationConfig.introspectClassAnnotations(enumClass).classInfo
        val ids = EnumResolver.constructFor(mapper.deserializationConfig, classInfo).enumIds.toSet()
        val names = enumClass.enumConstants.map { (it as Enum<*>).name }.toSet()
        return if (ids == names) null else "jackson-enum-ids=${AttributeValues.value(ids)}"
    }

    private fun lowercase(enumClass: Class<*>): String? {
        val name = (enumClass.enumConstants.firstOrNull() as? Enum<*>)?.name ?: return null
        if (name.lowercase() == name) {
            return null
        }
        val accepted = runCatching { mapper.readerFor(enumClass).readValue<Any>("\"${name.lowercase()}\"") }.isSuccess
        return if (accepted) "jackson-enum-lowercase=accepted" else null
    }

    private fun creator(enumClass: Class<*>): String? {
        val description = mapper.deserializationConfig.introspect(mapper.constructType(enumClass))
        val creators = description.factoryMethods.count { it.hasAnnotation(JsonCreator::class.java) }
        return if (creators > 0) "jackson-enum-creator" else null
    }

    private fun jsonValue(enumClass: Class<*>): String? {
        val description = mapper.serializationConfig.introspect(mapper.constructType(enumClass))
        return if (description.findJsonValueAccessor() != null) "jackson-enum-json-value" else null
    }
}
