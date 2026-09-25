package com.example.mykku.block.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.block.application.dto.KeywordBlockListResult
import com.example.mykku.block.application.dto.KeywordBlockResult
import com.example.mykku.block.application.dto.MemberBlockListResult
import com.example.mykku.block.application.dto.MemberBlockResult
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.docs.RestDocumentationResponse
import com.example.mykku.block.exception.BlockErrorCode
import com.example.mykku.block.exception.BlockException
import com.example.mykku.member.exception.MemberErrorCode
import com.example.mykku.member.exception.MemberException
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import java.time.LocalDateTime

class BlockDocumentTest : BaseDocumentTest() {

    @Nested
    @DisplayName("사용자 차단")
    inner class BlockMember {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("memberId").type(JsonFieldType.STRING)
                    .description(
                        "차단할 사용자의 아이디(memberId, 영문·숫자 최대 16자 문자열). 숫자 PK나 차단 ID가 아님. " +
                            "해당 회원이 없으면 404 MB001, 본인 아이디면 400 BL101"
                    )
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val request = BlockMemberRequest(memberId = "blockeduser1")
            val result = MemberBlockResult(
                id = 1L,
                blockedMemberId = "blockeduser1",
                blockedMemberNickname = "차단유저",
                blockedMemberProfileImage = "https://example.com/profile.jpg",
                blockedAt = LocalDateTime.now()
            )

            `when`(blockMemberUseCase.blockMember(any())).thenReturn(result)

            val documentFilter = document("block/member/create", 201)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("생성된 차단 정보"),
                            fieldWithPath("data.id").type(JsonFieldType.NUMBER)
                                .description("차단 기록 ID (차단 해제 등 다른 API에는 사용하지 않음)"),
                            fieldWithPath("data.blockedMemberId").type(JsonFieldType.STRING)
                                .description("차단된 사용자의 아이디(memberId). 차단 해제 API 경로에 이 값을 사용"),
                            fieldWithPath("data.blockedMemberNickname").type(JsonFieldType.STRING)
                                .description("차단된 사용자 닉네임 (닉네임을 아직 설정하지 않은 회원이면 null)")
                                .optional(),
                            fieldWithPath("data.blockedMemberProfileImage").type(JsonFieldType.STRING)
                                .description("차단된 사용자 프로필 이미지 URL (설정하지 않은 회원은 null이 아닌 빈 문자열 \"\")"),
                            fieldWithPath("data.blockedAt").type(JsonFieldType.STRING)
                                .description(
                                    "차단 일시 (KST, ISO-8601, 오프셋 없음). " +
                                        "생성 응답에는 소수점 이하 초가 붙을 수 있음(자릿수 가변)"
                                )
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/blocks/members")
                .then()
                .statusCode(201)
        }

        @Test
        fun `자기 자신 차단 에러`() {
            val request = BlockMemberRequest(memberId = testMember.memberId!!)

            `when`(blockMemberUseCase.blockMember(any()))
                .thenThrow(BlockException(BlockErrorCode.CANNOT_BLOCK_SELF))

            val documentFilter = document("block/member/create", "CANNOT_BLOCK_SELF")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/blocks/members")
                .then()
                .statusCode(400)
        }

        @Test
        fun `이미 차단된 사용자 에러`() {
            val request = BlockMemberRequest(memberId = "blockeduser1")

            `when`(blockMemberUseCase.blockMember(any()))
                .thenThrow(BlockException(BlockErrorCode.MEMBER_ALREADY_BLOCKED))

            val documentFilter = document("block/member/create", "MEMBER_ALREADY_BLOCKED")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/blocks/members")
                .then()
                .statusCode(409)
        }

        @Test
        fun `존재하지 않는 사용자 에러`() {
            val request = BlockMemberRequest(memberId = "unknownuser")

            `when`(blockMemberUseCase.blockMember(any()))
                .thenThrow(MemberException(MemberErrorCode.MEMBER_NOT_FOUND))

            val documentFilter = document("block/member/create", "MEMBER_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/blocks/members")
                .then()
                .statusCode(404)
        }
    }

    @Nested
    @DisplayName("사용자 차단 해제")
    inner class UnblockMember {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("memberId")
                    .description(
                        "차단 해제할 사용자의 아이디(memberId, 문자열). 차단 목록 응답의 blockedMemberId 값을 " +
                            "그대로 사용하며, 차단 기록 ID(id)가 아님"
                    )
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val memberId = "blockeduser1"

            val documentFilter = document("block/member/delete", 200)
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
                .`when`()
                .delete("/api/v1/blocks/members/{memberId}", memberId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `차단 정보를 찾을 수 없음`() {
            val memberId = "notblocked1"

            `when`(unblockMemberUseCase.unblockMember(any()))
                .thenThrow(BlockException(BlockErrorCode.MEMBER_BLOCK_NOT_FOUND))

            val documentFilter = document("block/member/delete", "MEMBER_BLOCK_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .`when`()
                .delete("/api/v1/blocks/members/{memberId}", memberId)
                .then()
                .statusCode(404)
        }

        @Test
        fun `존재하지 않는 사용자 에러`() {
            val memberId = "unknownuser"

            `when`(unblockMemberUseCase.unblockMember(any()))
                .thenThrow(MemberException(MemberErrorCode.MEMBER_NOT_FOUND))

            val documentFilter = document("block/member/delete", "MEMBER_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .`when`()
                .delete("/api/v1/blocks/members/{memberId}", memberId)
                .then()
                .statusCode(404)
        }
    }

    @Nested
    @DisplayName("차단한 사용자 목록 조회")
    inner class GetMemberBlocks {

        private val apiConfig = ApiRequestConfig(
            queryParameters = listOf(
                parameterWithName("page").description("페이지 번호 (0부터 시작, 0 이상, 기본값: 0)").optional(),
                parameterWithName("size").description("페이지 크기 (1~1000, 기본값: 20)").optional()
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val result = MemberBlockListResult(
                blocks = listOf(
                    MemberBlockResult(
                        id = 2L,
                        blockedMemberId = "blocked2",
                        blockedMemberNickname = "유저2",
                        blockedMemberProfileImage = "https://example.com/profile2.jpg",
                        blockedAt = LocalDateTime.of(2026, 9, 24, 14, 15, 33)
                    ),
                    MemberBlockResult(
                        id = 1L,
                        blockedMemberId = "blocked1",
                        blockedMemberNickname = "유저1",
                        blockedMemberProfileImage = "",
                        blockedAt = LocalDateTime.of(2026, 9, 20, 9, 30, 0)
                    )
                ),
                totalCount = 2,
                hasNext = false
            )

            `when`(getMemberBlocksUseCase.getMemberBlocks(any())).thenReturn(result)

            val documentFilter = document("block/member/list", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.blocks[]").type(JsonFieldType.ARRAY)
                                .description("차단한 사용자 목록 (최근 차단순)"),
                            fieldWithPath("data.blocks[].id").type(JsonFieldType.NUMBER)
                                .description("차단 기록 ID (차단 해제 등 다른 API에는 사용하지 않음)"),
                            fieldWithPath("data.blocks[].blockedMemberId").type(JsonFieldType.STRING)
                                .description("차단된 사용자의 현재 아이디(memberId, 조회 시점 기준). 차단 해제 API 경로에 이 값을 사용"),
                            fieldWithPath("data.blocks[].blockedMemberNickname").type(JsonFieldType.STRING)
                                .description("차단된 사용자 닉네임 (닉네임을 아직 설정하지 않은 회원이면 빈 문자열 \"\")"),
                            fieldWithPath("data.blocks[].blockedMemberProfileImage").type(JsonFieldType.STRING)
                                .description("차단된 사용자 프로필 이미지 URL (설정하지 않은 회원은 null이 아닌 빈 문자열 \"\")"),
                            fieldWithPath("data.blocks[].blockedAt").type(JsonFieldType.STRING)
                                .description("차단 일시 (KST, ISO-8601, 오프셋 없음). 목록 값은 초 단위까지이며 소수점 이하 없음"),
                            fieldWithPath("data.totalCount").type(JsonFieldType.NUMBER)
                                .description("내가 차단한 사용자 전체 수 (현재 페이지와 무관)"),
                            fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .`when`()
                .get("/api/v1/blocks/members")
                .then()
                .statusCode(200)
        }
    }

    @Nested
    @DisplayName("키워드 차단")
    inner class BlockKeyword {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("keyword").type(JsonFieldType.STRING)
                    .description(
                        "차단할 키워드. 앞뒤 공백을 제거하고 소문자로 바꾼 값으로 검증·저장(응답의 keyword는 이 값). " +
                            "공백만으로는 등록할 수 없고, 정규화 후 1~50자(UTF-16 코드 유닛 기준, 대부분의 이모지는 1개가 2자 이상). " +
                            "대소문자만 다른 키워드는 같은 키워드로 취급. 사용자당 최대 100개"
                    )
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val request = BlockKeywordRequest(keyword = "스포일러")
            val result = KeywordBlockResult(
                id = 1L,
                keyword = "스포일러",
                blockedAt = LocalDateTime.now()
            )

            `when`(blockKeywordUseCase.blockKeyword(any())).thenReturn(result)

            val documentFilter = document("block/keyword/create", 201)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("생성된 키워드 차단 정보"),
                            fieldWithPath("data.id").type(JsonFieldType.NUMBER)
                                .description("키워드 차단 기록 ID (차단 해제 등 다른 API에는 사용하지 않음)"),
                            fieldWithPath("data.keyword").type(JsonFieldType.STRING)
                                .description("차단된 키워드 (정규화된 값: 앞뒤 공백 제거, 소문자)"),
                            fieldWithPath("data.blockedAt").type(JsonFieldType.STRING)
                                .description(
                                    "차단 일시 (KST, ISO-8601, 오프셋 없음). " +
                                        "생성 응답에는 소수점 이하 초가 붙을 수 있음(자릿수 가변)"
                                )
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/blocks/keywords")
                .then()
                .statusCode(201)
        }

        @Test
        fun `빈 키워드 에러`() {
            val request = BlockKeywordRequest(keyword = "   ")

            `when`(blockKeywordUseCase.blockKeyword(any()))
                .thenThrow(BlockException(BlockErrorCode.KEYWORD_EMPTY))

            val documentFilter = document("block/keyword/create", "KEYWORD_EMPTY")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/blocks/keywords")
                .then()
                .statusCode(400)
        }

        @Test
        fun `키워드 길이 초과 에러`() {
            val request = BlockKeywordRequest(keyword = "a".repeat(51))

            `when`(blockKeywordUseCase.blockKeyword(any()))
                .thenThrow(BlockException(BlockErrorCode.KEYWORD_TOO_LONG))

            val documentFilter = document("block/keyword/create", "KEYWORD_TOO_LONG")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/blocks/keywords")
                .then()
                .statusCode(400)
        }

        @Test
        fun `이미 차단된 키워드 에러`() {
            val request = BlockKeywordRequest(keyword = "스포일러")

            `when`(blockKeywordUseCase.blockKeyword(any()))
                .thenThrow(BlockException(BlockErrorCode.KEYWORD_ALREADY_BLOCKED))

            val documentFilter = document("block/keyword/create", "KEYWORD_ALREADY_BLOCKED")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/blocks/keywords")
                .then()
                .statusCode(409)
        }

        @Test
        fun `키워드 제한 초과 에러`() {
            val request = BlockKeywordRequest(keyword = "새키워드")

            `when`(blockKeywordUseCase.blockKeyword(any()))
                .thenThrow(BlockException(BlockErrorCode.KEYWORD_LIMIT_EXCEEDED))

            val documentFilter = document("block/keyword/create", "KEYWORD_LIMIT_EXCEEDED")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/blocks/keywords")
                .then()
                .statusCode(400)
        }
    }

    @Nested
    @DisplayName("키워드 차단 해제")
    inner class UnblockKeyword {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("keyword")
                    .description(
                        "차단 해제할 키워드. 앞뒤 공백 제거·소문자 변환 후 비교하므로 대소문자를 구분하지 않음. " +
                            "한글·공백·특수문자는 URL 인코딩해서 전송"
                    )
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val keyword = "스포일러"

            val documentFilter = document("block/keyword/delete", 200)
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
                .`when`()
                .delete("/api/v1/blocks/keywords/{keyword}", keyword)
                .then()
                .statusCode(200)
        }

        @Test
        fun `키워드 차단 정보를 찾을 수 없음`() {
            val keyword = "not-blocked-keyword"

            `when`(unblockKeywordUseCase.unblockKeyword(any()))
                .thenThrow(BlockException(BlockErrorCode.KEYWORD_BLOCK_NOT_FOUND))

            val documentFilter = document("block/keyword/delete", "KEYWORD_BLOCK_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .`when`()
                .delete("/api/v1/blocks/keywords/{keyword}", keyword)
                .then()
                .statusCode(404)
        }
    }

    @Nested
    @DisplayName("차단한 키워드 목록 조회")
    inner class GetKeywordBlocks {

        private val apiConfig = ApiRequestConfig(
            queryParameters = listOf(
                parameterWithName("page").description("페이지 번호 (0부터 시작, 0 이상, 기본값: 0)").optional(),
                parameterWithName("size").description("페이지 크기 (1~1000, 기본값: 20)").optional()
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val result = KeywordBlockListResult(
                blocks = listOf(
                    KeywordBlockResult(
                        id = 2L,
                        keyword = "광고",
                        blockedAt = LocalDateTime.of(2026, 9, 24, 14, 15, 33)
                    ),
                    KeywordBlockResult(
                        id = 1L,
                        keyword = "스포일러",
                        blockedAt = LocalDateTime.of(2026, 9, 20, 9, 30, 0)
                    )
                ),
                totalCount = 2,
                hasNext = false
            )

            `when`(getKeywordBlocksUseCase.getKeywordBlocks(any())).thenReturn(result)

            val documentFilter = document("block/keyword/list", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.blocks[]").type(JsonFieldType.ARRAY)
                                .description("차단한 키워드 목록 (최근 차단순)"),
                            fieldWithPath("data.blocks[].id").type(JsonFieldType.NUMBER)
                                .description("키워드 차단 기록 ID (차단 해제 등 다른 API에는 사용하지 않음)"),
                            fieldWithPath("data.blocks[].keyword").type(JsonFieldType.STRING)
                                .description("차단된 키워드 (정규화된 값: 앞뒤 공백 제거, 소문자). 차단 해제 API 경로에 이 값을 사용"),
                            fieldWithPath("data.blocks[].blockedAt").type(JsonFieldType.STRING)
                                .description("차단 일시 (KST, ISO-8601, 오프셋 없음). 목록 값은 초 단위까지이며 소수점 이하 없음"),
                            fieldWithPath("data.totalCount").type(JsonFieldType.NUMBER)
                                .description("내가 차단한 키워드 전체 개수 (현재 페이지와 무관, 최대 100)"),
                            fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .`when`()
                .get("/api/v1/blocks/keywords")
                .then()
                .statusCode(200)
        }
    }
}
