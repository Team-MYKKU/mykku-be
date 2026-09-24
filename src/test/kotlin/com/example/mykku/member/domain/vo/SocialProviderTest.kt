package com.example.mykku.member.domain.vo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("SocialProvider 테스트")
class SocialProviderTest {

    @Test
    @DisplayName("이메일을 주지 않는 제공처는 소셜 ID로 대체 이메일을 만든다")
    fun `대체 이메일 형식`() {
        assertThat(SocialProvider.KAKAO.placeholderEmail("1001")).isEqualTo("kakao_1001@kakao.com")
        assertThat(SocialProvider.NAVER.placeholderEmail("abc")).isEqualTo("naver_abc@naver.com")
        assertThat(SocialProvider.APPLE.placeholderEmail("sub1")).isEqualTo("apple_sub1@privaterelay.appleid.com")
    }

    @Test
    @DisplayName("구글과 이메일 가입은 대체 이메일이 없다")
    fun `대체 이메일 없음`() {
        assertThat(SocialProvider.GOOGLE.placeholderEmail("g1")).isNull()
        assertThat(SocialProvider.EMAIL.placeholderEmail("e1")).isNull()
    }
}
