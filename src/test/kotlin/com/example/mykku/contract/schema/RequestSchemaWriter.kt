package com.example.mykku.contract.schema

import com.example.mykku.contract.dump.AttributeValues
import com.example.mykku.contract.dump.KotlinTypes
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition
import jakarta.validation.Validator
import jakarta.validation.metadata.BeanDescriptor
import jakarta.validation.metadata.PropertyDescriptor
import org.springframework.beans.BeanUtils
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.NumberFormat
import org.springframework.web.bind.annotation.BindParam
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField

private data class DtoScope(
    val klass: KClass<*>,
    val prefix: String,
    val binding: RequestBinding,
    val applied: Boolean,
    val ancestors: Set<KClass<*>>,
    val bindings: Map<KTypeParameter, KType>,
    val groups: List<Class<*>>
)

private data class NestedDto(val klass: KClass<*>, val suffix: String, val type: KType)

private data class FieldContext(
    val scope: DtoScope,
    val bean: BeanDescriptor,
    val creators: Map<Int, BeanPropertyDefinition>,
    val defaults: Map<String, String>
)

class RequestSchemaWriter(private val mapper: ObjectMapper, private val validator: Validator) {

    private val enumTraits = JacksonEnumTraits(mapper)

    fun render(target: RequestTarget): String {
        val nested = nestedDto(target.type)
        val typeLine = "${ResponseSchemaWriter.ROOT} type=${KotlinTypes.render(target.type)}"
        if (nested == null) {
            return SchemaBlocks.render(target.header, listOf(typeLine))
        }
        val isObject = nested.suffix == OBJECT_SUFFIX
        val lines = if (isObject) mutableListOf() else mutableListOf(typeLine)
        describe(rootScope(target, nested, if (isObject) "" else ResponseSchemaWriter.ROOT + nested.suffix), lines)
        return SchemaBlocks.render(target.header, lines)
    }

    private fun rootScope(target: RequestTarget, nested: NestedDto, prefix: String): DtoScope {
        val bindings = KotlinTypes.bindingsOf(nested.type)
        return DtoScope(nested.klass, prefix, target.binding, target.validated, emptySet(), bindings, target.groups)
    }

    private fun describe(scope: DtoScope, lines: MutableList<String>) {
        val bean = validator.getConstraintsForClass(scope.klass.java)
        lines += bean.constraintDescriptors
            .map { ConstraintLines.classLine(scope.prefix, it, scope.applied, scope.groups) }
            .sorted()
        val parameters = scope.klass.primaryConstructor?.parameters.orEmpty()
        val context = FieldContext(scope, bean, creatorsOf(scope), DtoDefaults.of(scope.klass))
        parameters.forEach { field(context, it, lines) }
        lines += extraProperties(scope, bean, parameters.mapNotNull { it.name }.toSet())
    }

    private fun field(context: FieldContext, parameter: KParameter, lines: MutableList<String>) {
        val scope = context.scope
        val property = context.bean.getConstraintsForProperty(parameter.name)
        val type = KotlinTypes.substitute(parameter.type, scope.bindings)
        val wireName = context.creators[parameter.index]?.name ?: bindName(parameter)
        lines += fieldLine(context, parameter, type, wireName, property)
        lines += ConstraintLines.of(property, scope.applied, scope.groups)
        nested(scope, type, wireName, property)?.let { describe(it, lines) }
    }

    private fun fieldLine(
        context: FieldContext,
        parameter: KParameter,
        type: KType,
        wireName: String,
        property: PropertyDescriptor?
    ): String = listOfNotNull(
        context.scope.prefix + wireName,
        "kotlin=${parameter.name}",
        "type=${KotlinTypes.render(type)}",
        "default=${context.defaults[parameter.name] ?: DtoDefaults.NONE}",
        jacksonTokens(context, parameter, type),
        "cascade".takeIf { ConstraintLines.isCascaded(property) },
        formats(context.scope.klass, parameter)
    ).joinToString(" ")

    private fun jacksonTokens(context: FieldContext, parameter: KParameter, type: KType): String? {
        if (context.scope.binding != RequestBinding.JSON) {
            return null
        }
        val required = "jackson-required=${context.creators[parameter.index]?.metadata?.required ?: "unknown"}"
        return listOfNotNull(required, enumTraits.of(type)).joinToString(" ")
    }

    private fun nested(scope: DtoScope, type: KType, wireName: String, property: PropertyDescriptor?): DtoScope? {
        val nested = nestedDto(type) ?: return null
        if (nested.klass in scope.ancestors || nested.klass == scope.klass) {
            return null
        }
        val applied = scope.applied && ConstraintLines.isCascaded(property)
        val prefix = scope.prefix + wireName + nested.suffix
        val bindings = KotlinTypes.bindingsOf(nested.type)
        val ancestors = scope.ancestors + scope.klass
        return DtoScope(nested.klass, prefix, scope.binding, applied, ancestors, bindings, scope.groups)
    }

    private fun nestedDto(type: KType): NestedDto? {
        val klass = type.classifier as? KClass<*> ?: return null
        if (KotlinTypes.isProjectDto(klass)) {
            return NestedDto(klass, OBJECT_SUFFIX, type)
        }
        val element = type.arguments.firstOrNull()?.type ?: return null
        val elementClass = element.classifier as? KClass<*> ?: return null
        val isDtoList = KotlinTypes.isCollection(klass) && KotlinTypes.isProjectDto(elementClass)
        return if (isDtoList) NestedDto(elementClass, ELEMENT_SUFFIX, element) else null
    }

    private fun extraProperties(scope: DtoScope, bean: BeanDescriptor, constructorNames: Set<String>): List<String> {
        val settable = settableOutsideConstructor(scope, constructorNames)
        val constrained = bean.constrainedProperties.associateBy { it.propertyName }
            .filterKeys { it !in constructorNames }
        return (settable.keys + constrained.keys).sorted().flatMap { name ->
            listOf(extraLine(scope, name, settable[name])) + constraintsOf(scope, constrained[name])
        }
    }

    private fun constraintsOf(scope: DtoScope, property: PropertyDescriptor?): List<String> =
        ConstraintLines.of(property, scope.applied, scope.groups)

    private fun extraLine(scope: DtoScope, name: String, settable: String?): String =
        "${scope.prefix}$name (생성자 밖 프로퍼티: ${settable ?: "검증 전용"})"

    private fun settableOutsideConstructor(scope: DtoScope, constructorNames: Set<String>): Map<String, String> {
        if (scope.binding == RequestBinding.FORM) {
            return BeanUtils.getPropertyDescriptors(scope.klass.java)
                .filter { it.writeMethod != null && it.name !in constructorNames }
                .associate { it.name to "setter type=${it.propertyType.simpleName}" }
        }
        return jacksonProperties(scope.klass)
            .filter { it.constructorParameter == null }
            .mapNotNull { property -> jacksonMutator(property)?.let { property.name to it } }
            .toMap()
    }

    private fun jacksonMutator(property: BeanPropertyDefinition): String? {
        val kind = when {
            property.hasSetter() -> "setter"
            property.hasField() -> "field"
            isSetterlessContainer(property) -> "getter-as-setter"
            else -> return null
        }
        return "$kind type=${property.rawPrimaryType.simpleName}"
    }

    private fun isSetterlessContainer(property: BeanPropertyDefinition): Boolean {
        if (!mapper.isEnabled(MapperFeature.USE_GETTERS_AS_SETTERS) || !property.hasGetter()) {
            return false
        }
        val type = property.getter.rawType
        return Collection::class.java.isAssignableFrom(type) || Map::class.java.isAssignableFrom(type)
    }

    private fun creatorsOf(scope: DtoScope): Map<Int, BeanPropertyDefinition> {
        if (scope.binding != RequestBinding.JSON) {
            return emptyMap()
        }
        return jacksonProperties(scope.klass)
            .filter { it.constructorParameter != null }
            .associateBy { it.constructorParameter.index }
    }

    private fun jacksonProperties(klass: KClass<*>): List<BeanPropertyDefinition> =
        mapper.deserializationConfig.introspect(mapper.constructType(klass.java)).findProperties()

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
        const val OBJECT_SUFFIX = "."
        const val ELEMENT_SUFFIX = "[]."
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
