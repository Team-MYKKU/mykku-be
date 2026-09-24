package com.example.mykku.notification.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.notification.application.dto.FcmTokenResult
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import java.time.LocalDateTime

class FcmTokenDocumentTest : BaseDocumentTest() {

    @Nested
    @DisplayName("FCM 토큰 등록")
    inner class RegisterFcmToken {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("token").type(JsonFieldType.STRING).description("Firebase Cloud Messaging 토큰"),
                fieldWithPath("deviceId").type(JsonFieldType.STRING).description("기기 고유 식별자"),
                fieldWithPath("deviceType").type(JsonFieldType.STRING).description("기기 타입 (ANDROID, IOS 등)")
                    .optional()
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val request = RegisterFcmTokenRequest(
                token = "fGH8xK2mRxy4hN3qW1pLzA:APA91bF...",
                deviceId = "device_android_001",
                deviceType = "ANDROID"
            )
            val response = FcmTokenResult(
                id = 1L,
                deviceId = "device_android_001",
                deviceType = "ANDROID",
                createdAt = LocalDateTime.now()
            )

            `when`(manageFcmTokenUseCase.registerOrUpdateToken(any())).thenReturn(response)

            val documentFilter = document("fcm-token/register", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("FCM 토큰 응답 데이터"),
                            fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("FCM 토큰 ID"),
                            fieldWithPath("data.deviceId").type(JsonFieldType.STRING).description("기기 ID"),
                            fieldWithPath("data.deviceType").type(JsonFieldType.STRING).description("기기 타입").optional(),
                            fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("토큰 등록 일시")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/fcm-tokens")
                .then()
                .statusCode(200)
        }
    }

    @Nested
    @DisplayName("FCM 토큰 목록 조회")
    inner class GetFcmTokenList {

        private val apiConfig = ApiRequestConfig(
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val responses = listOf(
                FcmTokenResult(
                    id = 1L,
                    deviceId = "device_android_001",
                    deviceType = "ANDROID",
                    createdAt = LocalDateTime.now()
                ),
                FcmTokenResult(
                    id = 2L,
                    deviceId = "device_ios_002",
                    deviceType = "IOS",
                    createdAt = LocalDateTime.now()
                )
            )

            `when`(manageFcmTokenUseCase.getTokens(any())).thenReturn(responses)

            val documentFilter = document("fcm-token/list", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data[]").type(JsonFieldType.ARRAY).description("FCM 토큰 목록"),
                            fieldWithPath("data[].id").type(JsonFieldType.NUMBER).description("FCM 토큰 ID"),
                            fieldWithPath("data[].deviceId").type(JsonFieldType.STRING).description("기기 ID"),
                            fieldWithPath("data[].deviceType").type(JsonFieldType.STRING).description("기기 타입").optional(),
                            fieldWithPath("data[].createdAt").type(JsonFieldType.STRING).description("토큰 등록 일시")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/fcm-tokens")
                .then()
                .statusCode(200)
        }
    }

    @Nested
    @DisplayName("FCM 토큰 삭제")
    inner class DeleteFcmToken {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("deviceId").description("삭제할 기기 ID")
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val deviceId = "device_android_001"

            doNothing().`when`(manageFcmTokenUseCase).deleteToken(any())

            val documentFilter = document("fcm-token/delete", 200)
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
                .`when`()
                .delete("/api/v1/fcm-tokens/{deviceId}", deviceId)
                .then()
                .statusCode(200)
        }
    }
}
