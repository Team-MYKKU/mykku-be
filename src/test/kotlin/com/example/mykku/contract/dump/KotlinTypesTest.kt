package com.example.mykku.contract.dump

import com.example.mykku.contract.jackson.ProbeLevel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.web.multipart.MultipartFile
import kotlin.reflect.typeOf

@DisplayName("정적 덤프 타입·값 표기")
class KotlinTypesTest {

    @Test
    fun `Kotlin 타입은 기본 타입 이름과 nullable, enum 상수, 파일, 프로젝트 DTO로 적는다`() {
        assertThat(KotlinTypes.render(typeOf<List<String?>?>())).isEqualTo("List<String?>?")
        assertThat(KotlinTypes.render(typeOf<ProbeLevel>())).isEqualTo("enum[LOW,HIGH]")
        assertThat(KotlinTypes.render(typeOf<List<MultipartFile>>())).isEqualTo("List<file>")
        assertThat(KotlinTypes.render(typeOf<Map<String, KotlinTypesTest>>())).isEqualTo("Map<String, object>")
        assertThat(KotlinTypes.render(typeOf<List<ValueProbe>?>())).isEqualTo("List<String>?")
    }

    @Test
    fun `순서가 보장되지 않는 Set·Map은 정렬하고 문자열은 따옴표로 감싸 이스케이프한다`() {
        assertThat(AttributeValues.value(linkedSetOf("b", "a"))).isEqualTo("[\"a\", \"b\"]")
        assertThat(AttributeValues.value(linkedMapOf("z" to 1, "a" to 2))).isEqualTo("{\"a\"=2, \"z\"=1}")
        assertThat(AttributeValues.value("a\"b\\c\nd")).isEqualTo("\"a\\\"b\\\\c\\nd\"")
        assertThat(AttributeValues.value(arrayOf(1, 2))).isEqualTo("[1, 2]")
        assertThat(AttributeValues.value(intArrayOf(1, 3))).isEqualTo("[1, 3]")
    }
}

@JvmInline
value class ValueProbe(val value: String)
