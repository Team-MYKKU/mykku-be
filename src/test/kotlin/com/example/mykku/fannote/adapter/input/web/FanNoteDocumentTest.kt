package com.example.mykku.fannote.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.docs.RestDocumentationResponse
import com.example.mykku.fannote.application.dto.FanNoteDetailResult
import com.example.mykku.fannote.application.dto.FanNoteListResult
import com.example.mykku.fannote.application.dto.FanNotePageResult
import com.example.mykku.fannote.exception.FanNoteErrorCode
import com.example.mykku.fannote.exception.FanNoteException
import java.time.LocalDate
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName

class FanNoteDocumentTest : BaseDocumentTest() {

    @Nested
    @DisplayName("덕질노트 목록 조회")
    inner class GetFanNoteList {

        private val apiConfig = ApiRequestConfig(
            queryParameters = listOf(
                parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0)").optional(),
                parameterWithName("size").description("페이지 크기 (기본값: 20)").optional()
            )
        )

        @Test
        fun `성공`() {
            val fanNoteList = listOf(
                FanNoteListResult(
                    id = 1L,
                    title = "첫 번째 덕질노트",
                    subtitle = "서브타이틀 1",
                    content = "덕질노트 내용입니다",
                    productionDate = LocalDate.of(2024, 1, 15),
                    coverImageUrl = "https://s3.amazonaws.com/mykku/covers/cover1.jpg"
                ),
                FanNoteListResult(
                    id = 2L,
                    title = "두 번째 덕질노트",
                    subtitle = "서브타이틀 2",
                    content = "또 다른 덕질노트 내용",
                    productionDate = LocalDate.of(2024, 1, 10),
                    coverImageUrl = "https://s3.amazonaws.com/mykku/covers/cover2.jpg"
                )
            )
            val fanNotePage = PageImpl(fanNoteList, PageRequest.of(0, 20), fanNoteList.size.toLong())

            `when`(getFanNoteListUseCase.execute(any())).thenReturn(fanNotePage)

            val documentFilter = document("fan-note/list", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("페이지 응답 데이터"),
                            fieldWithPath("data.content[]").type(JsonFieldType.ARRAY).description("덕질노트 목록"),
                            fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("덕질노트 ID"),
                            fieldWithPath("data.content[].title").type(JsonFieldType.STRING).description("덕질노트 제목"),
                            fieldWithPath("data.content[].subtitle").type(JsonFieldType.STRING).description("서브 제목")
                                .optional(),
                            fieldWithPath("data.content[].content").type(JsonFieldType.STRING).description("덕질노트 내용")
                                .optional(),
                            fieldWithPath("data.content[].productionDate").type(JsonFieldType.STRING)
                                .description("제작 날짜 (yyyy-MM-dd)"),
                            fieldWithPath("data.content[].coverImageUrl").type(JsonFieldType.STRING)
                                .description("표지 이미지 URL").optional(),
                            fieldWithPath("data.pageable").type(JsonFieldType.OBJECT).description("페이지 정보"),
                            fieldWithPath("data.pageable.pageNumber").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                            fieldWithPath("data.pageable.pageSize").type(JsonFieldType.NUMBER).description("페이지 크기"),
                            fieldWithPath("data.pageable.sort").type(JsonFieldType.OBJECT).description("정렬 정보"),
                            fieldWithPath("data.pageable.sort.empty").type(JsonFieldType.BOOLEAN).description("정렬 비어있음 여부"),
                            fieldWithPath("data.pageable.sort.sorted").type(JsonFieldType.BOOLEAN).description("정렬됨 여부"),
                            fieldWithPath("data.pageable.sort.unsorted").type(JsonFieldType.BOOLEAN)
                                .description("정렬되지 않음 여부"),
                            fieldWithPath("data.pageable.offset").type(JsonFieldType.NUMBER).description("오프셋"),
                            fieldWithPath("data.pageable.paged").type(JsonFieldType.BOOLEAN).description("페이징 여부"),
                            fieldWithPath("data.pageable.unpaged").type(JsonFieldType.BOOLEAN).description("페이징되지 않음 여부"),
                            fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
                            fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                            fieldWithPath("data.last").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부"),
                            fieldWithPath("data.first").type(JsonFieldType.BOOLEAN).description("첫 페이지 여부"),
                            fieldWithPath("data.number").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                            fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                            fieldWithPath("data.numberOfElements").type(JsonFieldType.NUMBER).description("현재 페이지 요소 수"),
                            fieldWithPath("data.empty").type(JsonFieldType.BOOLEAN).description("빈 페이지 여부"),
                            fieldWithPath("data.sort").type(JsonFieldType.OBJECT).description("정렬 정보"),
                            fieldWithPath("data.sort.empty").type(JsonFieldType.BOOLEAN).description("정렬 비어있음 여부"),
                            fieldWithPath("data.sort.sorted").type(JsonFieldType.BOOLEAN).description("정렬됨 여부"),
                            fieldWithPath("data.sort.unsorted").type(JsonFieldType.BOOLEAN).description("정렬되지 않음 여부")
                        )
                )
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .param("page", "0")
                .param("size", "20")
                .`when`()
                .get("/api/v1/fan-notes")
                .then()
                .statusCode(200)
        }
    }

    @Nested
    @DisplayName("덕질노트 상세 조회")
    inner class GetFanNoteDetail {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("fanNoteId").description("조회할 덕질노트 ID")
            )
        )

        @Test
        fun `성공`() {
            val fanNoteId = 1L
            val fanNoteDetail = FanNoteDetailResult(
                id = fanNoteId,
                title = "웹툰 스타일 덕질노트",
                subtitle = "좌우로 넘기면서 보는 만화",
                content = "네이버 웹툰처럼 옆으로 넘기면서 볼 수 있는 덕질 콘텐츠입니다",
                productionDate = LocalDate.of(2024, 1, 15),
                coverImageUrl = "https://s3.amazonaws.com/mykku/covers/cover1.jpg",
                pages = listOf(
                    FanNotePageResult(1, "https://s3.amazonaws.com/mykku/pages/page1.jpg"),
                    FanNotePageResult(2, "https://s3.amazonaws.com/mykku/pages/page2.jpg"),
                    FanNotePageResult(3, "https://s3.amazonaws.com/mykku/pages/page3.jpg")
                )
            )

            `when`(getFanNoteDetailUseCase.execute(eq(fanNoteId), anyOrNull())).thenReturn(fanNoteDetail)

            val documentFilter = document("fan-note/detail", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("덕질노트 상세 정보"),
                            fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("덕질노트 ID"),
                            fieldWithPath("data.title").type(JsonFieldType.STRING).description("덕질노트 제목"),
                            fieldWithPath("data.subtitle").type(JsonFieldType.STRING).description("서브 제목").optional(),
                            fieldWithPath("data.content").type(JsonFieldType.STRING).description("덕질노트 내용").optional(),
                            fieldWithPath("data.productionDate").type(JsonFieldType.STRING)
                                .description("제작 날짜 (yyyy-MM-dd)"),
                            fieldWithPath("data.coverImageUrl").type(JsonFieldType.STRING).description("표지 이미지 URL")
                                .optional(),
                            fieldWithPath("data.pages[]").type(JsonFieldType.ARRAY).description("페이지 목록"),
                            fieldWithPath("data.pages[].pageNumber").type(JsonFieldType.NUMBER).description("페이지 번호"),
                            fieldWithPath("data.pages[].imageUrl").type(JsonFieldType.STRING).description("페이지 이미지 URL")
                        )
                )
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/fan-notes/{fanNoteId}", fanNoteId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `존재하지 않는 덕질노트`() {
            val fanNoteId = 999L

            `when`(getFanNoteDetailUseCase.execute(eq(fanNoteId), anyOrNull()))
                .thenThrow(FanNoteException(FanNoteErrorCode.FAN_NOTE_NOT_FOUND))

            val documentFilter = document("fan-note/detail", "FAN_NOTE_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/fan-notes/{fanNoteId}", fanNoteId)
                .then()
                .statusCode(404)
        }
    }
}
