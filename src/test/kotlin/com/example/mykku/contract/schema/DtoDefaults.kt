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

    fun of(klass: KClass<*>): Map<String, String> {
        val constructor = klass.primaryConstructor ?: return emptyMap()
        val optional = constructor.parameters.filter { it.isOptional }
        if (optional.isEmpty()) {
            return emptyMap()
        }
        val instance = instanceOf(klass, constructor)
        return optional.associate { it.name.orEmpty() to rendered(klass, instance, it) }
    }

    private fun instanceOf(klass: KClass<*>, constructor: KFunction<*>): Any? {
        val withDummies = runCatching { construct(constructor, setOf(klass), nulls = false) }
        if (withDummies.isSuccess) {
            return withDummies.getOrNull()
        }
        return runCatching { construct(constructor, setOf(klass), nulls = true) }.getOrElse {
            val message = "${klass.qualifiedName} 기본값을 계산하지 못했습니다. 필수 인자 더미 값을 보강합니다"
            throw IllegalStateException(message, withDummies.exceptionOrNull())
        }
    }

    private fun construct(constructor: KFunction<*>, visiting: Set<KClass<*>>, nulls: Boolean): Any? {
        constructor.isAccessible = true
        val required = constructor.parameters.filterNot { it.isOptional }
        return constructor.callBy(required.associateWith { dummy(it.type, visiting, nulls) })
    }

    private fun rendered(klass: KClass<*>, instance: Any?, parameter: KParameter): String {
        val property = checkNotNull(klass.memberProperties.firstOrNull { it.name == parameter.name }) {
            "${klass.qualifiedName}의 기본값 있는 생성자 인자 ${parameter.name}이 프로퍼티가 아니라 기본값을 읽을 수 없습니다"
        }
        property.isAccessible = true
        return AttributeValues.value(property.getter.call(instance))
    }

    private fun dummy(type: KType, visiting: Set<KClass<*>>, nulls: Boolean): Any? {
        if (nulls && type.isMarkedNullable) {
            return null
        }
        val klass = type.classifier as? KClass<*> ?: return null
        KotlinTypes.valueClassUnderlying(klass)?.let { return valueOf(klass, dummy(it, visiting, nulls)) }
        klass.java.enumConstants?.let { return it.firstOrNull() }
        if (KotlinTypes.isProjectDto(klass) && klass !in visiting) {
            return klass.primaryConstructor?.let { construct(it, visiting + klass, nulls) }
        }
        return SIMPLE[klass] ?: COLLECTIONS[klass]
    }

    private fun valueOf(klass: KClass<*>, underlying: Any?): Any? = klass.primaryConstructor?.call(underlying)

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
