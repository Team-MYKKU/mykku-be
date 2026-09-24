package com.example.mykku.auth.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.auth.adapter.input.web.dto.LogoutRequest
import com.example.mykku.auth.adapter.input.web.dto.MobileLoginRequest
import com.example.mykku.auth.adapter.input.web.dto.RefreshTokenRequest
import com.example.mykku.auth.application.dto.LoginResult
import com.example.mykku.auth.application.dto.MemberInfoResult
import com.example.mykku.auth.application.dto.RefreshTokenResult
import com.example.mykku.auth.exception.AuthErrorCode
import com.example.mykku.auth.exception.AuthException
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.docs.RestDocumentationResponse
import com.example.mykku.member.domain.vo.SocialProvider
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath

class AuthDocumentTest : BaseDocumentTest() {

    @Nested
    @DisplayName("모바일 소셜 로그인")
    inner class MobileLogin {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("provider").type(JsonFieldType.STRING)
                    .description("OAuth 제공자 (GOOGLE, KAKAO, APPLE, NAVER)"),
                fieldWithPath("accessToken").type(JsonFieldType.STRING)
                    .description("OAuth 제공자에서 받은 액세스 토큰"),
                fieldWithPath("idToken").type(JsonFieldType.STRING)
                    .description("Apple 로그인 시 필요한 ID 토큰 (선택사항)")
                    .optional()
            )
        )

        @Test
        fun `성공`() {
            val request = MobileLoginRequest(
                provider = SocialProvider.GOOGLE,
                accessToken = "google_access_token_example"
            )

            val loginResult = LoginResult(
                accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh...",
                tokenType = "Bearer",
                accessTokenExpiresIn = 86400000,
                refreshTokenExpiresIn = 1209600000,
                member = MemberInfoResult(
                    memberId = "google_123456789",
                    email = "user@gmail.com",
                    nickname = "홍길동",
                    profileImage = "https://lh3.googleusercontent.com/profile.jpg"
                ),
                isExistingUser = true,
                isProfileComplete = true
            )

            whenever(mobileLoginUseCase.login(any())).thenReturn(loginResult)

            val documentFilter = document("auth/mobile-login", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("로그인 응답 데이터"),
                            fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("JWT 액세스 토큰"),
                            fieldWithPath("data.tokenType").type(JsonFieldType.STRING).description("토큰 타입"),
                            fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("JWT 리프레시 토큰"),
                            fieldWithPath("data.accessTokenExpiresIn").type(JsonFieldType.NUMBER)
                                .description("액세스 토큰 만료 시간 (밀리초)"),
                            fieldWithPath("data.refreshTokenExpiresIn").type(JsonFieldType.NUMBER)
                                .description("리프레시 토큰 만료 시간 (밀리초)"),
                            fieldWithPath("data.member").type(JsonFieldType.OBJECT).description("회원 정보"),
                            fieldWithPath("data.member.memberId").type(JsonFieldType.STRING).description("회원 ID"),
                            fieldWithPath("data.member.email").type(JsonFieldType.STRING).description("회원 이메일"),
                            fieldWithPath("data.member.nickname").type(JsonFieldType.STRING).description("회원 닉네임"),
                            fieldWithPath("data.member.profileImage").type(JsonFieldType.STRING)
                                .description("프로필 이미지 URL").optional(),
                            fieldWithPath("data.isExistingUser").type(JsonFieldType.BOOLEAN)
                                .description("기존 가입자 여부 (true: 기존 가입자, false: 신규 가입자)"),
                            fieldWithPath("data.isProfileComplete").type(JsonFieldType.BOOLEAN)
                                .description("프로필 설정 완료 여부 (true: 완료, false: 미완료)")
                        )
                )
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/auth/mobile/login")
                .then()
                .statusCode(200)
        }

        @Test
        fun `유효하지 않은 토큰`() {
            val request = MobileLoginRequest(
                provider = SocialProvider.GOOGLE,
                accessToken = "invalid_access_token"
            )

            whenever(mobileLoginUseCase.login(any()))
                .thenThrow(AuthException(AuthErrorCode.OAUTH_INVALID_TOKEN))

            val documentFilter = document("auth/mobile-login", "OAUTH_INVALID_TOKEN")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/auth/mobile/login")
                .then()
                .statusCode(401)
        }

        @Test
        fun `사용자 정보 가져오기 실패`() {
            val request = MobileLoginRequest(
                provider = SocialProvider.GOOGLE,
                accessToken = "google_access_token_example"
            )

            whenever(mobileLoginUseCase.login(any()))
                .thenThrow(AuthException(AuthErrorCode.OAUTH_USER_INFO_FAILED))

            val documentFilter = document("auth/mobile-login", "OAUTH_USER_INFO_FAILED")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/auth/mobile/login")
                .then()
                .statusCode(400)
        }

        @Test
        fun `외부 서비스 오류`() {
            val request = MobileLoginRequest(
                provider = SocialProvider.GOOGLE,
                accessToken = "google_access_token_example"
            )

            whenever(mobileLoginUseCase.login(any()))
                .thenThrow(AuthException(AuthErrorCode.OAUTH_EXTERNAL_SERVICE_ERROR))

            val documentFilter = document("auth/mobile-login", "OAUTH_EXTERNAL_SERVICE_ERROR")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/auth/mobile/login")
                .then()
                .statusCode(503)
        }
    }

    @Nested
    @DisplayName("액세스 토큰 갱신")
    inner class RefreshToken {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("refreshToken").type(JsonFieldType.STRING)
                    .description("리프레시 토큰")
            )
        )

        @Test
        fun `성공`() {
            val request = RefreshTokenRequest(
                refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh.token.example"
            )

            val result = RefreshTokenResult(
                accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.new.access.token",
                refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.new.refresh.token",
                tokenType = "Bearer",
                expiresIn = 86400000,
                refreshTokenExpiresIn = 1209600000
            )

            whenever(refreshTokenUseCase.refresh(any())).thenReturn(result)

            val documentFilter = document("auth/refresh", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("토큰 갱신 응답 데이터"),
                            fieldWithPath("data.accessToken").type(JsonFieldType.STRING)
                                .description("새로운 JWT 액세스 토큰"),
                            fieldWithPath("data.refreshToken").type(JsonFieldType.STRING)
                                .description("새로 재발급된 리프레시 토큰 (클라이언트는 저장값을 갱신해야 함)"),
                            fieldWithPath("data.tokenType").type(JsonFieldType.STRING).description("토큰 타입"),
                            fieldWithPath("data.expiresIn").type(JsonFieldType.NUMBER)
                                .description("액세스 토큰 만료 시간 (밀리초)"),
                            fieldWithPath("data.refreshTokenExpiresIn").type(JsonFieldType.NUMBER)
                                .description("리프레시 토큰 만료 시간 (밀리초)")
                        )
                )
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(200)
        }

        @Test
        fun `유효하지 않은 리프레시 토큰`() {
            val request = RefreshTokenRequest(
                refreshToken = "invalid_refresh_token"
            )

            whenever(refreshTokenUseCase.refresh(any()))
                .thenThrow(AuthException(AuthErrorCode.INVALID_TOKEN))

            val documentFilter = document("auth/refresh", "INVALID_TOKEN")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(401)
        }
    }

    @Nested
    @DisplayName("로그아웃")
    inner class Logout {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("deviceId").type(JsonFieldType.STRING)
                    .description("FCM 토큰을 삭제할 기기 식별자")
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val request = LogoutRequest(deviceId = "device-123")

            val documentFilter = document("auth/logout", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터").optional()
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/auth/logout")
                .then()
                .statusCode(200)
        }
    }
}
