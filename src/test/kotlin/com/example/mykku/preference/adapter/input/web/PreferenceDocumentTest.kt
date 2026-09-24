package com.example.mykku.preference.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.docs.RestDocumentationResponse
import com.example.mykku.preference.application.dto.GenrePreferenceResult
import com.example.mykku.preference.application.dto.GoodsPreferenceResult
import com.example.mykku.preference.application.dto.MoodPreferenceResult
import com.example.mykku.preference.domain.vo.GenreType
import com.example.mykku.preference.domain.vo.GoodsType
import com.example.mykku.preference.domain.vo.MoodType
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath

class PreferenceDocumentTest : BaseDocumentTest() {

    @Nested
    @DisplayName("장르 취향 저장")
    inner class UpdateGenrePreference {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("genreTypes").type(JsonFieldType.ARRAY)
                    .description(
                        "최종 선택된 장르 취향 전체 목록. 저장된 장르 취향을 이 목록으로 전체 교체하며 빈 배열이면 모두 삭제. " +
                            "대소문자 구분, 같은 값 중복이나 현재 저장된 값 포함 시 409 C302 " +
                            "(${GenreType.entries.joinToString { "${it.name}: ${it.displayName}" }})"
                    )
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val request = UpdateGenrePreferenceRequest(
                genreTypes = listOf(GenreType.KPOP, GenreType.BAND_ROCK, GenreType.GAME_ESPORTS)
            )

            doNothing().`when`(updateGenrePreferenceUseCase).updateGenrePreferences(any())

            val documentFilter = document("preference/genre-update", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT)
                                .description("항상 빈 객체({}). 저장 결과는 조회 API로 확인")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/preferences/genre")
                .then()
                .statusCode(200)
        }

        @Test
        fun `현재 저장된 값이 포함되었거나 같은 값이 중복된 경우`() {
            val request = UpdateGenrePreferenceRequest(
                genreTypes = listOf(GenreType.KPOP, GenreType.BAND_ROCK)
            )

            doThrow(DataIntegrityViolationException("uk_genre_preference_member_type"))
                .`when`(updateGenrePreferenceUseCase).updateGenrePreferences(any())

            val documentFilter = document("preference/genre-update", "DATA_INTEGRITY_VIOLATION")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/preferences/genre")
                .then()
                .statusCode(409)
        }
    }

    @Nested
    @DisplayName("장르 취향 조회")
    inner class GetGenrePreference {

        private val apiConfig = ApiRequestConfig(
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val genreTypes = listOf(GenreType.KPOP, GenreType.BAND_ROCK)
            val result = GenrePreferenceResult(genreTypes)

            `when`(getGenrePreferenceUseCase.getGenrePreferences(any())).thenReturn(result)

            val documentFilter = document("preference/genre-get", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.genreTypes").type(JsonFieldType.ARRAY)
                                .description(
                                    "저장된 장르 취향 목록. 없으면 빈 배열, 순서 보장 안 됨 " +
                                        "(${GenreType.entries.joinToString { "${it.name}: ${it.displayName}" }})"
                                )
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/preferences/genre")
                .then()
                .statusCode(200)
        }
    }

    @Nested
    @DisplayName("굿즈 취향 저장")
    inner class UpdateGoodsPreference {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("goodsTypes").type(JsonFieldType.ARRAY)
                    .description(
                        "최종 선택된 굿즈 취향 전체 목록. 저장된 굿즈 취향을 이 목록으로 전체 교체하며 빈 배열이면 모두 삭제. " +
                            "대소문자 구분, 같은 값 중복이나 현재 저장된 값 포함 시 409 C302 " +
                            "(${GoodsType.entries.joinToString { "${it.name}: ${it.displayName}" }})"
                    )
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val request = UpdateGoodsPreferenceRequest(
                goodsTypes = listOf(GoodsType.ITABAG, GoodsType.PHOTOCARD_HOLDER)
            )

            doNothing().`when`(updateGoodsPreferenceUseCase).updateGoodsPreferences(any())

            val documentFilter = document("preference/goods-update", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT)
                                .description("항상 빈 객체({}). 저장 결과는 조회 API로 확인")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/preferences/goods")
                .then()
                .statusCode(200)
        }

        @Test
        fun `현재 저장된 값이 포함되었거나 같은 값이 중복된 경우`() {
            val request = UpdateGoodsPreferenceRequest(
                goodsTypes = listOf(GoodsType.ITABAG, GoodsType.PHOTOCARD_HOLDER)
            )

            doThrow(DataIntegrityViolationException("uk_goods_preference_member_type"))
                .`when`(updateGoodsPreferenceUseCase).updateGoodsPreferences(any())

            val documentFilter = document("preference/goods-update", "DATA_INTEGRITY_VIOLATION")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/preferences/goods")
                .then()
                .statusCode(409)
        }
    }

    @Nested
    @DisplayName("굿즈 취향 조회")
    inner class GetGoodsPreference {

        private val apiConfig = ApiRequestConfig(
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val goodsTypes = listOf(GoodsType.PHOTOCARD_HOLDER, GoodsType.UCHIWA)
            val result = GoodsPreferenceResult(goodsTypes)

            `when`(getGoodsPreferenceUseCase.getGoodsPreferences(any())).thenReturn(result)

            val documentFilter = document("preference/goods-get", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.goodsTypes").type(JsonFieldType.ARRAY)
                                .description(
                                    "저장된 굿즈 취향 목록. 없으면 빈 배열, 순서 보장 안 됨 " +
                                        "(${GoodsType.entries.joinToString { "${it.name}: ${it.displayName}" }})"
                                )
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/preferences/goods")
                .then()
                .statusCode(200)
        }
    }

    @Nested
    @DisplayName("분위기 취향 저장")
    inner class UpdateMoodPreference {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("moodTypes").type(JsonFieldType.ARRAY)
                    .description(
                        "최종 선택된 분위기 취향 전체 목록. 저장된 분위기 취향을 이 목록으로 전체 교체하며 빈 배열이면 모두 삭제. " +
                            "대소문자 구분, 같은 값 중복이나 현재 저장된 값 포함 시 409 C302 " +
                            "(${MoodType.entries.joinToString { "${it.name}: ${it.displayName}" }})"
                    )
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val request = UpdateMoodPreferenceRequest(
                moodTypes = listOf(MoodType.COZY, MoodType.FRESH)
            )

            doNothing().`when`(updateMoodPreferenceUseCase).updateMoodPreferences(any())

            val documentFilter = document("preference/mood-update", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT)
                                .description("항상 빈 객체({}). 저장 결과는 조회 API로 확인")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/preferences/mood")
                .then()
                .statusCode(200)
        }

        @Test
        fun `현재 저장된 값이 포함되었거나 같은 값이 중복된 경우`() {
            val request = UpdateMoodPreferenceRequest(
                moodTypes = listOf(MoodType.COZY, MoodType.FRESH)
            )

            doThrow(DataIntegrityViolationException("uk_mood_preference_member_type"))
                .`when`(updateMoodPreferenceUseCase).updateMoodPreferences(any())

            val documentFilter = document("preference/mood-update", "DATA_INTEGRITY_VIOLATION")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/preferences/mood")
                .then()
                .statusCode(409)
        }
    }

    @Nested
    @DisplayName("분위기 취향 조회")
    inner class GetMoodPreference {

        private val apiConfig = ApiRequestConfig(
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val moodTypes = listOf(MoodType.KITSCH, MoodType.Y2K)
            val result = MoodPreferenceResult(moodTypes)

            `when`(getMoodPreferenceUseCase.getMoodPreferences(any())).thenReturn(result)

            val documentFilter = document("preference/mood-get", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.moodTypes").type(JsonFieldType.ARRAY)
                                .description(
                                    "저장된 분위기 취향 목록. 없으면 빈 배열, 순서 보장 안 됨 " +
                                        "(${MoodType.entries.joinToString { "${it.name}: ${it.displayName}" }})"
                                )
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/preferences/mood")
                .then()
                .statusCode(200)
        }
    }
}
