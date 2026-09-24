package com.example.mykku.member.domain.vo

enum class SocialProvider(val label: String, private val placeholderEmailDomain: String?) {
    GOOGLE("구글", null),
    KAKAO("카카오", "kakao.com"),
    NAVER("네이버", "naver.com"),
    APPLE("애플", "privaterelay.appleid.com"),
    EMAIL("이메일", null);

    fun placeholderEmail(socialId: String): String? {
        return placeholderEmailDomain?.let { "${name.lowercase()}_$socialId@$it" }
    }
}
