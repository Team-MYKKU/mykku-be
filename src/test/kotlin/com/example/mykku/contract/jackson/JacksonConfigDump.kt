package com.example.mykku.contract.jackson

import com.example.mykku.contract.dump.DumpSections
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.cfg.CoercionInputShape
import com.fasterxml.jackson.databind.cfg.DatatypeFeature
import com.fasterxml.jackson.databind.cfg.EnumFeature
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature
import com.fasterxml.jackson.databind.type.LogicalType
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import java.io.StringWriter

class JacksonConfigDump(private val mapper: ObjectMapper) {

    private val serialization = mapper.serializationConfig
    private val deserialization = mapper.deserializationConfig

    fun render(): String = listOf(
        section("라이브러리 버전", versions()),
        section("등록 모듈", mapper.registeredModuleIds.map { moduleId(it) }.sorted()),
        section("포함 규칙·명명·날짜", settings()),
        section("SerializationFeature", states(enumValues<SerializationFeature>()) { serialization.isEnabled(it) }),
        section("DeserializationFeature", deserializationFeatures()),
        section("MapperFeature", states(enumValues<MapperFeature>()) { serialization.isEnabled(it) }),
        section("JsonParser.Feature(실제 파서)", parserFeatures()),
        section("JsonGenerator.Feature(실제 생성기)", generatorFeatures()),
        section("JsonFactory.Feature", states(enumValues<JsonFactory.Feature>()) { mapper.factory.isEnabled(it) }),
        section("DatatypeFeature", datatypeFeatures()),
        section("강제 변환(LogicalType × 입력 형태)", coercions()),
        section("스트림 제약", streamConstraints())
    ).joinToString("\n")

    private fun versions(): List<String> = listOf(
        "jackson-core ${mapper.factory.version()}",
        "jackson-databind ${mapper.version()}",
        "jackson-module-kotlin ${KotlinModule.Builder().build().version()}",
        "jackson-datatype-jsr310 ${JavaTimeModule().version()}",
        "jackson-datatype-jdk8 ${Jdk8Module().version()}",
        "jackson-module-parameter-names ${ParameterNamesModule().version()}"
    )

    private fun settings(): List<String> = listOf(
        "default-property-inclusion=${serialization.defaultPropertyInclusion}",
        "property-naming-strategy=${serialization.propertyNamingStrategy?.javaClass?.name ?: "null"}",
        "date-format=${serialization.dateFormat?.javaClass?.name ?: "null"}",
        "time-zone=${serialization.timeZone.id}",
        "explicit-time-zone=${serialization.hasExplicitTimeZone()}",
        "serialization-visibility=${serialization.defaultVisibilityChecker}",
        "deserialization-visibility=${deserialization.defaultVisibilityChecker}",
        "default-setter-info=${deserialization.defaultSetterInfo}",
        "default-mergeable=${deserialization.defaultMergeable}",
        "annotation-introspectors=${serialization.annotationIntrospector.allIntrospectors().map { it.javaClass.name }}"
    )

    private fun deserializationFeatures(): List<String> =
        states(enumValues<DeserializationFeature>()) { deserialization.isEnabled(it) }

    private fun parserFeatures(): List<String> = mapper.createParser("{}").use { parser ->
        states(enumValues<JsonParser.Feature>()) { parser.isEnabled(it) }
    }

    private fun generatorFeatures(): List<String> = mapper.createGenerator(StringWriter()).use { generator ->
        states(enumValues<JsonGenerator.Feature>()) { generator.isEnabled(it) }
    }

    private fun datatypeFeatures(): List<String> {
        val features: List<DatatypeFeature> = enumValues<EnumFeature>().toList() + enumValues<JsonNodeFeature>()
        return features.map { "${it.javaClass.simpleName}.$it=${serialization.isEnabled(it)}" }
    }

    private fun coercions(): List<String> = enumValues<LogicalType>().map { type ->
        val actions = enumValues<CoercionInputShape>().map { "${it.name}=${coercion(type, it)}" }
        "${type.name}: ${actions.joinToString(" ")}"
    }

    private fun coercion(type: LogicalType, shape: CoercionInputShape): String =
        deserialization.findCoercionAction(type, null, shape).name

    private fun streamConstraints(): List<String> {
        val read = mapper.factory.streamReadConstraints()
        val write = mapper.factory.streamWriteConstraints()
        return listOf(
            "read.max-nesting-depth=${read.maxNestingDepth}",
            "read.max-document-length=${read.maxDocumentLength}",
            "read.max-token-count=${read.maxTokenCount}",
            "read.max-number-length=${read.maxNumberLength}",
            "read.max-string-length=${read.maxStringLength}",
            "read.max-name-length=${read.maxNameLength}",
            "write.max-nesting-depth=${write.maxNestingDepth}"
        )
    }

    private fun <T : Enum<T>> states(features: Array<T>, enabled: (T) -> Boolean): List<String> =
        features.map { "${it.name}=${enabled(it)}" }

    private fun moduleId(id: Any): String = id.toString().replace(GENERATED_MODULE_NAME, "SimpleModule-(자동 번호)")

    private fun section(title: String, lines: List<String>): String = DumpSections.render(title, lines)

    private companion object {
        val GENERATED_MODULE_NAME = Regex("^SimpleModule-\\d+$")
    }
}
