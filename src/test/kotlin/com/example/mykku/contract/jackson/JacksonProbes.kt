package com.example.mykku.contract.jackson

import com.example.mykku.common.dto.ApiResponse
import com.example.mykku.contract.support.ContractJson
import com.example.mykku.contract.support.JsonRules
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.core.ResolvableType
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.mock.http.MockHttpInputMessage
import org.springframework.mock.http.MockHttpOutputMessage
import java.lang.reflect.Type
import java.math.BigDecimal
import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDate
import java.time.LocalDateTime

class SerializationProbe(
    val name: String,
    private val value: Any,
    private val type: Type,
    private val rules: JsonRules = JsonRules()
) {
    fun serialize(converter: MappingJackson2HttpMessageConverter): JsonNode {
        val message = MockHttpOutputMessage()
        converter.write(value, type, MediaType.APPLICATION_JSON, message)
        return ContractJson.parse(ContractJson.render(message.bodyAsBytes, rules))
    }
}

class DeserializationProbe(private val name: String, private val json: String, private val type: Class<*>) {

    fun describe(converter: MappingJackson2HttpMessageConverter): String = "$name: $json -> ${result(converter)}"

    private fun result(converter: MappingJackson2HttpMessageConverter): String = try {
        "값 ${converter.read(type, null, input())}"
    } catch (exception: HttpMessageNotReadableException) {
        "오류 ${generateSequence<Throwable>(exception) { it.cause }.joinToString(" <- ") { it.javaClass.simpleName }}"
    }

    private fun input(): MockHttpInputMessage = MockHttpInputMessage(json.toByteArray(UTF_8)).apply {
        headers.contentType = MediaType.APPLICATION_JSON
    }
}

object JacksonProbes {

    private val MOMENT: LocalDateTime = LocalDateTime.of(2001, 2, 3, 4, 5, 6)

    fun serialization(): List<SerializationProbe> = listOf(
        SerializationProbe("booleanNames", BooleanNamesProbe(true, null, "isbn", false), BooleanNamesProbe::class.java),
        SerializationProbe("nulls", NullsProbe(null, null, null, null), NullsProbe::class.java),
        SerializationProbe("empties", EmptiesProbe(emptyList(), emptyMap(), "", emptyList()), EmptiesProbe::class.java),
        SerializationProbe("dateTimes", dateTimes(), DateTimesProbe::class.java),
        SerializationProbe("enums", EnumsProbe(ProbeStatus.SECOND, ProbeStatus.entries, null), EnumsProbe::class.java),
        SerializationProbe("numbers", numbers(), NumbersProbe::class.java),
        SerializationProbe("apiResponseUnit", ApiResponse("메시지", Unit), apiResponseOf(Unit::class.java)),
        SerializationProbe("apiResponseNull", ApiResponse("메시지", null), apiResponseOf(Void::class.java)),
        SerializationProbe("pageImpl", pageImpl(), pageImplType(), JsonRules.pageImpl(base = ""))
    )

    fun deserialization(): List<DeserializationProbe> = nullAndDefaults() + numberCoercions() + textFormats() + syntax()

    private fun nullAndDefaults(): List<DeserializationProbe> = listOf(
        DeserializationProbe("null → String", """{"value":null}""", TextProbe::class.java),
        DeserializationProbe("누락 → String", """{}""", TextProbe::class.java),
        DeserializationProbe("null → String?", """{"value":null}""", NullableTextProbe::class.java),
        DeserializationProbe("누락 → String?", """{}""", NullableTextProbe::class.java),
        DeserializationProbe("누락 → 기본값 String", """{}""", DefaultTextProbe::class.java),
        DeserializationProbe("null → 기본값 String", """{"value":null}""", DefaultTextProbe::class.java),
        DeserializationProbe("is 접두 이름 → Boolean", """{"isActive":true}""", BooleanNameProbe::class.java),
        DeserializationProbe("접두 없는 이름 → Boolean", """{"active":true}""", BooleanNameProbe::class.java)
    )

    private fun numberCoercions(): List<DeserializationProbe> = listOf(
        DeserializationProbe("누락 → Long", """{}""", LongProbe::class.java),
        DeserializationProbe("null → Long", """{"value":null}""", LongProbe::class.java),
        DeserializationProbe("1.5 → Long", """{"value":1.5}""", LongProbe::class.java),
        DeserializationProbe("1.0 → Long", """{"value":1.0}""", LongProbe::class.java),
        DeserializationProbe("문자열 7 → Long", """{"value":"7"}""", LongProbe::class.java),
        DeserializationProbe("빈 문자열 → Long?", """{"value":""}""", NullableLongProbe::class.java),
        DeserializationProbe("빈 문자열 → Long", """{"value":""}""", LongProbe::class.java),
        DeserializationProbe("Int 범위 초과 → Int", """{"value":2147483648}""", IntProbe::class.java),
        DeserializationProbe("숫자 1 → String", """{"value":1}""", TextProbe::class.java)
    )

    private fun textFormats(): List<DeserializationProbe> = listOf(
        DeserializationProbe("상수 이름 enum", """{"value":"HIGH"}""", EnumProbe::class.java),
        DeserializationProbe("소문자 enum", """{"value":"high"}""", EnumProbe::class.java),
        DeserializationProbe("숫자 enum", """{"value":1}""", EnumProbe::class.java),
        DeserializationProbe("toString 값 enum", """{"value":"둘째"}""", LabeledEnumProbe::class.java),
        DeserializationProbe("Z 붙은 시각", """{"value":"2001-02-03T04:05:06Z"}""", DateTimeProbe::class.java),
        DeserializationProbe("오프셋 붙은 시각", """{"value":"2001-02-03T04:05:06+09:00"}""", DateTimeProbe::class.java),
        DeserializationProbe("공백 구분 시각", """{"value":"2001-02-03 04:05:06"}""", DateTimeProbe::class.java),
        DeserializationProbe("초 없는 시각", """{"value":"2001-02-03T04:05"}""", DateTimeProbe::class.java),
        DeserializationProbe("단일 값 → 배열", """{"values":"a"}""", ListProbe::class.java)
    )

    private fun syntax(): List<DeserializationProbe> = listOf(
        DeserializationProbe("모르는 필드", """{"value":"a","unknown":1}""", TextProbe::class.java),
        DeserializationProbe("중복 키", """{"value":"a","value":"b"}""", TextProbe::class.java),
        DeserializationProbe("뒤따르는 토큰", """{"value":"a"} {"value":"b"}""", TextProbe::class.java),
        DeserializationProbe("주석", """{/* c */"value":"a"}""", TextProbe::class.java),
        DeserializationProbe("작은따옴표", """{'value':'a'}""", TextProbe::class.java)
    )

    private fun dateTimes(): DateTimesProbe = DateTimesProbe(
        fraction0 = MOMENT,
        fraction2 = MOMENT.withNano(120_000_000),
        fraction6 = MOMENT.withNano(123_456_000),
        fraction9 = MOMENT.withNano(123_456_789),
        secondsZero = MOMENT.withSecond(0),
        date = LocalDate.of(2001, 2, 3)
    )

    private fun numbers(): NumbersProbe =
        NumbersProbe(Long.MAX_VALUE, Long.MIN_VALUE, Int.MAX_VALUE, 1.0, 0.1, BigDecimal("1.50"))

    private fun pageImpl(): PageImpl<PageItemProbe> = PageImpl(
        listOf(PageItemProbe(1, "첫째"), PageItemProbe(2, "둘째")),
        PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt")),
        5
    )

    private fun apiResponseOf(data: Class<*>): Type =
        ResolvableType.forClassWithGenerics(ApiResponse::class.java, data).type

    private fun pageImplType(): Type =
        ResolvableType.forClassWithGenerics(PageImpl::class.java, PageItemProbe::class.java).type
}
