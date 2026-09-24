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
                fieldWithPath("memberId").type(JsonFieldType.STRING).description("차단할 사용자 ID")
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val request = BlockMemberRequest(memberId = "blocked-user-id")
            val result = MemberBlockResult(
                id = 1L,
                blockedMemberId = "blocked-user-id",
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
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("차단 ID"),
                            fieldWithPath("data.blockedMemberId").type(JsonFieldType.STRING).description("차단된 사용자 ID"),
                            fieldWithPath("data.blockedMemberNickname").type(JsonFieldType.STRING).description("차단된 사용자 닉네임"),
                            fieldWithPath("data.blockedMemberProfileImage").type(JsonFieldType.STRING).description("차단된 사용자 프로필 이미지").optional(),
                            fieldWithPath("data.blockedAt").type(JsonFieldType.STRING).description("차단 시간")
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
            val request = BlockMemberRequest(memberId = "blocked-user-id")

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
    }

    @Nested
    @DisplayName("사용자 차단 해제")
    inner class UnblockMember {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("memberId").description("차단 해제할 사용자 ID")
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val memberId = "blocked-user-id"

            val documentFilter = document("block/member/delete", 200)
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
                .`when`()
                .delete("/api/v1/blocks/members/{memberId}", memberId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `차단 정보를 찾을 수 없음`() {
            val memberId = "not-blocked-user-id"

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
    }

    @Nested
    @DisplayName("차단한 사용자 목록 조회")
    inner class GetMemberBlocks {

        private val apiConfig = ApiRequestConfig(
            queryParameters = listOf(
                parameterWithName("page").description("페이지 번호 (0부터 시작)").optional(),
                parameterWithName("size").description("페이지 크기 (기본값: 20)").optional()
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val result = MemberBlockListResult(
                blocks = listOf(
                    MemberBlockResult(
                        id = 1L,
                        blockedMemberId = "blocked1",
                        blockedMemberNickname = "유저1",
                        blockedMemberProfileImage = "https://example.com/profile1.jpg",
                        blockedAt = LocalDateTime.now()
                    ),
                    MemberBlockResult(
                        id = 2L,
                        blockedMemberId = "blocked2",
                        blockedMemberNickname = "유저2",
                        blockedMemberProfileImage = "",
                        blockedAt = LocalDateTime.now()
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
                            fieldWithPath("data.blocks[]").type(JsonFieldType.ARRAY).description("차단 목록"),
                            fieldWithPath("data.blocks[].id").type(JsonFieldType.NUMBER).description("차단 ID"),
                            fieldWithPath("data.blocks[].blockedMemberId").type(JsonFieldType.STRING).description("차단된 사용자 ID"),
                            fieldWithPath("data.blocks[].blockedMemberNickname").type(JsonFieldType.STRING).description("차단된 사용자 닉네임"),
                            fieldWithPath("data.blocks[].blockedMemberProfileImage").type(JsonFieldType.STRING).description("차단된 사용자 프로필 이미지").optional(),
                            fieldWithPath("data.blocks[].blockedAt").type(JsonFieldType.STRING).description("차단 시간"),
                            fieldWithPath("data.totalCount").type(JsonFieldType.NUMBER).description("총 차단 수"),
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
                fieldWithPath("keyword").type(JsonFieldType.STRING).description("차단할 키워드 (최대 50자)")
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
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("키워드 차단 ID"),
                            fieldWithPath("data.keyword").type(JsonFieldType.STRING).description("차단된 키워드"),
                            fieldWithPath("data.blockedAt").type(JsonFieldType.STRING).description("차단 시간")
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
                parameterWithName("keyword").description("차단 해제할 키워드")
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
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터").optional()
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
                parameterWithName("page").description("페이지 번호 (0부터 시작)").optional(),
                parameterWithName("size").description("페이지 크기 (기본값: 20)").optional()
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val result = KeywordBlockListResult(
                blocks = listOf(
                    KeywordBlockResult(
                        id = 1L,
                        keyword = "스포일러",
                        blockedAt = LocalDateTime.now()
                    ),
                    KeywordBlockResult(
                        id = 2L,
                        keyword = "광고",
                        blockedAt = LocalDateTime.now()
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
                            fieldWithPath("data.blocks[]").type(JsonFieldType.ARRAY).description("키워드 차단 목록"),
                            fieldWithPath("data.blocks[].id").type(JsonFieldType.NUMBER).description("키워드 차단 ID"),
                            fieldWithPath("data.blocks[].keyword").type(JsonFieldType.STRING).description("차단된 키워드"),
                            fieldWithPath("data.blocks[].blockedAt").type(JsonFieldType.STRING).description("차단 시간"),
                            fieldWithPath("data.totalCount").type(JsonFieldType.NUMBER).description("총 키워드 차단 수"),
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
