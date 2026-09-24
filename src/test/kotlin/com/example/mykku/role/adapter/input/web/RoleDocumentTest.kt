package com.example.mykku.role.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.docs.RestDocumentationResponse
import com.example.mykku.role.application.dto.ChangeRepresentativeRoleCommand
import com.example.mykku.role.application.dto.MemberRoleResult
import com.example.mykku.role.application.dto.RoleResult
import com.example.mykku.role.exception.RoleErrorCode
import com.example.mykku.role.exception.RoleException
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import java.time.LocalDateTime

class RoleDocumentTest : BaseDocumentTest() {

    @Nested
    @DisplayName("내 칭호 목록 조회")
    inner class GetMyRoles {

        private val apiConfig = ApiRequestConfig(
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val memberRoles = listOf(
                MemberRoleResult(
                    id = 12L,
                    role = RoleResult(id = 1L, name = "첫 만남", description = "로그인 하면 무조건 줌"),
                    isRepresentative = true,
                    earnedAt = LocalDateTime.now()
                ),
                MemberRoleResult(
                    id = 15L,
                    role = RoleResult(id = 11L, name = "이 몸 등장", description = "게시글 1회 업로드"),
                    isRepresentative = false,
                    earnedAt = LocalDateTime.now()
                )
            )

            `when`(getMyRolesUseCase.getMyRoles(any<Long>(), anyOrNull())).thenReturn(memberRoles)

            val documentFilter = document("role/my-roles", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data[]").type(JsonFieldType.ARRAY)
                                .description("보유 칭호 목록 (정렬 순서 보장 안 됨, 보유 칭호가 없으면 빈 배열)"),
                            fieldWithPath("data[].id").type(JsonFieldType.NUMBER)
                                .description(MEMBER_ROLE_ID_DESCRIPTION),
                            fieldWithPath("data[].role").type(JsonFieldType.OBJECT).description("칭호 정보"),
                            fieldWithPath("data[].role.id").type(JsonFieldType.NUMBER)
                                .description("칭호 ID (전체 칭호 목록 조회의 `data[].id` 와 같은 값)"),
                            fieldWithPath("data[].role.name").type(JsonFieldType.STRING).description("칭호 이름"),
                            fieldWithPath("data[].role.description").type(JsonFieldType.STRING)
                                .description(ROLE_DESCRIPTION_DESCRIPTION)
                                .optional(),
                            fieldWithPath("data[].earnedAt").type(JsonFieldType.STRING)
                                .description("칭호 획득 일시 (KST, ISO-8601, 오프셋 없음)"),
                            fieldWithPath("data[].isRepresentative").type(JsonFieldType.BOOLEAN)
                                .description(IS_REPRESENTATIVE_DESCRIPTION)
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/roles/me")
                .then()
                .statusCode(200)
        }
    }

    @Nested
    @DisplayName("전체 칭호 목록 조회")
    inner class GetRoles {

        private val apiConfig = ApiRequestConfig(
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val roles = listOf(
                RoleResult(id = 1L, name = "첫 만남", description = "로그인 하면 무조건 줌"),
                RoleResult(id = 11L, name = "이 몸 등장", description = "게시글 1회 업로드")
            )

            `when`(getRolesUseCase.getRoles()).thenReturn(roles)

            val documentFilter = document("role/roles", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data[]").type(JsonFieldType.ARRAY)
                                .description("전체 칭호 목록 (미획득 칭호 포함, 정렬 순서 보장 안 됨)"),
                            fieldWithPath("data[].id").type(JsonFieldType.NUMBER)
                                .description("칭호 ID (내 칭호 목록 조회의 `data[].role.id` 와 같은 값)"),
                            fieldWithPath("data[].name").type(JsonFieldType.STRING).description("칭호 이름"),
                            fieldWithPath("data[].description").type(JsonFieldType.STRING)
                                .description(ROLE_DESCRIPTION_DESCRIPTION)
                                .optional()
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/roles")
                .then()
                .statusCode(200)
        }
    }

    @Nested
    @DisplayName("새로 획득한 칭호 조회")
    inner class GetNewRoles {

        private val apiConfig = ApiRequestConfig(
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val newRoles = listOf(
                MemberRoleResult(
                    id = 15L,
                    role = RoleResult(id = 11L, name = "이 몸 등장", description = "게시글 1회 업로드"),
                    isRepresentative = false,
                    earnedAt = LocalDateTime.now()
                )
            )

            `when`(getNewRolesUseCase.getNewRoles(any<Long>(), anyOrNull())).thenReturn(newRoles)

            val documentFilter = document("role/new-roles", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data[]").type(JsonFieldType.ARRAY)
                                .description("새로 획득한 칭호 목록 (획득 순, 없으면 빈 배열)"),
                            fieldWithPath("data[].id").type(JsonFieldType.NUMBER)
                                .description(MEMBER_ROLE_ID_DESCRIPTION),
                            fieldWithPath("data[].role").type(JsonFieldType.OBJECT).description("칭호 정보"),
                            fieldWithPath("data[].role.id").type(JsonFieldType.NUMBER)
                                .description("칭호 ID (전체 칭호 목록 조회의 `data[].id` 와 같은 값)"),
                            fieldWithPath("data[].role.name").type(JsonFieldType.STRING).description("칭호 이름"),
                            fieldWithPath("data[].role.description").type(JsonFieldType.STRING)
                                .description(ROLE_DESCRIPTION_DESCRIPTION)
                                .optional(),
                            fieldWithPath("data[].earnedAt").type(JsonFieldType.STRING)
                                .description("칭호 획득 일시 (KST, ISO-8601, 오프셋 없음)"),
                            fieldWithPath("data[].isRepresentative").type(JsonFieldType.BOOLEAN)
                                .description(IS_REPRESENTATIVE_DESCRIPTION)
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/roles/me/new")
                .then()
                .statusCode(200)
        }
    }

    @Nested
    @DisplayName("대표 칭호 변경")
    inner class ChangeRepresentativeRole {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("memberRoleId")
                    .description("대표로 설정할 보유 칭호 ID. 내 칭호 목록 조회 응답의 `data[].id` 값 (칭호 ID 아님)")
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val memberRoleId = 12L

            doNothing()
                .`when`(changeRepresentativeRoleUseCase)
                .changeRepresentativeRole(any<ChangeRepresentativeRoleCommand>())

            val documentFilter = document("role/change-representative", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("항상 빈 객체 `{}` (사용하지 않음)")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .patch("/api/v1/roles/{memberRoleId}/representative", memberRoleId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `보유하지 않은 칭호`() {
            val memberRoleId = 999L

            doThrow(RoleException(RoleErrorCode.MEMBER_ROLE_NOT_FOUND))
                .`when`(changeRepresentativeRoleUseCase)
                .changeRepresentativeRole(any<ChangeRepresentativeRoleCommand>())

            val documentFilter = document("role/change-representative", "MEMBER_ROLE_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .patch("/api/v1/roles/{memberRoleId}/representative", memberRoleId)
                .then()
                .statusCode(404)
        }

        @Test
        fun `다른 회원의 보유 칭호`() {
            val memberRoleId = 20L

            doThrow(RoleException(RoleErrorCode.MEMBER_ROLE_UNAUTHORIZED))
                .`when`(changeRepresentativeRoleUseCase)
                .changeRepresentativeRole(any<ChangeRepresentativeRoleCommand>())

            val documentFilter = document("role/change-representative", "MEMBER_ROLE_UNAUTHORIZED")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .patch("/api/v1/roles/{memberRoleId}/representative", memberRoleId)
                .then()
                .statusCode(403)
        }
    }

    companion object {
        private const val MEMBER_ROLE_ID_DESCRIPTION =
            "보유 칭호 ID (회원-칭호 매핑 ID). 대표 칭호 변경 API의 `memberRoleId` 로 사용"
        private const val ROLE_DESCRIPTION_DESCRIPTION =
            "칭호 설명. 자동 부여 칭호는 획득 조건 안내 문구 (예: `게시글 1회 업로드`). 설명 없이 등록된 칭호는 null"
        private const val IS_REPRESENTATIVE_DESCRIPTION =
            "현재 대표 칭호인지 여부. 대표 칭호는 최대 1개이며, 대표 칭호가 없는 회원은 모두 false"
    }
}
