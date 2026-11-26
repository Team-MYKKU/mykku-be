package com.example.mykku.contract.schema

import com.example.mykku.contract.dump.AttributeValues
import com.example.mykku.contract.dump.KotlinTypes
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition
import jakarta.validation.Validator
import jakarta.validation.metadata.BeanDescriptor
import jakarta.validation.metadata.PropertyDescriptor
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.NumberFormat
import org.springframework.web.bind.annotation.BindParam
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField

private data class DtoScope(
    val klass: KClass<*>,
    val prefix: String,
    val binding: RequestBinding,
    val applied: Boolean,
    val ancestors: Set<KClass<*>>
)

class RequestSchemaWriter(private val mapper: ObjectMapper, private val validator: Validator) {

    fun render(target: RequestTarget): String {
        val klass = target.type.classifier as? KClass<*>
        if (klass == null || !KotlinTypes.isProjectDto(klass)) {
            return SchemaBlocks.render(target.header, listOf("$ROOT type=${KotlinTypes.render(target.type)}"))
        }
        val lines = mutableListOf<String>()
        describe(DtoScope(klass, "", target.binding, target.validated, emptySet()), lines)
        return SchemaBlocks.render(target.header, lines)
    }

    private fun describe(scope: DtoScope, lines: MutableList<String>) {
        val bean = validator.getConstraintsForClass(scope.klass.java)
        val marker = ConstraintLines.marker(scope.applied)
        lines += bean.constraintDescriptors.map { "${scope.prefix}(class) $marker${ConstraintLines.render(it)}" }
            .sorted()
        val parameters = scope.klass.primaryConstructor?.parameters.orEmpty()
        val creators = if (scope.binding == RequestBinding.JSON) creatorProperties(scope.klass) else emptyMap()
        parameters.forEach { field(scope, bean, it, creators[it.index], lines) }
        lines += extraProperties(scope, bean, parameters.mapNotNull { it.name }.toSet())
    }

    private fun field(
        scope: DtoScope,
        bean: BeanDescriptor,
        parameter: KParameter,
        creator: BeanPropertyDefinition?,
        lines: MutableList<String>
    ) {
        val property = bean.getConstraintsForProperty(parameter.name)
        val wireName = creator?.name ?: bindName(parameter)
        lines += fieldLine(scope, parameter, wireName, creator, property)
        lines += ConstraintLines.of(property, scope.applied)
        nested(scope, parameter.type, wireName, property)?.let { describe(it, lines) }
    }

    private fun fieldLine(
        scope: DtoScope,
        parameter: KParameter,
        wireName: String,
        creator: BeanPropertyDefinition?,
        property: PropertyDescriptor?
    ): String = listOfNotNull(
        scope.prefix + wireName,
        "kotlin=${parameter.name}",
        "type=${KotlinTypes.render(parameter.type)}",
        "default=${if (parameter.isOptional) "yes" else "no"}",
        jacksonRequired(scope, creator),
        "cascade".takeIf { ConstraintLines.isCascaded(property) },
        formats(scope.klass, parameter)
    ).joinToString(" ")

    private fun jacksonRequired(scope: DtoScope, creator: BeanPropertyDefinition?): String? {
        if (scope.binding != RequestBinding.JSON) {
            return null
        }
        return "jackson-required=${creator?.metadata?.required ?: "unknown"}"
    }

    private fun nested(scope: DtoScope, type: KType, wireName: String, property: PropertyDescriptor?): DtoScope? {
        val (klass, suffix) = nestedClass(type) ?: return null
        if (klass in scope.ancestors || klass == scope.klass) {
            return null
        }
        val applied = scope.applied && ConstraintLines.isCascaded(property)
        return DtoScope(klass, scope.prefix + wireName + suffix, scope.binding, applied, scope.ancestors + scope.klass)
    }

    private fun nestedClass(type: KType): Pair<KClass<*>, String>? {
        val klass = type.classifier as? KClass<*> ?: return null
        if (KotlinTypes.isProjectDto(klass)) {
            return klass to "."
        }
        val element = type.arguments.firstOrNull()?.type?.classifier as? KClass<*> ?: return null
        return if (KotlinTypes.isCollection(klass) && KotlinTypes.isProjectDto(element)) element to "[]." else null
    }

    private fun extraProperties(scope: DtoScope, bean: BeanDescriptor, constructorNames: Set<String>): List<String> =
        bean.constrainedProperties
            .filter { it.propertyName !in constructorNames }
            .sortedBy { it.propertyName }
            .flatMap { listOf("${scope.prefix}${it.propertyName} (검증 전용 프로퍼티)") + constraintsOf(scope, it) }

    private fun constraintsOf(scope: DtoScope, property: PropertyDescriptor): List<String> =
        ConstraintLines.of(property, scope.applied)

    private fun creatorProperties(klass: KClass<*>): Map<Int, BeanPropertyDefinition> =
        mapper.deserializationConfig.introspect(mapper.constructType(klass.java)).findProperties()
            .filter { it.constructorParameter != null }
            .associateBy { it.constructorParameter.index }

    private fun bindName(parameter: KParameter): String =
        parameter.annotations.filterIsInstance<BindParam>().firstOrNull()?.value ?: parameter.name.orEmpty()

    private fun formats(klass: KClass<*>, parameter: KParameter): String? {
        val field = klass.memberProperties.firstOrNull { it.name == parameter.name }?.javaField
        val annotations = parameter.annotations + field?.annotations.orEmpty().toList()
        return annotations.filter { it.annotationClass.java in FORMAT_ANNOTATIONS }
            .map { AttributeValues.annotation(it) }
            .distinct()
            .sorted()
            .joinToString(" ")
            .ifEmpty { null }
    }

    private companion object {
        const val ROOT = "$"
        val FORMAT_ANNOTATIONS = setOf(
            DateTimeFormat::class.java,
            NumberFormat::class.java,
            JsonFormat::class.java,
            JsonProperty::class.java,
            JsonAlias::class.java,
            JsonIgnore::class.java,
            JsonSetter::class.java,
            BindParam::class.java
        )
    }
}
