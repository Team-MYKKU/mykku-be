package com.example.mykku.email.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.auth.adapter.input.web.dto.LoginResponse
import com.example.mykku.auth.adapter.input.web.dto.MemberInfo
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.docs.RestDocumentationResponse
import com.example.mykku.email.domain.VerificationPurpose
import com.example.mykku.email.dto.EmailLoginRequest
import com.example.mykku.email.dto.ResetPasswordRequest
import com.example.mykku.email.dto.SendTemporaryPasswordRequest
import com.example.mykku.email.dto.SendVerificationCodeRequest
import com.example.mykku.email.dto.SignupRequest
import com.example.mykku.email.dto.VerifyCodeRequest
import com.example.mykku.email.exception.EmailAuthErrorCode
import com.example.mykku.email.exception.EmailAuthException
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath

class EmailAuthDocumentTest : BaseDocumentTest() {

    @Nested
    @DisplayName("인증 코드 발송")
    inner class SendVerificationCode {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("email").type(JsonFieldType.STRING)
                    .description(
                        "인증 코드를 받을 이메일 주소. 이 문자열 그대로(대소문자 구분) 코드가 저장되므로 " +
                            "인증 코드 검증·비밀번호 재설정에도 똑같은 값을 보내야 함"
                    ),
                fieldWithPath("purpose").type(JsonFieldType.STRING)
                    .description(
                        "인증 목적 (SIGNUP: 회원가입, PASSWORD_RESET: 비밀번호 재설정). " +
                            "대소문자 구분, 그 외 값은 400 INVALID_INPUT(C101)"
                    )
            )
        )

        @Test
        fun `성공`() {
            val request = SendVerificationCodeRequest(
                email = "user@example.com",
                purpose = VerificationPurpose.SIGNUP
            )

            doNothing().`when`(emailAuthService).sendVerificationCode(any(), any())

            val documentFilter = document("email-auth/send-code", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("항상 빈 객체({}). 사용하지 않음")
                        )
                )
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/email-auth/send-code")
                .then()
                .statusCode(200)
        }

        @Test
        fun `너무 많은 요청`() {
            val request = SendVerificationCodeRequest(
                email = "user@example.com",
                purpose = VerificationPurpose.SIGNUP
            )

            doThrow(EmailAuthException(EmailAuthErrorCode.TOO_MANY_REQUESTS))
                .`when`(emailAuthService).sendVerificationCode(any(), any())

            val documentFilter = document("email-auth/send-code", "TOO_MANY_REQUESTS")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/email-auth/send-code")
                .then()
                .statusCode(429)
        }

        @Test
        fun `이미 가입된 이메일`() {
            val request = SendVerificationCodeRequest(
                email = "existing@example.com",
                purpose = VerificationPurpose.SIGNUP
            )

            doThrow(EmailAuthException(EmailAuthErrorCode.EMAIL_ALREADY_EXISTS))
                .`when`(emailAuthService).sendVerificationCode(any(), any())

            val documentFilter = document("email-auth/send-code", "EMAIL_ALREADY_EXISTS")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/email-auth/send-code")
                .then()
                .statusCode(409)
        }

        @Test
        fun `이메일 발송 실패`() {
            val request = SendVerificationCodeRequest(
                email = "user@example.com",
                purpose = VerificationPurpose.SIGNUP
            )

            doThrow(EmailAuthException(EmailAuthErrorCode.EMAIL_SEND_FAILED))
                .`when`(emailAuthService).sendVerificationCode(any(), any())

            val documentFilter = document("email-auth/send-code", "EMAIL_SEND_FAILED")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/email-auth/send-code")
                .then()
                .statusCode(500)
        }
    }

    @Nested
    @DisplayName("인증 코드 검증")
    inner class VerifyCode {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("email").type(JsonFieldType.STRING)
                    .description("인증할 이메일 주소. 코드 발송 때와 대소문자까지 똑같은 값"),
                fieldWithPath("code").type(JsonFieldType.STRING)
                    .description("발송받은 6자리 숫자 인증 코드 (숫자 타입이 아닌 문자열로 전송, 앞자리 0 유지. 예: \"012345\")"),
                fieldWithPath("purpose").type(JsonFieldType.STRING)
                    .description(
                        "인증 목적 (SIGNUP: 회원가입, PASSWORD_RESET: 비밀번호 재설정). " +
                            "코드 발송 때와 같은 값이어야 함, 대소문자 구분"
                    )
            )
        )

        @Test
        fun `성공`() {
            val request = VerifyCodeRequest(
                email = "user@example.com",
                code = "123456",
                purpose = VerificationPurpose.SIGNUP
            )

            doNothing().`when`(emailAuthService).verifyCode(any(), any(), any())

            val documentFilter = document("email-auth/verify-code", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("항상 빈 객체({}). 사용하지 않음")
                        )
                )
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/email-auth/verify-code")
                .then()
                .statusCode(200)
        }

        @Test
        fun `유효하지 않은 코드`() {
            val request = VerifyCodeRequest(
                email = "user@example.com",
                code = "000000",
                purpose = VerificationPurpose.SIGNUP
            )

            doThrow(EmailAuthException(EmailAuthErrorCode.INVALID_VERIFICATION_CODE))
                .`when`(emailAuthService).verifyCode(any(), any(), any())

            val documentFilter = document("email-auth/verify-code", "INVALID_VERIFICATION_CODE")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/email-auth/verify-code")
                .then()
                .statusCode(400)
        }

        @Test
        fun `만료된 코드`() {
            val request = VerifyCodeRequest(
                email = "user@example.com",
                code = "123456",
                purpose = VerificationPurpose.SIGNUP
            )

            doThrow(EmailAuthException(EmailAuthErrorCode.VERIFICATION_CODE_EXPIRED))
                .`when`(emailAuthService).verifyCode(any(), any(), any())

            val documentFilter = document("email-auth/verify-code", "VERIFICATION_CODE_EXPIRED")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/email-auth/verify-code")
                .then()
                .statusCode(400)
        }
    }

    @Nested
    @DisplayName("이메일 회원가입")
    inner class Signup {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("email").type(JsonFieldType.STRING)
                    .description("가입할 이메일 주소. 인증 코드 발송·검증에 사용한 이메일과 같은 값"),
                fieldWithPath("password").type(JsonFieldType.STRING)
                    .description(
                        "비밀번호 (최소 8자, 영문·숫자·특수문자를 각각 1자 이상 포함. " +
                            "특수문자는 @ \$ ! % * # ? & 만 허용되며 공백 등 그 외 문자는 사용 불가)"
                    )
            )
        )

        @Test
        fun `성공`() {
            val request = SignupRequest(
                email = "newuser@example.com",
                password = "password123!"
            )

            val loginResponse = LoginResponse(
                accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh...",
                accessTokenExpiresIn = 86400000,
                refreshTokenExpiresIn = 1209600000,
                member = MemberInfo(
                    memberId = null,
                    email = request.email,
                    nickname = null,
                    profileImage = ""
                ),
                isExistingUser = false,
                isProfileComplete = false
            )

            `when`(emailAuthService.signup(any(), any())).thenReturn(loginResponse)

            val documentFilter = document("email-auth/signup", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT)
                                .description("로그인 응답 데이터 (가입과 동시에 로그인 처리되어 토큰이 발급됨)"),
                            fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("JWT 액세스 토큰"),
                            fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("JWT 리프레시 토큰"),
                            fieldWithPath("data.tokenType").type(JsonFieldType.STRING)
                                .description("토큰 타입. 항상 \"Bearer\" (Authorization 헤더에 Bearer 뒤 한 칸 띄고 accessToken)"),
                            fieldWithPath("data.accessTokenExpiresIn").type(JsonFieldType.NUMBER)
                                .description(
                                    "액세스 토큰 유효 기간 (ms). 발급 시점부터의 길이이며 만료 절대 시각(epoch)이 아님. " +
                                        "예: 86400000 = 24시간"
                                ),
                            fieldWithPath("data.refreshTokenExpiresIn").type(JsonFieldType.NUMBER)
                                .description(
                                    "리프레시 토큰 유효 기간 (ms). 발급 시점부터의 길이이며 만료 절대 시각(epoch)이 아님. " +
                                        "예: 1209600000 = 14일"
                                ),
                            fieldWithPath("data.member").type(JsonFieldType.OBJECT).description("회원 정보"),
                            fieldWithPath("data.member.memberId").type(JsonFieldType.STRING)
                                .description(
                                    "회원 아이디 (사용자가 정하는 영문/숫자 최대 16자 문자열, DB PK 아님). " +
                                        "회원가입 직후에는 항상 null이며 프로필 설정 후 채워짐"
                                )
                                .optional(),
                            fieldWithPath("data.member.email").type(JsonFieldType.STRING).description("회원 이메일"),
                            fieldWithPath("data.member.nickname").type(JsonFieldType.STRING)
                                .description("회원 닉네임. 회원가입 직후에는 항상 null이며 프로필 설정 후 채워짐")
                                .optional(),
                            fieldWithPath("data.member.profileImage").type(JsonFieldType.STRING)
                                .description("프로필 이미지 URL. 회원가입 직후에는 항상 빈 문자열(\"\"), null은 오지 않음"),
                            fieldWithPath("data.isExistingUser").type(JsonFieldType.BOOLEAN)
                                .description("기존 가입자 여부. 회원가입 응답은 항상 false (소셜 로그인과 같은 응답 구조)"),
                            fieldWithPath("data.isProfileComplete").type(JsonFieldType.BOOLEAN)
                                .description(
                                    "프로필 설정 완료 여부 (memberId와 nickname이 모두 설정되면 true). " +
                                        "회원가입 직후에는 항상 false이므로 프로필 설정이 필요함"
                                )
                        )
                )
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/email-auth/signup")
                .then()
                .statusCode(200)
        }

        @Test
        fun `이미 존재하는 이메일`() {
            val request = SignupRequest(
                email = "existing@example.com",
                password = "password123!"
            )

            `when`(emailAuthService.signup(any(), any()))
                .thenThrow(EmailAuthException(EmailAuthErrorCode.EMAIL_ALREADY_EXISTS))

            val documentFilter = document("email-auth/signup", "EMAIL_ALREADY_EXISTS")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/email-auth/signup")
                .then()
                .statusCode(409)
        }
    }

    @Nested
    @DisplayName("이메일 로그인")
    inner class Login {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("email").type(JsonFieldType.STRING).description("이메일 주소"),
                fieldWithPath("password").type(JsonFieldType.STRING)
                    .description("비밀번호. 빈 값·공백만 있는 값은 400, 그 외 형식 검사는 하지 않음 (임시 비밀번호도 사용 가능)")
            )
        )

        @Test
        fun `성공`() {
            val request = EmailLoginRequest(
                email = "user@example.com",
                password = "password123!"
            )

            val loginResponse = LoginResponse(
                accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh...",
                accessTokenExpiresIn = 86400000,
                refreshTokenExpiresIn = 1209600000,
                member = MemberInfo(
                    memberId = "userId",
                    email = request.email,
                    nickname = "테스트유저",
                    profileImage = ""
                ),
                isExistingUser = true,
                isProfileComplete = true
            )

            `when`(emailAuthService.login(any(), any())).thenReturn(loginResponse)

            val documentFilter = document("email-auth/login", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("로그인 응답 데이터"),
                            fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("JWT 액세스 토큰"),
                            fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("JWT 리프레시 토큰"),
                            fieldWithPath("data.tokenType").type(JsonFieldType.STRING)
                                .description("토큰 타입. 항상 \"Bearer\" (Authorization 헤더에 Bearer 뒤 한 칸 띄고 accessToken)"),
                            fieldWithPath("data.accessTokenExpiresIn").type(JsonFieldType.NUMBER)
                                .description(
                                    "액세스 토큰 유효 기간 (ms). 발급 시점부터의 길이이며 만료 절대 시각(epoch)이 아님. " +
                                        "예: 86400000 = 24시간"
                                ),
                            fieldWithPath("data.refreshTokenExpiresIn").type(JsonFieldType.NUMBER)
                                .description(
                                    "리프레시 토큰 유효 기간 (ms). 발급 시점부터의 길이이며 만료 절대 시각(epoch)이 아님. " +
                                        "예: 1209600000 = 14일"
                                ),
                            fieldWithPath("data.member").type(JsonFieldType.OBJECT).description("회원 정보"),
                            fieldWithPath("data.member.memberId").type(JsonFieldType.STRING)
                                .description(
                                    "회원 아이디 (사용자가 정하는 영문/숫자 최대 16자 문자열, DB PK 아님). " +
                                        "프로필 설정 전이면 null"
                                )
                                .optional(),
                            fieldWithPath("data.member.email").type(JsonFieldType.STRING).description("회원 이메일"),
                            fieldWithPath("data.member.nickname").type(JsonFieldType.STRING)
                                .description("회원 닉네임. 프로필 설정 전이면 null")
                                .optional(),
                            fieldWithPath("data.member.profileImage").type(JsonFieldType.STRING)
                                .description("프로필 이미지 URL. 설정하지 않았으면 빈 문자열(\"\"), null은 오지 않음"),
                            fieldWithPath("data.isExistingUser").type(JsonFieldType.BOOLEAN)
                                .description("기존 가입자 여부. 이메일 로그인은 항상 true (소셜 로그인과 같은 응답 구조)"),
                            fieldWithPath("data.isProfileComplete").type(JsonFieldType.BOOLEAN)
                                .description(
                                    "프로필 설정 완료 여부 (memberId와 nickname이 모두 설정되면 true). " +
                                        "false면 프로필 설정 화면으로 이동해야 함"
                                )
                        )
                )
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/email-auth/login")
                .then()
                .statusCode(200)
        }

        @Test
        fun `잘못된 이메일 또는 비밀번호`() {
            val request = EmailLoginRequest(
                email = "wrong@example.com",
                password = "wrongPassword123!"
            )

            `when`(emailAuthService.login(any(), any()))
                .thenThrow(EmailAuthException(EmailAuthErrorCode.INVALID_EMAIL_OR_PASSWORD))

            val documentFilter = document("email-auth/login", "INVALID_EMAIL_OR_PASSWORD")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/email-auth/login")
                .then()
                .statusCode(401)
        }
    }

    @Nested
    @DisplayName("비밀번호 재설정")
    inner class ResetPassword {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("email").type(JsonFieldType.STRING)
                    .description("비밀번호를 재설정할 이메일 주소. 코드 발송 때와 대소문자까지 똑같은 값"),
                fieldWithPath("code").type(JsonFieldType.STRING)
                    .description(
                        "purpose=PASSWORD_RESET으로 발송받은 6자리 숫자 인증 코드 " +
                            "(숫자 타입이 아닌 문자열로 전송, 앞자리 0 유지. 예: \"012345\")"
                    ),
                fieldWithPath("newPassword").type(JsonFieldType.STRING)
                    .description(
                        "새로운 비밀번호 (최소 8자, 영문·숫자·특수문자를 각각 1자 이상 포함. " +
                            "특수문자는 @ \$ ! % * # ? & 만 허용되며 공백 등 그 외 문자는 사용 불가)"
                    )
            )
        )

        @Test
        fun `성공`() {
            val request = ResetPasswordRequest(
                email = "user@example.com",
                code = "123456",
                newPassword = "newPassword123!"
            )

            doNothing().`when`(emailAuthService).resetPassword(any(), any(), any())

            val documentFilter = document("email-auth/reset-password", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("항상 빈 객체({}). 사용하지 않음")
                        )
                )
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/email-auth/reset-password")
                .then()
                .statusCode(200)
        }

        @Test
        fun `유효하지 않은 코드`() {
            val request = ResetPasswordRequest(
                email = "user@example.com",
                code = "000000",
                newPassword = "newPassword123!"
            )

            doThrow(EmailAuthException(EmailAuthErrorCode.INVALID_VERIFICATION_CODE))
                .`when`(emailAuthService).resetPassword(any(), any(), any())

            val documentFilter = document("email-auth/reset-password", "INVALID_VERIFICATION_CODE")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/email-auth/reset-password")
                .then()
                .statusCode(400)
        }

        @Test
        fun `만료된 코드`() {
            val request = ResetPasswordRequest(
                email = "user@example.com",
                code = "123456",
                newPassword = "newPassword123!"
            )

            doThrow(EmailAuthException(EmailAuthErrorCode.VERIFICATION_CODE_EXPIRED))
                .`when`(emailAuthService).resetPassword(any(), any(), any())

            val documentFilter = document("email-auth/reset-password", "VERIFICATION_CODE_EXPIRED")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/email-auth/reset-password")
                .then()
                .statusCode(400)
        }

        @Test
        fun `가입되지 않은 이메일`() {
            val request = ResetPasswordRequest(
                email = "unknown@example.com",
                code = "123456",
                newPassword = "newPassword123!"
            )

            doThrow(EmailAuthException(EmailAuthErrorCode.INVALID_EMAIL_OR_PASSWORD))
                .`when`(emailAuthService).resetPassword(any(), any(), any())

            val documentFilter = document("email-auth/reset-password", "INVALID_EMAIL_OR_PASSWORD")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/email-auth/reset-password")
                .then()
                .statusCode(401)
        }
    }

    @Nested
    @DisplayName("임시 비밀번호 발송")
    inner class SendTemporaryPassword {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("email").type(JsonFieldType.STRING).description("임시 비밀번호를 받을 가입된 이메일 주소")
            )
        )

        @Test
        fun `성공`() {
            val request = SendTemporaryPasswordRequest(email = "user@example.com")

            doNothing().`when`(emailAuthService).sendTemporaryPassword(any())

            val documentFilter = document("email-auth/send-temporary-password", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("항상 빈 객체({}). 사용하지 않음")
                        )
                )
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/email-auth/send-temporary-password")
                .then()
                .statusCode(200)
        }

        @Test
        fun `가입되지 않은 이메일`() {
            val request = SendTemporaryPasswordRequest(email = "unknown@example.com")

            doThrow(EmailAuthException(EmailAuthErrorCode.INVALID_EMAIL_OR_PASSWORD))
                .`when`(emailAuthService).sendTemporaryPassword(any())

            val documentFilter = document("email-auth/send-temporary-password", "INVALID_EMAIL_OR_PASSWORD")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/email-auth/send-temporary-password")
                .then()
                .statusCode(401)
        }

        @Test
        fun `이메일 발송 실패`() {
            val request = SendTemporaryPasswordRequest(email = "user@example.com")

            doThrow(EmailAuthException(EmailAuthErrorCode.EMAIL_SEND_FAILED))
                .`when`(emailAuthService).sendTemporaryPassword(any())

            val documentFilter = document("email-auth/send-temporary-password", "EMAIL_SEND_FAILED")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/email-auth/send-temporary-password")
                .then()
                .statusCode(500)
        }
    }

}
