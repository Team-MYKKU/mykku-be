package com.example.mykku.contract.schema

import com.example.mykku.contract.dump.AttributeValues
import com.example.mykku.contract.dump.KotlinTypes
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

object DtoDefaults {

    const val NONE = "none"
    private const val UNKNOWN = "(계산 불가)"

    fun of(klass: KClass<*>): Map<String, String> {
        val constructor = klass.primaryConstructor ?: return emptyMap()
        val instance = instantiate(constructor)
        return constructor.parameters.filter { it.isOptional }
            .associate { it.name.orEmpty() to rendered(klass, instance, it) }
    }

    fun instantiate(constructor: KFunction<*>): Any? = runCatching {
        constructor.isAccessible = true
        constructor.callBy(constructor.parameters.filterNot { it.isOptional }.associateWith { dummy(it.type) })
    }.getOrNull()

    private fun rendered(klass: KClass<*>, instance: Any?, parameter: KParameter): String {
        val property = klass.memberProperties.firstOrNull { it.name == parameter.name }
        if (instance == null || property == null) {
            return UNKNOWN
        }
        property.isAccessible = true
        return AttributeValues.value(property.getter.call(instance))
    }

    private fun dummy(type: KType): Any? {
        val klass = type.classifier as? KClass<*> ?: return null
        KotlinTypes.valueClassUnderlying(klass)?.let { return klass.primaryConstructor?.call(dummy(it)) }
        klass.java.enumConstants?.let { return it.firstOrNull() }
        if (KotlinTypes.isProjectDto(klass)) {
            return klass.primaryConstructor?.let { instantiate(it) }
        }
        return SIMPLE[klass] ?: COLLECTIONS[klass]
    }

    private val SIMPLE: Map<KClass<*>, Any> = mapOf(
        String::class to "x",
        Int::class to 1,
        Long::class to 1L,
        Short::class to 1.toShort(),
        Double::class to 1.0,
        Float::class to 1.0f,
        Boolean::class to false,
        BigDecimal::class to BigDecimal.ONE,
        LocalDate::class to LocalDate.of(2001, 1, 1),
        LocalDateTime::class to LocalDateTime.of(2001, 1, 1, 0, 0),
        LocalTime::class to LocalTime.of(0, 0),
        MultipartFile::class to MockMultipartFile("dummy", ByteArray(0))
    )

    private val COLLECTIONS: Map<KClass<*>, Any> = mapOf(
        List::class to emptyList<Any>(),
        Collection::class to emptyList<Any>(),
        Set::class to emptySet<Any>(),
        Map::class to emptyMap<Any, Any>()
    )
}
