package com.example.mykku.event.domain.vo

import com.example.mykku.event.exception.EventErrorCode
import com.example.mykku.event.exception.EventException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("GoogleFormUrl 값 객체 테스트")
class GoogleFormUrlTest {

    @Nested
    @DisplayName("normalize 메서드")
    inner class Normalize {

        @ParameterizedTest(name = "[{0}]")
        @ValueSource(strings = ["", "   ", " \t\n "])
        @DisplayName("빈 값이나 공백만 있으면 미입력(null)으로 본다")
        fun `정규화 - 빈 값은 null`(raw: String) {
            assertThat(GoogleFormUrl.normalize(raw)).isNull()
        }

        @Test
        @DisplayName("null은 null로 둔다")
        fun `정규화 - null`() {
            assertThat(GoogleFormUrl.normalize(null)).isNull()
        }

        @Test
        @DisplayName("앞뒤 공백과 줄바꿈을 잘라 낸다")
        fun `정규화 - 앞뒤 공백 제거`() {
            assertThat(GoogleFormUrl.normalize("  https://forms.gle/abc \n")).isEqualTo("https://forms.gle/abc")
        }
    }

    @Nested
    @DisplayName("of 메서드")
    inner class Of {

        @ParameterizedTest(name = "{0}")
        @ValueSource(
            strings = [
                "https://docs.google.com/forms/d/e/1FAIpQLSc_example/viewform",
                "https://docs.google.com/forms/d/1AbC_example/viewform",
                "https://docs.google.com/forms/d/e/x/viewform?usp=pp_url&entry.1=a",
                "https://docs.google.com/forms/d/e/x/viewform?usp=sf_link",
                "https://docs.google.com/forms/d/e/x/viewform/",
                "https://DOCS.Google.com/forms/d/e/x/viewform",
                "https://forms.gle/abc",
                "HTTPS://forms.gle/abc"
            ]
        )
        @DisplayName("구글폼 응답 링크는 그대로 받는다")
        fun `허용 - 응답 링크`(url: String) {
            assertThat(GoogleFormUrl.of(url).value).isEqualTo(url)
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(
            strings = [
                "http://docs.google.com/forms/d/e/x/viewform",
                "http://forms.gle/abc",
                "javascript:alert(1)",
                "data:text/html,hello",
                "https://docs.google.com/document/d/x/edit",
                "https://docs.google.com/forms/d/x/edit",
                "https://docs.google.com/forms/d/x/edit/",
                "https://docs.google.com/forms/d/e/x/formResponse",
                "https://docs.google.com/forms/",
                "https://docs.google.com/forms",
                "https://docs.google.com/forms/../document/d/x/viewform",
                "https://forms.gle/",
                "https://forms.gle",
                "https://x@docs.google.com/forms/d/e/x/viewform",
                "https://docs.google.com:8443/forms/d/e/x/viewform",
                "https://docs.google.com:443/forms/d/e/x/viewform",
                "https://docs.google.com.example.com/forms/d/e/x/viewform",
                "https://example.com/forms/d/e/x/viewform",
                "https://forms.gle/a b",
                "forms.gle/abc",
                " https://forms.gle/abc"
            ]
        )
        @DisplayName("https가 아니거나 허용 호스트의 응답 링크가 아니면 EV109")
        fun `거부 - 응답 링크가 아님`(url: String) {
            assertInvalid(url)
        }

        @Test
        @DisplayName("500자는 받고 501자는 EV109")
        fun `길이 - 500자 경계`() {
            val url = formsGleUrlOfLength(GoogleFormUrl.MAX_LENGTH)

            assertThat(GoogleFormUrl.of(url).value).hasSize(GoogleFormUrl.MAX_LENGTH)
            assertInvalid(url + "a")
        }

        @Test
        @DisplayName("원문은 500자를 넘어도 앞뒤 공백을 잘라 낸 값이 500자 이하면 받는다")
        fun `길이 - 정규화 뒤 500자`() {
            val url = formsGleUrlOfLength(GoogleFormUrl.MAX_LENGTH)

            val normalized = GoogleFormUrl.normalize("   $url   ")

            assertThat(GoogleFormUrl.of(normalized!!).value).isEqualTo(url)
        }

        @Test
        @DisplayName("앞뒤 공백을 잘라 낸 값이 501자면 EV109")
        fun `길이 - 정규화 뒤 501자`() {
            val normalized = GoogleFormUrl.normalize("  " + formsGleUrlOfLength(GoogleFormUrl.MAX_LENGTH + 1) + "  ")

            assertInvalid(normalized!!)
        }

        private fun formsGleUrlOfLength(length: Int): String {
            val prefix = "https://forms.gle/"
            return prefix + "a".repeat(length - prefix.length)
        }

        private fun assertInvalid(url: String) {
            val exception = assertThrows<EventException> { GoogleFormUrl.of(url) }

            assertThat(exception.errorCode).isEqualTo(EventErrorCode.INVALID_EVENT_TYPE_SETTING)
        }
    }
}
