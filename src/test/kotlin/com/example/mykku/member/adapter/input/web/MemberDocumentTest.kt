package com.example.mykku.member.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.docs.RestDocumentationResponse
import com.example.mykku.image.exception.ImageErrorCode
import com.example.mykku.image.exception.ImageException
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
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.RequestDocumentation
import java.time.LocalDateTime

class MemberDocumentTest : BaseDocumentTest() {

    private fun profileResponseFields(
        memberId: FieldDescriptor = fieldWithPath("data.memberId").type(JsonFieldType.STRING)
            .description(
                "회원 아이디 (회원이 설정한 영문·숫자 문자열, 최대 16자. DB PK 아님). " +
                    "프로필 설정(setup-profile) 전이면 null"
            )
            .optional(),
        nickname: FieldDescriptor = fieldWithPath("data.nickname").type(JsonFieldType.STRING)
            .description("닉네임. 프로필 설정(setup-profile) 전이면 null")
            .optional(),
        profileImageDescription: String = "프로필 이미지 URL (구글·카카오·네이버 가입은 소셜 계정의 프로필 사진 URL, " +
            "애플·이메일 가입은 빈 문자열로 시작). " +
            "이미지가 없으면 null이 아닌 빈 문자열(\"\")이므로 클라이언트 기본 이미지를 표시"
    ): RestDocumentationResponse = response().responseBodyField(
        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
        memberId,
        fieldWithPath("data.email").type(JsonFieldType.STRING)
            .description(
                "이메일. 소셜 계정에서 이메일을 받지 못하면 서버가 만든 대체 주소가 들어감 " +
                    "(kakao_{카카오 회원번호}@kakao.com, apple_{애플 sub}@privaterelay.appleid.com, " +
                    "naver_{네이버 회원 ID}@naver.com)"
            ),
        nickname,
        fieldWithPath("data.profileImage").type(JsonFieldType.STRING).description(profileImageDescription),
        fieldWithPath("data.role").type(JsonFieldType.OBJECT)
            .description(
                "대표 칭호 1개 (보유 칭호 전체 아님. 칭호 API의 대표 칭호 변경으로 바꿀 수 있음). " +
                    "가입 시 '첫 만남' 칭호가 비동기로 지급되어 대표로 지정되며, 대표 칭호가 없으면 null"
            )
            .optional(),
        fieldWithPath("data.role.id").type(JsonFieldType.NUMBER).description("칭호 ID"),
        fieldWithPath("data.role.name").type(JsonFieldType.STRING).description("칭호 이름"),
        fieldWithPath("data.role.description").type(JsonFieldType.STRING)
            .description("칭호 설명 (없으면 null)").optional(),
        fieldWithPath("data.provider").type(JsonFieldType.STRING)
            .description(
                "가입 경로 (GOOGLE: 구글 로그인, KAKAO: 카카오 로그인, NAVER: 네이버 로그인, " +
                    "APPLE: 애플 로그인, EMAIL: 이메일 회원가입). 비밀번호 변경은 EMAIL 회원만 가능. " +
                    "현재 모든 가입 경로에서 값이 채워지지만 null 가능 필드로 선언됨"
            )
            .optional(),
        fieldWithPath("data.emailVerified").type(JsonFieldType.BOOLEAN)
            .description(
                "이메일 인증 여부. 가입 시 소셜 가입은 true, 이메일 가입은 false로 저장되고 이후 바뀌지 않음 " +
                    "(이메일 가입 기능 도입 전에 가입한 소셜 회원은 false). " +
                    "이메일 가입자의 인증 코드 확인 여부를 나타내지 않음"
            ),
        fieldWithPath("data.createdAt").type(JsonFieldType.STRING)
            .description("가입일시 (KST, ISO-8601, 오프셋 없음)")
    )

    @Nested
    @DisplayName("비밀번호 변경")
    inner class ChangePassword {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("currentPassword").type(JsonFieldType.STRING)
                    .description("현재 비밀번호 (빈 값이면 400 C101)"),
                fieldWithPath("newPassword").type(JsonFieldType.STRING)
                    .description(
                        "새 비밀번호 (8자 이상, 영문·숫자·특수문자를 각각 1개 이상 포함. " +
                            "사용 가능한 문자는 영문 대소문자, 숫자, 특수문자 @ \$ ! % * # ? & 뿐이며 " +
                            "그 외 문자(공백, 하이픈, 마침표, 한글 등)가 하나라도 있으면 400 C101). " +
                            "길이 상한 검증은 없으며 72자를 넘으면 400이 아닌 500 C402가 반환됨"
                    )
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
                            fieldWithPath("data").type(JsonFieldType.OBJECT)
                                .description("항상 빈 객체({}). 사용하지 않음").optional()
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
                .response(profileResponseFields())
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
                    .description(
                        "새 닉네임 (최대 10자, 한글 완성형·영문·숫자·공백만 허용). 생략하거나 null이면 변경하지 않음. " +
                            "10자 초과는 400 C101, 허용 외 문자·빈 문자열은 MB102, 다른 회원이 사용 중이면 MB301"
                    ).optional(),
                fieldWithPath("profileImage").type(JsonFieldType.STRING)
                    .description(
                        "새 프로필 이미지 URL (http 또는 https URL, 최대 255자. 어기면 400 C101). " +
                            "빈 문자열(\"\")이면 이미지 제거, 생략하거나 null이면 변경하지 않음. 전달한 URL을 그대로 저장"
                    ).optional()
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
                .response(profileResponseFields())
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
        fun `닉네임 형식 오류`() {
            val request = UpdateProfileRequest(
                nickname = "닉네임!",
                profileImage = null
            )

            doThrow(MemberException(MemberErrorCode.MEMBER_NICKNAME_INVALID_FORMAT))
                .whenever(updateMemberProfileUseCase).updateProfile(any(), any())

            val documentFilter = document("member/update-profile", "MEMBER_NICKNAME_INVALID_FORMAT")
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

        @Test
        fun `닉네임 길이 초과`() {
            val request = UpdateProfileRequest(
                nickname = "가나다라마바사아자차카",
                profileImage = null
            )

            val documentFilter = document("member/update-profile", "INVALID_INPUT")
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
                    .description(
                        "수정할 정보 JSON (선택). 파트 Content-Type은 application/json이어야 하며 아니면 415 C203"
                    ).optional(),
                RequestDocumentation.partWithName("profileImage")
                    .description(
                        "업로드할 프로필 이미지 파일 (선택, 파일명 확장자 jpg·jpeg·png·gif, 최대 10MB). " +
                            "비어 있지 않으면 업로드한 이미지 URL로 변경되고 request.profileImage는 무시됨. 빈 파일은 무시"
                    ).optional()
            ),
            requestPartFields = mapOf(
                "request" to listOf(
                    fieldWithPath("nickname").type(JsonFieldType.STRING)
                        .description(
                            "새 닉네임 (최대 10자, 한글 완성형·영문·숫자·공백만 허용). 생략하거나 null이면 변경하지 않음"
                        ).optional(),
                    fieldWithPath("profileImage").type(JsonFieldType.STRING)
                        .description(
                            "새 프로필 이미지 URL (http 또는 https URL, 최대 255자). 빈 문자열이면 이미지 제거. " +
                                "profileImage 파일 파트가 비어 있지 않으면 파일이 우선 적용되어 이 값은 무시됨"
                        ).optional()
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
                    profileResponseFields(
                        profileImageDescription = "프로필 이미지 URL (파일을 올렸으면 업로드된 이미지 URL). " +
                            "이미지가 없으면 null이 아닌 빈 문자열(\"\")이므로 클라이언트 기본 이미지를 표시"
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

        @Test
        fun `닉네임 중복`() {
            doThrow(MemberException(MemberErrorCode.NICKNAME_ALREADY_EXISTS))
                .whenever(updateMemberProfileUseCase).updateProfile(any(), any())

            val documentFilter = document("member/update-profile-multipart", "NICKNAME_ALREADY_EXISTS")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .multiPart("request", """{"nickname":"중복닉네임"}""", "application/json")
                .`when`()
                .patch("/api/v1/members/me")
                .then()
                .statusCode(409)
        }

        @Test
        fun `닉네임 형식 오류`() {
            doThrow(MemberException(MemberErrorCode.MEMBER_NICKNAME_INVALID_FORMAT))
                .whenever(updateMemberProfileUseCase).updateProfile(any(), any())

            val documentFilter = document("member/update-profile-multipart", "MEMBER_NICKNAME_INVALID_FORMAT")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .multiPart("request", """{"nickname":"닉네임!"}""", "application/json")
                .`when`()
                .patch("/api/v1/members/me")
                .then()
                .statusCode(400)
        }

        @Test
        fun `지원하지 않는 이미지 확장자`() {
            doThrow(ImageException(ImageErrorCode.IMAGE_INVALID_FORMAT))
                .whenever(updateMemberProfileUseCase).updateProfile(any(), any())

            val documentFilter = document("member/update-profile-multipart", "IMAGE_INVALID_FORMAT")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .multiPart("request", """{"nickname":"새닉네임"}""", "application/json")
                .multiPart("profileImage", "profile.bmp", "image data".toByteArray(), "image/bmp")
                .`when`()
                .patch("/api/v1/members/me")
                .then()
                .statusCode(400)
        }

        @Test
        fun `읽을 수 없는 이미지 파일`() {
            doThrow(ImageException(ImageErrorCode.IMAGE_UNREADABLE))
                .whenever(updateMemberProfileUseCase).updateProfile(any(), any())

            val documentFilter = document("member/update-profile-multipart", "IMAGE_UNREADABLE")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .multiPart("request", """{"nickname":"새닉네임"}""", "application/json")
                .multiPart("profileImage", "profile.webp", "not an image".toByteArray(), "image/webp")
                .`when`()
                .patch("/api/v1/members/me")
                .then()
                .statusCode(400)
        }
    }

    @Nested
    @DisplayName("프로필 설정")
    inner class SetupProfile {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("memberId").type(JsonFieldType.STRING)
                    .description(
                        "아이디 (영문 대소문자·숫자만, 최대 16자, 빈 값 불가). 형식 오류는 400 C101, " +
                            "이미 사용 중이면 MB302 (대소문자 구분 없이 비교)"
                    ),
                fieldWithPath("nickname").type(JsonFieldType.STRING)
                    .description(
                        "닉네임 (한글 완성형·영문·숫자·공백만, 최대 10자, 빈 값·공백만 불가). " +
                            "형식 오류는 400 C101. 다른 회원과 중복 검사하지 않음"
                    )
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
                role = RoleResult(id = 1L, name = "첫 만남", description = "로그인 하면 무조건 줌"),
                provider = "GOOGLE",
                emailVerified = true,
                createdAt = LocalDateTime.now()
            )

            whenever(setupProfileUseCase.setupProfile(any(), any())).thenReturn(result)

            val documentFilter = document("member/setup-profile", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    profileResponseFields(
                        memberId = fieldWithPath("data.memberId").type(JsonFieldType.STRING)
                            .description("설정된 회원 아이디 (영문·숫자 문자열, DB PK 아님)"),
                        nickname = fieldWithPath("data.nickname").type(JsonFieldType.STRING)
                            .description("설정된 닉네임")
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
                    .description(
                        "새 아이디 (영문 대소문자·숫자만, 최대 16자, 빈 값 불가). 형식 오류는 400 C101. " +
                            "현재 본인 아이디와 같아도 성공"
                    )
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
                    profileResponseFields(
                        memberId = fieldWithPath("data.memberId").type(JsonFieldType.STRING)
                            .description("변경된 회원 아이디 (영문·숫자 문자열, DB PK 아님)")
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
                    .description(
                        "확인할 아이디 (영문 대소문자·숫자만, 최대 16자, 빈 값 불가). " +
                            "형식이 맞지 않으면 available 대신 400 C101"
                    )
            )
        )

        private fun checkIdResponseFields(): RestDocumentationResponse = response()
            .responseBodyField(
                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                fieldWithPath("data.memberId").type(JsonFieldType.STRING).description("확인한 아이디 (요청 값 그대로)"),
                fieldWithPath("data.available").type(JsonFieldType.BOOLEAN)
                    .description(
                        "true: 사용 가능, false: 이미 사용 중 (본인의 현재 아이디 포함, 대소문자 구분 없이 비교)"
                    )
            )

        @Test
        fun `사용 가능한 아이디`() {
            whenever(checkMemberIdUseCase.checkAvailability(any())).thenReturn(true)

            val request = CheckMemberIdRequest(memberId = "availableid")

            val documentFilter = document("member/check-id", 200)
                .request(request().applyConfig(apiConfig))
                .response(checkIdResponseFields())
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
                .response(checkIdResponseFields())
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
