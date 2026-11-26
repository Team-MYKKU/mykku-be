package com.example.mykku.contract.support

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets.UTF_8

object ContractJson {

    private const val EXCERPT_RADIUS = 40

    private val reader: JsonMapper = JsonMapper.builder()
        .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
        .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        .configure(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES, false)
        .build()

    private val compactWriter: ObjectWriter = JsonMapper.builder().build().writer()

    private val prettyWriter: ObjectWriter = JsonMapper.builder().build()
        .writer(DefaultPrettyPrinter().withObjectIndenter(DefaultIndenter("  ", "\n")))

    fun render(body: ByteArray, rules: JsonRules = JsonRules()): String {
        val text = decodeUtf8(body)
        val tree = parse(text)
        requireCanonical(text, tree)
        rules.applyTo(tree)
        return pretty(tree)
    }

    fun parse(text: String): JsonNode = try {
        reader.readTree(text)
    } catch (exception: JsonProcessingException) {
        throw ContractViolationError("[API 계약 위반] JSON으로 읽을 수 없습니다(중복 키 포함): ${exception.message}", exception)
    }

    fun pretty(tree: JsonNode): String = prettyWriter.writeValueAsString(tree)

    fun decodeUtf8(bytes: ByteArray): String = try {
        UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (exception: CharacterCodingException) {
        throw ContractViolationError("[API 계약 위반] 응답 본문이 UTF-8이 아닙니다: $exception", exception)
    }

    private fun requireCanonical(text: String, tree: JsonNode) {
        val canonical = compactWriter.writeValueAsString(tree)
        if (canonical == text) {
            return
        }
        val index = text.zip(canonical).indexOfFirst { it.first != it.second }
            .takeIf { it >= 0 } ?: minOf(text.length, canonical.length)
        throw ContractViolationError(
            "[API 계약 위반] 응답 원문이 compact 정규형과 다릅니다(공백·이스케이프·숫자 표기). " +
                "첫 차이 위치 $index: 원문 '${excerpt(text, index)}' / 정규형 '${excerpt(canonical, index)}'"
        )
    }

    private fun excerpt(text: String, index: Int): String =
        text.substring(maxOf(0, index - EXCERPT_RADIUS), minOf(text.length, index + EXCERPT_RADIUS))
}
