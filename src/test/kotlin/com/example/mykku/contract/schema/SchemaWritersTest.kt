package com.example.mykku.contract.schema

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.validation.GroupSequence
import jakarta.validation.Valid
import jakarta.validation.Validation
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import jakarta.validation.groups.ConvertGroup
import jakarta.validation.groups.Default
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import java.time.LocalDateTime
import kotlin.reflect.typeOf

@DisplayName("스키마 덤프 작성기")
class SchemaWritersTest {

    private val mapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `자기 자신을 담는 응답 타입은 조상 경로를 가리키고 멈춘다`() {
        val lines = writer().lines(mapper.constructType(TreeProbe::class.java), typeOf<TreeProbe>())

        assertThat(lines).containsExactly(
            "$ object non-null",
            "$.name string non-null",
            "$.children array non-null",
            "$.children[] recursive=$",
            "$.parent recursive=$"
        )
    }

    @Test
    fun `Page는 PageImpl의 직렬화기로 보고 프로퍼티를 이름순으로 적으며 Java 타입의 nullable 열은 비운다`() {
        val pageMapper = jacksonObjectMapper().addMixIn(PageImpl::class.java, PageImplWithoutPageable::class.java)
        val type = pageMapper.typeFactory.constructType(object : TypeReference<HolderProbe<Page<String>>>() {})
        val lines = ResponseSchemaWriter(pageMapper).lines(type, typeOf<HolderProbe<Page<String>>>())
        val pageProperties = lines.filter { it.matches(PAGE_PROPERTY) }.map { it.split(' ')[0] }

        assertThat(lines).contains("$.value object non-null ${ResponseSchemaWriter.ORDER_UNFIXED}")
        assertThat(lines).contains("$.value.content array")
        assertThat(pageProperties).isSorted().doesNotContain("$.value.pageable")
        assertThat(lines).contains("$.optional string nullable")
    }

    @Test
    fun `Nothing 타입 인자는 null로, 시각은 Java 타입과 함께, 프로퍼티 포함 규칙과 형식은 기본값이 아닐 때 적는다`() {
        val nothing = writer().lines(mapper.constructType(HolderProbe::class.java), typeOf<HolderProbe<Nothing?>>())
        val traits = writer().lines(mapper.constructType(TraitsProbe::class.java), typeOf<TraitsProbe>())

        assertThat(nothing).contains("$.value null nullable")
        assertThat(traits[1]).isEqualTo("$.at string non-null format=date-time java=LocalDateTime")
        assertThat(traits[2]).startsWith("$.pattern string non-null format=date-time java=LocalDateTime jsonformat=")
        assertThat(traits[3]).isEqualTo("$.hidden string nullable include=NON_NULL/ALWAYS")
    }

    @Test
    fun `Valid가 닿지 않는 제약은 미적용으로 적고 cascade된 중첩 DTO의 제약만 적용으로 적는다`() {
        val block = RequestSchemaWriter(mapper, validator).render(target<OuterFormProbe>(validated = true))

        assertThat(block.lines()).containsSubsequence(
            "title kotlin=title type=String default=none jackson-required=true",
            "  @Size(max=2, message=\"길이\", min=0)",
            "inner.name kotlin=name type=String default=none jackson-required=true",
            "  미적용 @NotBlank(message=\"안쪽\")",
            "validInner kotlin=validInner type=object default=none jackson-required=true cascade",
            "validInner.name kotlin=name type=String default=none jackson-required=true",
            "  @NotBlank(message=\"안쪽\")"
        )
    }

    @Test
    fun `핸들러 인자에 Valid가 없으면 모든 제약을 미적용으로 적는다`() {
        val constraints = RequestSchemaWriter(mapper, validator).render(target<OuterFormProbe>(validated = false))
            .lines()
            .filter { it.startsWith("  ") }

        assertThat(constraints).hasSize(3).allMatch { it.startsWith("  미적용 @") }
    }

    @Test
    fun `기본값은 실제 값으로, 제네릭 필드는 치환해, 루트 목록은 원소로 내려가 적는다`() {
        val writer = RequestSchemaWriter(mapper, validator)
        val defaults = writer.render(target<DefaultsProbe>(validated = true)).lines()
        val generic = writer.render(target<GenericProbe<InnerFormProbe>>(validated = true)).lines()
        val list = writer.render(target<List<InnerFormProbe>>(validated = true)).lines()

        assertThat(defaults).anyMatch { it.startsWith("flag kotlin=flag type=Boolean default=true ") }
            .anyMatch { it.startsWith("label kotlin=label type=String? default=\"기본\" ") }
            .anyMatch { it.startsWith("extra (생성자 밖 프로퍼티: setter type=String)") }
        assertThat(generic).anyMatch { it.startsWith("item.name kotlin=name type=String") }
        assertThat(list).startsWith("## POST /probe body:json", "$ type=List<object>")
            .anyMatch { it.startsWith("$[].name kotlin=name type=String") }
    }

    @Test
    fun `Jackson이 소문자 enum을 받으면 요청 필드에 표시하고 값 클래스는 내부 값 타입으로 적는다`() {
        val lenient = jacksonMapperBuilder().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS).build()
        val lines = RequestSchemaWriter(lenient, validator).render(target<EnumAndValueProbe>(validated = true)).lines()
        val level = lines.single { it.startsWith("level ") }

        assertThat(level).contains("type=enum[LOW,HIGH] default=none", LOWERCASE_ACCEPTED)
        assertThat(lines).anyMatch { it.startsWith("id kotlin=id type=Long default=none") }
    }

    @Test
    fun `클래스 수준 @JsonInclude도 실제 직렬화기처럼 프로퍼티 포함 규칙에 넣는다`() {
        val lines = writer().lines(mapper.constructType(ClassIncludeProbe::class.java), typeOf<ClassIncludeProbe>())

        assertThat(lines.drop(1)).allMatch { it.endsWith(" include=NON_NULL/ALWAYS") }.hasSize(2)
    }

    @Test
    fun `검증 그룹을 지정하면 그 그룹에 속하지 않는 제약은 미적용으로 적는다`() {
        val target = RequestTarget("POST /probe body:json", typeOf<GroupedProbe>(), RequestBinding.JSON, true, GROUPS)
        val constraints = RequestSchemaWriter(mapper, validator).render(target).lines().filter { it.startsWith("  ") }

        assertThat(constraints).containsExactly(
            "  미적용 @NotBlank(message=\"기본\")",
            "  @NotBlank(groups=[GroupProbe], message=\"그룹\")"
        )
    }

    @Test
    fun `생성자 밖 프로퍼티는 Jackson이 채우는 필드와 폼이 쓰는 public setter만 적는다`() {
        val writer = RequestSchemaWriter(mapper, validator)
        val json = writer.render(target<FieldPropertyProbe>(validated = true)).lines()
        val formTarget = RequestTarget("POST /probe model:form", typeOf<FormSetterProbe>(), RequestBinding.FORM, true)
        val form = writer.render(formTarget).lines()

        assertThat(json).contains("normalized (생성자 밖 프로퍼티: field type=String)")
        assertThat(form).contains("editable (생성자 밖 프로퍼티: setter type=String)").noneMatch { it.startsWith("locked") }
    }

    @Test
    fun `기본값을 계산하지 못하면 조용히 넘기지 않고 실패한다`() {
        assertThatThrownBy { RequestSchemaWriter(mapper, validator).render(target<InitGuardProbe>(validated = true)) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `타입별 포함 규칙 재정의는 직렬화기처럼 선언 타입과 접근자 타입 기준으로 적는다`() {
        val overriding = jacksonObjectMapper()
        overriding.configOverride(List::class.java).setIncludeAsProperty(NON_EMPTY)
        val holder = overriding.typeFactory.constructType(object : TypeReference<HolderProbe<List<String>>>() {})
        val generic = ResponseSchemaWriter(overriding).lines(holder, typeOf<HolderProbe<List<String>>>())
        val listType = overriding.constructType(ListProbe::class.java)
        val declared = ResponseSchemaWriter(overriding).lines(listType, typeOf<ListProbe>())

        assertThat(overriding.writeValueAsString(HolderProbe(emptyList<String>(), "x"))).contains("\"value\":[]")
        assertThat(overriding.writeValueAsString(ListProbe(emptyList()))).isEqualTo("{}")
        assertThat(generic.single { it.startsWith("$.value ") }).doesNotContain("include=")
        assertThat(declared.single { it.startsWith("$.tags ") }).contains("include=NON_EMPTY/")
    }

    @Test
    fun `생성자 밖 프로퍼티는 실제 역직렬화기처럼 무시 목록과 읽기 전용을 빼고 getter-as-setter를 적는다`() {
        val lines = RequestSchemaWriter(mapper, validator).render(target<JacksonIgnoralProbe>(validated = true)).lines()

        assertThat(lines).contains("tags (생성자 밖 프로퍼티: getter-as-setter type=List)")
            .noneMatch { it.startsWith("normalized") || it.startsWith("readOnlyTags") }
    }

    @Test
    fun `nullable 필수 인자는 더미 값으로 먼저 만들고 실패하면 null로 채워 나머지 기본값을 계산한다`() {
        val writer = RequestSchemaWriter(mapper, validator)
        val nested = writer.render(target<NullableNestedProbe>(validated = true)).lines()
        val conditional = writer.render(target<ConditionalTokenProbe>(validated = true)).lines()

        assertThat(nested).anyMatch { it.startsWith("size kotlin=size type=Int default=20 ") }
        assertThat(conditional).anyMatch { it.startsWith("size kotlin=size type=Int default=20 ") }
    }

    @Test
    fun `검증 그룹 순서와 그룹 변환은 해석하지 않고 실패한다`() {
        val writer = RequestSchemaWriter(mapper, validator)
        val ordered = RequestTarget("POST /probe body:json", typeOf<GroupedProbe>(), RequestBinding.JSON, true, ORDERED)

        assertThatThrownBy { writer.render(ordered) }.hasMessageContaining("OrderedGroupProbe @GroupSequence")
        assertThatThrownBy { writer.render(target<ConvertGroupProbe>(validated = true)) }
            .hasMessageContaining("ConvertGroupProbe.inner @ConvertGroup")
    }

    @Test
    fun `상위 클래스의 그룹 순서와 인터페이스 getter의 그룹 변환도 실패로 잡고 빈 검증이 보지 않는 생성자 인자는 넘긴다`() {
        val writer = RequestSchemaWriter(mapper, validator)

        assertThatThrownBy { writer.render(target<SequencedChildProbe>(validated = true)) }
            .hasMessageContaining("SequencedBaseProbe @GroupSequence")
        assertThatThrownBy { writer.render(target<InterfaceConvertProbe>(validated = true)) }
            .hasMessageContaining("InterfaceConvertProbe.getInner @ConvertGroup")
        assertThatThrownBy { writer.render(target<GrandInterfaceConvertProbe>(validated = true)) }
            .hasMessageContaining("GrandInterfaceConvertProbe.getInner @ConvertGroup")
        assertThat(writer.render(target<ParameterConvertProbe>(validated = true)).lines())
            .contains("  미적용 @NotBlank(message=\"안쪽\")")
    }

    private fun writer(): ResponseSchemaWriter = ResponseSchemaWriter(mapper)

    private inline fun <reified T> target(validated: Boolean): RequestTarget =
        RequestTarget("POST /probe body:json", typeOf<T>(), RequestBinding.JSON, validated)

    private companion object {
        val GROUPS: List<Class<*>> = listOf(GroupProbe::class.java)
        val ORDERED: List<Class<*>> = listOf(OrderedGroupProbe::class.java)
        val NON_EMPTY: JsonInclude.Value = JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, null)
        const val LOWERCASE_ACCEPTED = "jackson-enum-lowercase=accepted"
        val PAGE_PROPERTY = Regex("""\$\.value\.[a-zA-Z]+ .*""")
    }
}

data class TreeProbe(val name: String, val children: List<TreeProbe>, val parent: TreeProbe?)

data class HolderProbe<T>(val value: T, val optional: String?)

@JsonIgnoreProperties("pageable")
abstract class PageImplWithoutPageable

data class TraitsProbe(
    val at: LocalDateTime,
    @get:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") val pattern: LocalDateTime,
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val hidden: String?
)

data class InnerFormProbe(@field:NotBlank(message = "안쪽") val name: String)

data class OuterFormProbe(
    @field:Size(max = 2, message = "길이") val title: String,
    val inner: InnerFormProbe,
    @field:Valid val validInner: InnerFormProbe
)

data class DefaultsProbe(val flag: Boolean = true, val label: String? = "기본") {
    var extra: String = ""
}

data class GenericProbe<T>(val item: T)

enum class LevelProbe { LOW, HIGH }

@JvmInline
value class IdProbe(val value: Long)

data class EnumAndValueProbe(val level: LevelProbe, val id: IdProbe)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ClassIncludeProbe(val hidden: String?, val shown: String)

interface GroupProbe

data class GroupedProbe(
    @field:NotBlank(message = "기본") val plain: String,
    @field:NotBlank(message = "그룹", groups = [GroupProbe::class]) val grouped: String
)

data class FieldPropertyProbe(val title: String) {
    val normalized: String = title.trim()
}

class FormSetterProbe(val title: String) {
    var editable: String = ""
    var locked: String = ""
        private set
}

data class InitGuardProbe(val name: String, val size: Int = 20) {
    init {
        require(name.length >= 2)
    }
}

data class ListProbe(val tags: List<String>)

@JsonIgnoreProperties("normalized")
data class JacksonIgnoralProbe(val title: String) {
    private val store = mutableListOf<String>()
    val normalized: String = title.trim()
    val tags: MutableList<String> get() = store

    @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    val readOnlyTags: MutableList<String> get() = store
}

data class GuardedFilterProbe(val keyword: String) {
    init {
        require(keyword.length >= 2)
    }
}

data class NullableNestedProbe(val filter: GuardedFilterProbe?, val size: Int = 20)

data class ConditionalTokenProbe(val token: String?, val size: Int = 20) {
    init {
        require(!token.isNullOrBlank())
    }
}

@GroupSequence(Default::class, GroupProbe::class)
interface OrderedGroupProbe

@GroupSequence(SequencedBaseProbe::class, GroupProbe::class)
open class SequencedBaseProbe

data class SequencedChildProbe(val name: String) : SequencedBaseProbe()

interface ConvertingInnerProbe {
    @get:Valid
    @get:ConvertGroup(to = GroupProbe::class)
    val inner: InnerFormProbe
}

data class InterfaceConvertProbe(override val inner: InnerFormProbe) : ConvertingInnerProbe

interface ChildConvertingInnerProbe : ConvertingInnerProbe

data class GrandInterfaceConvertProbe(override val inner: InnerFormProbe) : ChildConvertingInnerProbe

data class ParameterConvertProbe(@Valid @ConvertGroup(to = GroupProbe::class) val inner: InnerFormProbe)

data class ConvertGroupProbe(@field:Valid @field:ConvertGroup(to = GroupProbe::class) val inner: InnerFormProbe)
