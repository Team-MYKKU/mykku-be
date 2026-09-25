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
import com.example.mykku.member.exception.MemberErrorCode
import com.example.mykku.member.exception.MemberException
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath

class AuthDocumentTest : BaseDocumentTest() {

    companion object {
        private const val TOKEN_TYPE_DESCRIPTION =
            "토큰 타입. 항상 \"Bearer\" (인증 헤더는 Authorization: Bearer {JWT 액세스 토큰} 형식)"
        private const val ACCESS_TOKEN_EXPIRES_IN_DESCRIPTION =
            "액세스 토큰 유효 기간(밀리초). 발급 시점부터의 상대 시간이며 만료 시각 타임스탬프가 아님 " +
                "(예: 86400000 = 24시간)"
        private const val REFRESH_TOKEN_EXPIRES_IN_DESCRIPTION =
            "리프레시 토큰 유효 기간(밀리초). 발급 시점부터의 상대 시간이며 만료 시각 타임스탬프가 아님 " +
                "(예: 1209600000 = 14일)"
    }

    @Nested
    @DisplayName("모바일 소셜 로그인")
    inner class MobileLogin {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("provider").type(JsonFieldType.STRING)
                    .description(
                        "소셜 제공자 (GOOGLE: 구글, KAKAO: 카카오, APPLE: 애플, NAVER: 네이버). " +
                            "대소문자를 구분하며 그 외 값은 400(C101). " +
                            "EMAIL은 이메일 가입 전용이라 400(AU102)"
                    ),
                fieldWithPath("accessToken").type(JsonFieldType.STRING)
                    .description(
                        "소셜 SDK에서 받은 OAuth 액세스 토큰. provider가 GOOGLE, KAKAO, NAVER이면 필수" +
                            "(없거나 빈 문자열이면 400 C101), APPLE이면 생략 가능하며 보내도 사용되지 않음. " +
                            "GOOGLE도 ID 토큰이 아닌 액세스 토큰을 보내야 함"
                    )
                    .optional(),
                fieldWithPath("idToken").type(JsonFieldType.STRING)
                    .description(
                        "Sign in with Apple의 identityToken(JWT). provider가 APPLE이면 필수" +
                            "(없거나 빈 문자열이면 400 C101), 다른 제공자는 생략 가능하며 보내도 사용되지 않음"
                    )
                    .optional()
            )
        )

        private fun loginResponse(): RestDocumentationResponse = response()
            .responseBodyField(
                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                fieldWithPath("data").type(JsonFieldType.OBJECT).description("로그인 응답 데이터"),
                fieldWithPath("data.accessToken").type(JsonFieldType.STRING)
                    .description("JWT 액세스 토큰. 인증이 필요한 API의 Authorization 헤더에 사용"),
                fieldWithPath("data.tokenType").type(JsonFieldType.STRING).description(TOKEN_TYPE_DESCRIPTION),
                fieldWithPath("data.refreshToken").type(JsonFieldType.STRING)
                    .description("JWT 리프레시 토큰. 액세스 토큰 갱신(POST /api/v1/auth/refresh)에 사용"),
                fieldWithPath("data.accessTokenExpiresIn").type(JsonFieldType.NUMBER)
                    .description(ACCESS_TOKEN_EXPIRES_IN_DESCRIPTION),
                fieldWithPath("data.refreshTokenExpiresIn").type(JsonFieldType.NUMBER)
                    .description(REFRESH_TOKEN_EXPIRES_IN_DESCRIPTION),
                fieldWithPath("data.member").type(JsonFieldType.OBJECT).description("회원 정보"),
                fieldWithPath("data.member.memberId").type(JsonFieldType.STRING)
                    .description(
                        "사용자 아이디 (화면에 노출되는 문자열 ID, 영문·숫자 최대 16자, DB PK 아님). " +
                            "프로필 설정 전(isProfileComplete=false)에는 null"
                    )
                    .optional(),
                fieldWithPath("data.member.email").type(JsonFieldType.STRING)
                    .description(
                        "이번 로그인에서 소셜 제공자가 준 계정 이메일. 제공자가 이메일을 주지 않으면 서버가 만든 임시 값 " +
                            "(KAKAO: kakao_{소셜 ID}@kakao.com, NAVER: naver_{소셜 ID}@naver.com, " +
                            "APPLE: apple_{Apple sub}@privaterelay.appleid.com). GOOGLE은 항상 구글 계정 이메일"
                    ),
                fieldWithPath("data.member.nickname").type(JsonFieldType.STRING)
                    .description("닉네임 (최대 10자). 프로필 설정 전(isProfileComplete=false)에는 null")
                    .optional(),
                fieldWithPath("data.member.profileImage").type(JsonFieldType.STRING)
                    .description(
                        "회원 프로필 이미지 URL. 이미지가 없으면 빈 문자열(\"\")이며 null은 반환되지 않음. " +
                            "가입 시 소셜 제공자의 프로필 이미지 URL(APPLE은 빈 문자열)이 저장되고, " +
                            "이후 로그인에서 소셜 제공자 이미지로 다시 갱신되지 않음 (프로필 수정으로 바꾼 값은 그대로 반환)"
                    ),
                fieldWithPath("data.isExistingUser").type(JsonFieldType.BOOLEAN)
                    .description("기존 가입자 여부 (true: 이미 가입된 계정, false: 이번 요청에서 새로 가입됨)"),
                fieldWithPath("data.isProfileComplete").type(JsonFieldType.BOOLEAN)
                    .description(
                        "프로필 설정 완료 여부 (true: 아이디·닉네임 설정 완료, " +
                            "false: 미설정 → 프로필 설정 API(POST /api/v1/members/setup-profile) 호출 필요). " +
                            "기존 가입자도 프로필 설정 전이면 false"
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
                    memberId = "mykkuuser01",
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
                .response(loginResponse())
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
        fun `신규 가입자`() {
            val request = MobileLoginRequest(
                provider = SocialProvider.APPLE,
                accessToken = null,
                idToken = "eyJraWQiOiJXNldjT0tCIiwiYWxnIjoiUlMyNTYifQ.apple.identity.token"
            )

            val loginResult = LoginResult(
                accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh...",
                tokenType = "Bearer",
                accessTokenExpiresIn = 86400000,
                refreshTokenExpiresIn = 1209600000,
                member = MemberInfoResult(
                    memberId = null,
                    email = "apple_001234.abcdef0123456789.0123@privaterelay.appleid.com",
                    nickname = null,
                    profileImage = ""
                ),
                isExistingUser = false,
                isProfileComplete = false
            )

            whenever(mobileLoginUseCase.login(any())).thenReturn(loginResult)

            val documentFilter = document("auth/mobile-login", "NEW_USER")
                .request(request().applyConfig(apiConfig))
                .response(loginResponse())
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
        fun `소셜 제공자 접근 거부`() {
            val request = MobileLoginRequest(
                provider = SocialProvider.KAKAO,
                accessToken = "kakao_access_token_example"
            )

            whenever(mobileLoginUseCase.login(any()))
                .thenThrow(AuthException(AuthErrorCode.OAUTH_ACCESS_DENIED))

            val documentFilter = document("auth/mobile-login", "OAUTH_ACCESS_DENIED")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/auth/mobile/login")
                .then()
                .statusCode(403)
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
        fun `소셜 제공자 서버 오류`() {
            val request = MobileLoginRequest(
                provider = SocialProvider.GOOGLE,
                accessToken = "google_access_token_example"
            )

            whenever(mobileLoginUseCase.login(any()))
                .thenThrow(AuthException(AuthErrorCode.OAUTH_SERVER_ERROR))

            val documentFilter = document("auth/mobile-login", "OAUTH_SERVER_ERROR")
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

        @Test
        fun `EMAIL 제공자로 요청`() {
            val request = mapOf(
                "provider" to SocialProvider.EMAIL.name,
                "accessToken" to "email_provider_access_token"
            )

            val documentFilter = document("auth/mobile-login", "MOBILE_LOGIN_NOT_SUPPORTED")
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
    }

    @Nested
    @DisplayName("액세스 토큰 갱신")
    inner class RefreshToken {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("refreshToken").type(JsonFieldType.STRING)
                    .description(
                        "로그인 또는 이전 갱신 응답에서 받은 리프레시 토큰. " +
                            "만료·위조되었거나 형식이 잘못된 토큰(빈 문자열 포함), 액세스 토큰이면 401(AU202). " +
                            "필드가 없거나 null이면 400(C101)"
                    )
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
                                .description(
                                    "새로 재발급된 리프레시 토큰. 유효 기간이 이 시점부터 다시 시작되므로 " +
                                        "클라이언트는 저장값을 이 값으로 교체해야 함"
                                ),
                            fieldWithPath("data.tokenType").type(JsonFieldType.STRING)
                                .description(TOKEN_TYPE_DESCRIPTION),
                            fieldWithPath("data.expiresIn").type(JsonFieldType.NUMBER)
                                .description(
                                    "$ACCESS_TOKEN_EXPIRES_IN_DESCRIPTION. " +
                                        "로그인 응답의 accessTokenExpiresIn과 같은 의미이며 필드명만 다름"
                                ),
                            fieldWithPath("data.refreshTokenExpiresIn").type(JsonFieldType.NUMBER)
                                .description(REFRESH_TOKEN_EXPIRES_IN_DESCRIPTION)
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

        @Test
        fun `존재하지 않는 회원의 리프레시 토큰`() {
            val request = RefreshTokenRequest(
                refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.withdrawn.member.refresh.token"
            )

            whenever(refreshTokenUseCase.refresh(any()))
                .thenThrow(MemberException(MemberErrorCode.MEMBER_NOT_FOUND))

            val documentFilter = document("auth/refresh", "MEMBER_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(404)
        }
    }

    @Nested
    @DisplayName("로그아웃")
    inner class Logout {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("deviceId").type(JsonFieldType.STRING)
                    .description(
                        "FCM 토큰 등록(POST /api/v1/fcm-tokens) 때 보낸 것과 같은 기기 식별자. " +
                            "로그인 회원의 이 기기 FCM 토큰이 삭제되며, 일치하는 토큰이 없어도 200을 반환함. " +
                            "없거나 빈 문자열이면 400(C101)"
                    )
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
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("항상 빈 객체({})")
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
