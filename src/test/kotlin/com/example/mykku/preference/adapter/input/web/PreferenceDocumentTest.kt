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
import com.example.mykku.preference.exception.PreferenceErrorCode
import com.example.mykku.preference.exception.PreferenceException
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

class PreferenceDocumentTest : BaseDocumentTest() {

    @Nested
    @DisplayName("장르 취향 저장")
    inner class UpdateGenrePreference {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("genreTypes").type(JsonFieldType.ARRAY)
                    .description("장르 취향 목록 (${GenreType.entries.joinToString { it.name }})")
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
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터").optional()
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
        fun `빈 목록`() {
            val request = UpdateGenrePreferenceRequest(
                genreTypes = emptyList()
            )

            doThrow(PreferenceException(PreferenceErrorCode.EMPTY_PREFERENCE_LIST))
                .`when`(updateGenrePreferenceUseCase).updateGenrePreferences(any())

            val documentFilter = document("preference/genre-update", "EMPTY_PREFERENCE_LIST")
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
                .statusCode(400)
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
                                .description("장르 취향 목록 (${GenreType.entries.joinToString { it.name }})")
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
                    .description("굿즈 취향 목록 (${GoodsType.entries.joinToString { it.name }})")
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
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터").optional()
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
                                .description("굿즈 취향 목록 (${GoodsType.entries.joinToString { it.name }})")
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
                    .description("분위기 취향 목록 (${MoodType.entries.joinToString { it.name }})")
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
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터").optional()
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
                                .description("분위기 취향 목록 (${MoodType.entries.joinToString { it.name }})")
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
