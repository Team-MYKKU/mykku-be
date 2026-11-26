package com.example.mykku.contract.errorcode

import com.example.mykku.common.exception.BaseDomainException
import com.example.mykku.common.exception.DomainErrorCode
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

object DomainExceptions {

    private const val BASE_PACKAGE = "com.example.mykku"
    private const val ADDITIONAL_MESSAGE_PARAMETER = "additionalMessage"
    const val PROBE_MESSAGE = "contract-probe"

    private val constructors: Map<Class<*>, KFunction<Exception>> by lazy { errorCodeConstructors() }

    fun errorCodes(): List<DomainErrorCode> = productionTypes(DomainErrorCode::class.java)
        .filter { it.isEnum }
        .flatMap { type -> type.enumConstants.map { it as DomainErrorCode } }
        .sortedBy { it.code }

    fun create(errorCode: DomainErrorCode): Exception {
        val enumType = errorCode.javaClass.let { if (it.isEnum) it else it.superclass }
        val constructor = constructors[enumType] ?: error("${enumType.simpleName}를 받는 도메인 예외 생성자가 없습니다")
        val arguments = constructor.parameters.mapNotNull { argumentOf(it, errorCode) }.toMap()
        return constructor.callBy(arguments)
    }

    private fun argumentOf(parameter: KParameter, errorCode: DomainErrorCode): Pair<KParameter, Any?>? = when {
        parameter.index == 0 -> parameter to errorCode
        parameter.name == ADDITIONAL_MESSAGE_PARAMETER -> parameter to PROBE_MESSAGE
        else -> null
    }

    private fun errorCodeConstructors(): Map<Class<*>, KFunction<Exception>> =
        productionTypes(BaseDomainException::class.java)
            .flatMap { type -> exceptionConstructors(type.kotlin) }
            .groupBy({ it.first }, { it.second })
            .mapValues { (enumType, found) ->
                found.singleOrNull() ?: error("${enumType.simpleName}를 받는 도메인 예외 생성자가 여럿입니다: $found")
            }

    @Suppress("UNCHECKED_CAST")
    private fun exceptionConstructors(type: KClass<*>): List<Pair<Class<*>, KFunction<Exception>>> =
        type.constructors.mapNotNull { constructor ->
            val firstType = (constructor.parameters.firstOrNull()?.type?.classifier as? KClass<*>)?.java
            firstType?.takeIf { it.isEnum && DomainErrorCode::class.java.isAssignableFrom(it) }
                ?.let { it to constructor as KFunction<Exception> }
        }

    private fun productionTypes(type: Class<*>): List<Class<*>> {
        val scanner = ClassPathScanningCandidateComponentProvider(false)
        scanner.addIncludeFilter(AssignableTypeFilter(type))
        val productionLocation = type.protectionDomain.codeSource?.location
        return scanner.findCandidateComponents(BASE_PACKAGE)
            .map { Class.forName(it.beanClassName) }
            .filter { it.protectionDomain.codeSource?.location == productionLocation }
            .sortedBy { it.name }
    }
}
