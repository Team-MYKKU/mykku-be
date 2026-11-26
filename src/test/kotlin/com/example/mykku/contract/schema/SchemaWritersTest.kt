package com.example.mykku.contract.schema

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.validation.Valid
import jakarta.validation.Validation
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import kotlin.reflect.typeOf

@DisplayName("스키마 덤프 작성기")
class SchemaWritersTest {

    private val mapper: ObjectMapper = jacksonObjectMapper()

    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `자기 자신을 담는 응답 타입은 조상 경로를 가리키고 멈춘다`() {
        val lines = ResponseSchemaWriter(mapper).lines(mapper.constructType(TreeProbe::class.java), typeOf<TreeProbe>())

        assertThat(lines).containsExactly(
            "$ object non-null",
            "$.name string non-null",
            "$.children array non-null",
            "$.children[] recursive=$",
            "$.parent recursive=$"
        )
    }

    @Test
    fun `Page는 PageImpl로 보고 프로퍼티를 이름순으로 적으며 Java 타입의 nullable 열은 비운다`() {
        val type = mapper.typeFactory.constructType(object : TypeReference<HolderProbe<Page<String>>>() {})
        val lines = ResponseSchemaWriter(mapper).lines(type, typeOf<HolderProbe<Page<String>>>())
        val pageProperties = lines.filter { it.matches(Regex("""\$\.value\.[a-zA-Z]+ .*""")) }.map { it.split(' ')[0] }

        assertThat(lines).contains("$.value object non-null ${ResponseSchemaWriter.ORDER_UNFIXED}")
        assertThat(lines).contains("$.value.content array")
        assertThat(pageProperties).isSorted()
        assertThat(lines).contains("$.optional string nullable")
    }

    @Test
    fun `Nothing 타입 인자는 null로 적는다`() {
        val type = mapper.constructType(HolderProbe::class.java)
        val lines = ResponseSchemaWriter(mapper).lines(type, typeOf<HolderProbe<Nothing?>>())

        assertThat(lines).contains("$.value null nullable")
    }

    @Test
    fun `Valid가 닿지 않는 제약은 미적용으로 적고 cascade된 중첩 DTO의 제약만 적용으로 적는다`() {
        val block = RequestSchemaWriter(mapper, validator).render(target(validated = true))

        assertThat(block.lines()).containsSubsequence(
            "title kotlin=title type=String default=no jackson-required=true",
            "  @Size(max=2, message=\"길이\", min=0)",
            "inner.name kotlin=name type=String default=no jackson-required=true",
            "  미적용 @NotBlank(message=\"안쪽\")",
            "validInner kotlin=validInner type=object default=no jackson-required=true cascade",
            "validInner.name kotlin=name type=String default=no jackson-required=true",
            "  @NotBlank(message=\"안쪽\")"
        )
    }

    @Test
    fun `핸들러 인자에 Valid가 없으면 모든 제약을 미적용으로 적는다`() {
        val constraints = RequestSchemaWriter(mapper, validator).render(target(validated = false))
            .lines()
            .filter { it.startsWith("  ") }

        assertThat(constraints).hasSize(3).allMatch { it.startsWith("  미적용 @") }
    }

    private fun target(validated: Boolean): RequestTarget =
        RequestTarget("POST /probe body:json", typeOf<OuterFormProbe>(), RequestBinding.JSON, validated)
}

data class TreeProbe(val name: String, val children: List<TreeProbe>, val parent: TreeProbe?)

data class HolderProbe<T>(val value: T, val optional: String?)

data class InnerFormProbe(@field:NotBlank(message = "안쪽") val name: String)

data class OuterFormProbe(
    @field:Size(max = 2, message = "길이") val title: String,
    val inner: InnerFormProbe,
    @field:Valid val validInner: InnerFormProbe
)
