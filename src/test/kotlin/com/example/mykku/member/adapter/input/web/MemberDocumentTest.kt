package com.example.mykku.member.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.docs.RestDocumentationResponse
import com.example.mykku.member.adapter.input.web.dto.ChangeMemberIdRequest
import com.example.mykku.member.adapter.input.web.dto.ChangePasswordRequest
import com.example.mykku.member.adapter.input.web.dto.CheckMemberIdRequest
import com.example.mykku.member.adapter.input.web.dto.SetupProfileRequest
import com.example.mykku.member.adapter.input.web.dto.UpdateProfileRequest
import com.example.mykku.member.application.dto.MemberProfileResult
import com.example.mykku.member.domain.vo.MemberPk
import com.example.mykku.member.exception.MemberErrorCode
import com.example.mykku.member.exception.MemberException
import com.example.mykku.role.application.dto.RoleResult
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.mockito.kotlin.eq
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.RequestDocumentation
import java.time.LocalDateTime

class MemberDocumentTest : BaseDocumentTest() {

    @Nested
    @DisplayName("비밀번호 변경")
    inner class ChangePassword {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("currentPassword").type(JsonFieldType.STRING)
                    .description("현재 비밀번호"),
                fieldWithPath("newPassword").type(JsonFieldType.STRING)
                    .description("새로운 비밀번호 (최소 8자, 영문/숫자/특수문자 포함)")
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val request = ChangePasswordRequest(
                currentPassword = "oldPassword123!",
                newPassword = "newPassword123!"
            )

            doNothing().whenever(changePasswordUseCase).changePassword(any(), any())

            val documentFilter = document("member/change-password", 200)
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
                .put("/api/v1/members/password")
                .then()
                .statusCode(200)
        }

        @Test
        fun `현재 비밀번호 불일치`() {
            val request = ChangePasswordRequest(
                currentPassword = "wrongPassword123!",
                newPassword = "newPassword123!"
            )

            doThrow(MemberException(MemberErrorCode.INVALID_CURRENT_PASSWORD))
                .whenever(changePasswordUseCase).changePassword(any(), any())

            val documentFilter = document("member/change-password", "INVALID_CURRENT_PASSWORD")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .put("/api/v1/members/password")
                .then()
                .statusCode(400)
        }
    }

    @Nested
    @DisplayName("프로필 조회")
    inner class GetMyProfile {

        private val apiConfig = ApiRequestConfig(
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val result = MemberProfileResult(
                memberId = "testmemberid",
                email = TEST_MEMBER_EMAIL,
                nickname = "testuser",
                profileImage = "https://example.com/profile.jpg",
                role = RoleResult(id = 1L, name = "테스트 칭호", description = "테스트 칭호 설명"),
                provider = "GOOGLE",
                emailVerified = true,
                createdAt = LocalDateTime.now()
            )

            whenever(getMemberProfileUseCase.getMyProfile(any())).thenReturn(result)

            val documentFilter = document("member/get-my-profile", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data.memberId").type(JsonFieldType.STRING).description("회원 ID"),
                            fieldWithPath("data.email").type(JsonFieldType.STRING).description("이메일"),
                            fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("닉네임"),
                            fieldWithPath("data.profileImage").type(JsonFieldType.STRING).description("프로필 이미지 URL"),
                            fieldWithPath("data.role").type(JsonFieldType.OBJECT).description("역할/칭호").optional(),
                            fieldWithPath("data.role.id").type(JsonFieldType.NUMBER).description("칭호 ID"),
                            fieldWithPath("data.role.name").type(JsonFieldType.STRING).description("칭호 이름"),
                            fieldWithPath("data.role.description").type(JsonFieldType.STRING).description("칭호 설명").optional(),
                            fieldWithPath("data.provider").type(JsonFieldType.STRING).description("가입 경로 (GOOGLE, KAKAO, EMAIL 등)").optional(),
                            fieldWithPath("data.emailVerified").type(JsonFieldType.BOOLEAN).description("이메일 인증 여부"),
                            fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("가입일시")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .`when`()
                .get("/api/v1/members/me")
                .then()
                .statusCode(200)
        }
    }

    @Nested
    @DisplayName("프로필 수정")
    inner class UpdateProfile {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("nickname").type(JsonFieldType.STRING)
                    .description("새로운 닉네임 (최대 10자, 한글/영문/숫자만 허용)").optional(),
                fieldWithPath("profileImage").type(JsonFieldType.STRING)
                    .description("새로운 프로필 이미지 URL").optional()
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val request = UpdateProfileRequest(
                nickname = "새닉네임",
                profileImage = "https://example.com/new-profile.jpg"
            )
            val result = MemberProfileResult(
                memberId = "testmemberid",
                email = TEST_MEMBER_EMAIL,
                nickname = "새닉네임",
                profileImage = "https://example.com/new-profile.jpg",
                role = RoleResult(id = 1L, name = "테스트 칭호", description = "테스트 칭호 설명"),
                provider = "GOOGLE",
                emailVerified = true,
                createdAt = LocalDateTime.now()
            )

            whenever(updateMemberProfileUseCase.updateProfile(any(), any())).thenReturn(result)

            val documentFilter = document("member/update-profile", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data.memberId").type(JsonFieldType.STRING).description("회원 ID"),
                            fieldWithPath("data.email").type(JsonFieldType.STRING).description("이메일"),
                            fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("닉네임"),
                            fieldWithPath("data.profileImage").type(JsonFieldType.STRING).description("프로필 이미지 URL"),
                            fieldWithPath("data.role").type(JsonFieldType.OBJECT).description("역할/칭호").optional(),
                            fieldWithPath("data.role.id").type(JsonFieldType.NUMBER).description("칭호 ID"),
                            fieldWithPath("data.role.name").type(JsonFieldType.STRING).description("칭호 이름"),
                            fieldWithPath("data.role.description").type(JsonFieldType.STRING).description("칭호 설명").optional(),
                            fieldWithPath("data.provider").type(JsonFieldType.STRING).description("가입 경로").optional(),
                            fieldWithPath("data.emailVerified").type(JsonFieldType.BOOLEAN).description("이메일 인증 여부"),
                            fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("가입일시")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .patch("/api/v1/members/me")
                .then()
                .statusCode(200)
        }

        @Test
        fun `닉네임 중복`() {
            val request = UpdateProfileRequest(
                nickname = "중복닉네임",
                profileImage = null
            )

            doThrow(MemberException(MemberErrorCode.NICKNAME_ALREADY_EXISTS))
                .whenever(updateMemberProfileUseCase).updateProfile(any(), any())

            val documentFilter = document("member/update-profile", "NICKNAME_ALREADY_EXISTS")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .patch("/api/v1/members/me")
                .then()
                .statusCode(409)
        }

        @Test
        fun `닉네임 길이 초과`() {
            val request = UpdateProfileRequest(
                nickname = "가나다라마바사아자차카",
                profileImage = null
            )

            doThrow(MemberException(MemberErrorCode.MEMBER_NICKNAME_TOO_LONG))
                .whenever(updateMemberProfileUseCase).updateProfile(any(), any())

            val documentFilter = document("member/update-profile", "MEMBER_NICKNAME_TOO_LONG")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .patch("/api/v1/members/me")
                .then()
                .statusCode(400)
        }
    }

    @Nested
    @DisplayName("프로필 수정 (이미지 업로드)")
    inner class UpdateProfileWithImage {

        private val apiConfig = ApiRequestConfig(
            requestParts = listOf(
                RequestDocumentation.partWithName("request")
                    .description("프로필 수정 요청 정보 (JSON, 선택사항)").optional(),
                RequestDocumentation.partWithName("profileImage")
                    .description("업로드할 프로필 이미지 파일 (선택사항)").optional()
            ),
            requestPartFields = mapOf(
                "request" to listOf(
                    fieldWithPath("nickname").type(JsonFieldType.STRING)
                        .description("새로운 닉네임 (최대 10자, 한글/영문/숫자만 허용)").optional()
                )
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val result = MemberProfileResult(
                memberId = "testmemberid",
                email = TEST_MEMBER_EMAIL,
                nickname = "새닉네임",
                profileImage = "https://cdn.mykku.kr/profile-images/uploaded-image.jpg",
                role = RoleResult(id = 1L, name = "테스트 칭호", description = "테스트 칭호 설명"),
                provider = "GOOGLE",
                emailVerified = true,
                createdAt = LocalDateTime.now()
            )

            whenever(updateMemberProfileUseCase.updateProfile(any(), any())).thenReturn(result)

            val documentFilter = document("member/update-profile-multipart", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data.memberId").type(JsonFieldType.STRING).description("회원 ID"),
                            fieldWithPath("data.email").type(JsonFieldType.STRING).description("이메일"),
                            fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("닉네임"),
                            fieldWithPath("data.profileImage").type(JsonFieldType.STRING)
                                .description("업로드된 프로필 이미지 URL"),
                            fieldWithPath("data.role").type(JsonFieldType.OBJECT).description("역할/칭호").optional(),
                            fieldWithPath("data.role.id").type(JsonFieldType.NUMBER).description("칭호 ID"),
                            fieldWithPath("data.role.name").type(JsonFieldType.STRING).description("칭호 이름"),
                            fieldWithPath("data.role.description").type(JsonFieldType.STRING)
                                .description("칭호 설명").optional(),
                            fieldWithPath("data.provider").type(JsonFieldType.STRING).description("가입 경로").optional(),
                            fieldWithPath("data.emailVerified").type(JsonFieldType.BOOLEAN)
                                .description("이메일 인증 여부"),
                            fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("가입일시")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .multiPart("request", """{"nickname":"새닉네임"}""", "application/json")
                .multiPart("profileImage", "profile.jpg", "image data".toByteArray(), "image/jpeg")
                .`when`()
                .patch("/api/v1/members/me")
                .then()
                .statusCode(200)
        }
    }

    @Nested
    @DisplayName("프로필 설정")
    inner class SetupProfile {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("memberId").type(JsonFieldType.STRING)
                    .description("아이디 (영문/숫자, 최대 16자)"),
                fieldWithPath("nickname").type(JsonFieldType.STRING)
                    .description("닉네임 (한글/영문/숫자, 최대 10자)")
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val request = SetupProfileRequest(
                memberId = "newuser1",
                nickname = "새닉네임"
            )
            val result = MemberProfileResult(
                memberId = "newuser1",
                email = TEST_MEMBER_EMAIL,
                nickname = "새닉네임",
                profileImage = "https://example.com/profile.jpg",
                role = null,
                provider = "GOOGLE",
                emailVerified = true,
                createdAt = LocalDateTime.now()
            )

            whenever(setupProfileUseCase.setupProfile(any(), any())).thenReturn(result)

            val documentFilter = document("member/setup-profile", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data.memberId").type(JsonFieldType.STRING).description("회원 ID"),
                            fieldWithPath("data.email").type(JsonFieldType.STRING).description("이메일"),
                            fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("닉네임"),
                            fieldWithPath("data.profileImage").type(JsonFieldType.STRING).description("프로필 이미지 URL"),
                            fieldWithPath("data.role").type(JsonFieldType.OBJECT).description("역할/칭호").optional(),
                            fieldWithPath("data.provider").type(JsonFieldType.STRING).description("가입 경로").optional(),
                            fieldWithPath("data.emailVerified").type(JsonFieldType.BOOLEAN).description("이메일 인증 여부"),
                            fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("가입일시")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/members/setup-profile")
                .then()
                .statusCode(200)
        }

        @Test
        fun `아이디 중복`() {
            val request = SetupProfileRequest(
                memberId = "takenid",
                nickname = "새닉네임"
            )

            doThrow(MemberException(MemberErrorCode.MEMBER_ID_ALREADY_EXISTS))
                .whenever(setupProfileUseCase).setupProfile(any(), any())

            val documentFilter = document("member/setup-profile", "MEMBER_ID_ALREADY_EXISTS")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/members/setup-profile")
                .then()
                .statusCode(409)
        }
    }

    @Nested
    @DisplayName("아이디 변경")
    inner class ChangeMemberId {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("memberId").type(JsonFieldType.STRING)
                    .description("새로운 아이디 (영문/숫자, 최대 16자)")
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val request = ChangeMemberIdRequest(memberId = "newuserid")
            val result = MemberProfileResult(
                memberId = "newuserid",
                email = TEST_MEMBER_EMAIL,
                nickname = "testuser",
                profileImage = "https://example.com/profile.jpg",
                role = RoleResult(id = 1L, name = "테스트 칭호", description = "테스트 칭호 설명"),
                provider = "GOOGLE",
                emailVerified = true,
                createdAt = LocalDateTime.now()
            )

            whenever(changeMemberIdUseCase.changeMemberId(any(), any())).thenReturn(result)

            val documentFilter = document("member/change-member-id", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data.memberId").type(JsonFieldType.STRING).description("변경된 회원 ID"),
                            fieldWithPath("data.email").type(JsonFieldType.STRING).description("이메일"),
                            fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("닉네임"),
                            fieldWithPath("data.profileImage").type(JsonFieldType.STRING).description("프로필 이미지 URL"),
                            fieldWithPath("data.role").type(JsonFieldType.OBJECT).description("역할/칭호").optional(),
                            fieldWithPath("data.role.id").type(JsonFieldType.NUMBER).description("칭호 ID"),
                            fieldWithPath("data.role.name").type(JsonFieldType.STRING).description("칭호 이름"),
                            fieldWithPath("data.role.description").type(JsonFieldType.STRING).description("칭호 설명").optional(),
                            fieldWithPath("data.provider").type(JsonFieldType.STRING).description("가입 경로").optional(),
                            fieldWithPath("data.emailVerified").type(JsonFieldType.BOOLEAN).description("이메일 인증 여부"),
                            fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("가입일시")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .patch("/api/v1/members/me/member-id")
                .then()
                .statusCode(200)
        }

        @Test
        fun `아이디 중복`() {
            val request = ChangeMemberIdRequest(memberId = "takenid")

            doThrow(MemberException(MemberErrorCode.MEMBER_ID_ALREADY_EXISTS))
                .whenever(changeMemberIdUseCase).changeMemberId(any(), any())

            val documentFilter = document("member/change-member-id", "MEMBER_ID_ALREADY_EXISTS")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .patch("/api/v1/members/me/member-id")
                .then()
                .statusCode(409)
        }
    }

    @Nested
    @DisplayName("회원 탈퇴")
    inner class Withdraw {

        private val apiConfig = ApiRequestConfig(
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            doNothing().whenever(withdrawMemberUseCase).execute(MemberPk.of(testMember.id))

            val documentFilter = document("member/withdraw", 204)
                .request(request().applyConfig(apiConfig))
                .response(response())
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .`when`()
                .delete("/api/v1/members/me")
                .then()
                .statusCode(204)
        }
    }

    @Nested
    @DisplayName("아이디 중복 확인")
    inner class CheckMemberId {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("memberId").type(JsonFieldType.STRING)
                    .description("확인할 아이디 (영문/숫자, 최대 16자)")
            )
        )

        @Test
        fun `사용 가능한 아이디`() {
            whenever(checkMemberIdUseCase.checkAvailability(any())).thenReturn(true)

            val request = CheckMemberIdRequest(memberId = "availableid")

            val documentFilter = document("member/check-id", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data.memberId").type(JsonFieldType.STRING).description("확인한 아이디"),
                            fieldWithPath("data.available").type(JsonFieldType.BOOLEAN).description("사용 가능 여부")
                        )
                )
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/members/check-id")
                .then()
                .statusCode(200)
        }

        @Test
        fun `이미 사용 중인 아이디`() {
            whenever(checkMemberIdUseCase.checkAvailability(any())).thenReturn(false)

            val request = CheckMemberIdRequest(memberId = "takenid")

            val documentFilter = document("member/check-id", "ALREADY_EXISTS")
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data.memberId").type(JsonFieldType.STRING).description("확인한 아이디"),
                            fieldWithPath("data.available").type(JsonFieldType.BOOLEAN).description("사용 가능 여부")
                        )
                )
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/members/check-id")
                .then()
                .statusCode(200)
        }
    }
}
