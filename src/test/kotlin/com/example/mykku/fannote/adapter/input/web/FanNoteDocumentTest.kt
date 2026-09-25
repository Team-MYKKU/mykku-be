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
import org.springframework.data.domain.Sort
import org.springframework.http.HttpHeaders
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName

class FanNoteDocumentTest : BaseDocumentTest() {

    @Nested
    @DisplayName("덕질노트 목록 조회")
    inner class GetFanNoteList {

        private val apiConfig = ApiRequestConfig(
            queryParameters = listOf(
                parameterWithName("page").description("페이지 번호 (0부터 시작하는 정수, 0 이상, 기본값: 0)").optional(),
                parameterWithName("size").description("페이지 크기 (1 이상 1000 이하 정수, 기본값: 20)").optional()
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
            val pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "productionDate"))
            val fanNotePage = PageImpl(fanNoteList, pageRequest, fanNoteList.size.toLong())

            `when`(getFanNoteListUseCase.execute(any())).thenReturn(fanNotePage)

            val documentFilter = document("fan-note/list", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("페이지 응답 데이터"),
                            fieldWithPath("data.content[]").type(JsonFieldType.ARRAY)
                                .description("덕질노트 목록 (제작일 최신순, 범위를 넘는 page면 빈 배열)"),
                            fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER)
                                .description("덕질노트 ID (상세 조회의 fanNoteId로 사용)"),
                            fieldWithPath("data.content[].title").type(JsonFieldType.STRING).description("덕질노트 제목"),
                            fieldWithPath("data.content[].subtitle").type(JsonFieldType.STRING)
                                .description("서브 제목 (없으면 null 또는 빈 문자열 \"\")").optional(),
                            fieldWithPath("data.content[].content").type(JsonFieldType.STRING)
                                .description(
                                    "덕질노트 본문 전체 (요약이 아닌 상세 조회와 같은 원문이며 길이 제한 없음, " +
                                        "없으면 null 또는 빈 문자열 \"\")"
                                )
                                .optional(),
                            fieldWithPath("data.content[].productionDate").type(JsonFieldType.STRING)
                                .description("제작 날짜 (yyyy-MM-dd)"),
                            fieldWithPath("data.content[].coverImageUrl").type(JsonFieldType.STRING)
                                .description("표지 이미지 URL (표지 미등록 시 null)").optional(),
                            fieldWithPath("data.pageable").type(JsonFieldType.OBJECT).description("요청한 페이지 정보"),
                            fieldWithPath("data.pageable.pageNumber").type(JsonFieldType.NUMBER)
                                .description("현재 페이지 번호 (data.number와 동일)"),
                            fieldWithPath("data.pageable.pageSize").type(JsonFieldType.NUMBER)
                                .description("요청한 페이지 크기 (data.size와 동일)"),
                            fieldWithPath("data.pageable.sort").type(JsonFieldType.OBJECT)
                                .description("정렬 정보 (data.sort와 동일)"),
                            fieldWithPath("data.pageable.sort.empty").type(JsonFieldType.BOOLEAN)
                                .description("정렬 조건이 없으면 true (이 API는 항상 false)"),
                            fieldWithPath("data.pageable.sort.sorted").type(JsonFieldType.BOOLEAN)
                                .description("정렬 적용 여부 (이 API는 항상 true)"),
                            fieldWithPath("data.pageable.sort.unsorted").type(JsonFieldType.BOOLEAN)
                                .description("정렬 미적용 여부 (이 API는 항상 false)"),
                            fieldWithPath("data.pageable.offset").type(JsonFieldType.NUMBER)
                                .description("건너뛴 요소 수 (page × size)"),
                            fieldWithPath("data.pageable.paged").type(JsonFieldType.BOOLEAN)
                                .description("페이징 적용 여부 (항상 true)"),
                            fieldWithPath("data.pageable.unpaged").type(JsonFieldType.BOOLEAN)
                                .description("페이징 미적용 여부 (항상 false)"),
                            fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 덕질노트 수"),
                            fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                            fieldWithPath("data.last").type(JsonFieldType.BOOLEAN)
                                .description("마지막 페이지 여부 (true면 다음 페이지를 요청할 필요 없음)"),
                            fieldWithPath("data.first").type(JsonFieldType.BOOLEAN).description("첫 페이지 여부"),
                            fieldWithPath("data.number").type(JsonFieldType.NUMBER).description("현재 페이지 번호 (0부터 시작)"),
                            fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("요청한 페이지 크기"),
                            fieldWithPath("data.numberOfElements").type(JsonFieldType.NUMBER)
                                .description("현재 페이지에 담긴 덕질노트 수"),
                            fieldWithPath("data.empty").type(JsonFieldType.BOOLEAN)
                                .description("현재 페이지가 비어 있으면 true"),
                            fieldWithPath("data.sort").type(JsonFieldType.OBJECT)
                                .description("정렬 정보 (항상 productionDate 내림차순)"),
                            fieldWithPath("data.sort.empty").type(JsonFieldType.BOOLEAN)
                                .description("정렬 조건이 없으면 true (이 API는 항상 false)"),
                            fieldWithPath("data.sort.sorted").type(JsonFieldType.BOOLEAN)
                                .description("정렬 적용 여부 (이 API는 항상 true)"),
                            fieldWithPath("data.sort.unsorted").type(JsonFieldType.BOOLEAN)
                                .description("정렬 미적용 여부 (이 API는 항상 false)")
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
                parameterWithName("fanNoteId").description("조회할 덕질노트 ID (숫자, 목록 응답의 data.content[].id)")
            ),
            headerDescriptors = listOf(
                headerWithName(HttpHeaders.AUTHORIZATION)
                    .description(
                        "Bearer {JWT 액세스 토큰} (선택). 보내면 덕질노트 조회 횟수가 칭호 집계에 반영되고(응답 내용은 동일), " +
                            "없거나 유효하지 않은 토큰은 401 없이 비로그인으로 처리됨"
                    )
                    .optional()
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
                            fieldWithPath("data.subtitle").type(JsonFieldType.STRING)
                                .description("서브 제목 (없으면 null 또는 빈 문자열 \"\")").optional(),
                            fieldWithPath("data.content").type(JsonFieldType.STRING)
                                .description("덕질노트 본문 (길이 제한 없음, 없으면 null 또는 빈 문자열 \"\")").optional(),
                            fieldWithPath("data.productionDate").type(JsonFieldType.STRING)
                                .description("제작 날짜 (yyyy-MM-dd)"),
                            fieldWithPath("data.coverImageUrl").type(JsonFieldType.STRING)
                                .description("표지 이미지 URL (표지 미등록 시 null)").optional(),
                            fieldWithPath("data.pages[]").type(JsonFieldType.ARRAY)
                                .description("페이지 이미지 목록 (pageNumber 오름차순, 표지 이미지는 포함되지 않음, 등록된 페이지가 없으면 빈 배열)"),
                            fieldWithPath("data.pages[].pageNumber").type(JsonFieldType.NUMBER)
                                .description("페이지 번호 (1부터 시작하는 연속 정수, 보여줄 순서)"),
                            fieldWithPath("data.pages[].imageUrl").type(JsonFieldType.STRING)
                                .description("페이지 이미지 URL (절대 URL)")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
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
