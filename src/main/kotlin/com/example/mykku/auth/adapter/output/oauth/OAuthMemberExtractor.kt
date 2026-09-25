package com.example.mykku.auth.adapter.output.oauth

import com.example.mykku.auth.adapter.output.oauth.dto.AppleUserInfo
import com.example.mykku.auth.adapter.output.oauth.dto.GoogleUserInfo
import com.example.mykku.auth.adapter.output.oauth.dto.KakaoUserInfo
import com.example.mykku.auth.adapter.output.oauth.dto.NaverUserInfo
import com.example.mykku.auth.application.dto.OAuthMemberInfo
import com.example.mykku.member.domain.vo.SocialProvider
import org.springframework.stereotype.Component

@Component
class OAuthMemberExtractor {

    fun extractFromGoogle(userInfo: GoogleUserInfo): OAuthMemberInfo {
        return OAuthMemberInfo(
            profileImage = userInfo.picture ?: "",
            provider = SocialProvider.GOOGLE,
            socialId = userInfo.id,
            email = userInfo.email
        )
    }

    fun extractFromKakao(userInfo: KakaoUserInfo): OAuthMemberInfo {
        return OAuthMemberInfo(
            profileImage = userInfo.properties?.profileImage
                ?: userInfo.kakaoAccount?.profile?.profileImageUrl
                ?: "",
            provider = SocialProvider.KAKAO,
            socialId = userInfo.id.toString(),
            email = userInfo.kakaoAccount?.email ?: fallbackEmail(SocialProvider.KAKAO, userInfo.id.toString())
        )
    }

    fun extractFromApple(userInfo: AppleUserInfo): OAuthMemberInfo {
        return OAuthMemberInfo(
            profileImage = "",
            provider = SocialProvider.APPLE,
            socialId = userInfo.sub,
            email = userInfo.email ?: fallbackEmail(SocialProvider.APPLE, userInfo.sub)
        )
    }

    fun extractFromNaver(userInfo: NaverUserInfo): OAuthMemberInfo {
        return OAuthMemberInfo(
            profileImage = userInfo.response.profileImage ?: "",
            provider = SocialProvider.NAVER,
            socialId = userInfo.response.id,
            email = userInfo.response.email ?: fallbackEmail(SocialProvider.NAVER, userInfo.response.id)
        )
    }

    private fun fallbackEmail(provider: SocialProvider, socialId: String): String {
        return requireNotNull(provider.placeholderEmail(socialId))
    }
}
