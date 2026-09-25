package com.example.mykku.notification.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.docs.RestDocumentationResponse
import com.example.mykku.notification.application.dto.FcmTokenResult
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import java.time.LocalDateTime

class FcmTokenDocumentTest : BaseDocumentTest() {

    companion object {
        private const val TOKEN_ID_DESCRIPTION =
            "FCM 토큰 등록 ID (서버 내부 식별자). 같은 deviceId로 재등록해 토큰만 갱신될 때는 유지됨. " +
                "이 ID를 받는 API는 없음 (삭제는 deviceId로 함)"
        private const val CREATED_AT_DESCRIPTION =
            "이 기기 등록이 생성된 일시 (KST, ISO-8601). 같은 deviceId로 재등록해 토큰만 갱신될 때는 바뀌지 않으며, " +
                "삭제(로그아웃 포함) 후 다시 등록하면 새 일시가 됨"
    }

    @Nested
    @DisplayName("FCM 토큰 등록")
    inner class RegisterFcmToken {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("token").type(JsonFieldType.STRING).description(
                    "Firebase SDK가 발급한 FCM 등록 토큰 (최대 500자, 빈 문자열/공백 불가). 서버는 실제 유효성을 확인하지 않음"
                ),
                fieldWithPath("deviceId").type(JsonFieldType.STRING).description(
                    "클라이언트가 앱 설치 단위로 한 번 생성해 기기에 영구 저장하는 기기 식별자 " +
                        "(예: 최초 실행 시 생성한 UUID, 최대 100자, 빈 문자열/공백 불가). " +
                        "회원 ID와 무관하며, 로그아웃 API의 deviceId와 FCM 토큰 삭제 API의 경로 파라미터에 같은 값을 사용해야 함. " +
                        "URL 경로에도 쓰이므로 URL-safe 문자 권장"
                ),
                fieldWithPath("deviceType").type(JsonFieldType.STRING).description(
                    "기기 타입 (선택, 자유 문자열, 최대 50자). 서버는 값을 검증하지 않고 받은 문자열을 대소문자 그대로 저장·반환하며 " +
                        "푸시 발송 등 서버 로직에는 사용하지 않음. 권장 값: ANDROID(안드로이드 앱), IOS(iOS 앱). " +
                        "보내지 않으면 null로 저장되며, 같은 deviceId로 재등록할 때는 바뀌지 않음"
                )
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
                            fieldWithPath("data").type(JsonFieldType.OBJECT)
                                .description("등록 또는 갱신된 기기의 FCM 토큰 정보 (토큰 문자열은 포함하지 않음)"),
                            fieldWithPath("data.id").type(JsonFieldType.NUMBER).description(TOKEN_ID_DESCRIPTION),
                            fieldWithPath("data.deviceId").type(JsonFieldType.STRING)
                                .description("기기 식별자 (요청한 deviceId)"),
                            fieldWithPath("data.deviceType").type(JsonFieldType.STRING).description(
                                "기기 타입. 새로 등록될 때 deviceType을 보내지 않았으면 null. " +
                                    "같은 deviceId로 재등록(토큰 갱신)할 때는 요청 값과 관계없이 기존 값이 반환됨"
                            ).optional(),
                            fieldWithPath("data.createdAt").type(JsonFieldType.STRING)
                                .description(CREATED_AT_DESCRIPTION)
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

        @Test
        fun `길이 초과 또는 동시 등록 충돌로 저장 실패`() {
            val request = RegisterFcmTokenRequest(
                token = "fGH8xK2mRxy4hN3qW1pLzA:APA91bF...",
                deviceId = "device_android_001",
                deviceType = "ANDROID"
            )

            `when`(manageFcmTokenUseCase.registerOrUpdateToken(any()))
                .thenThrow(DataIntegrityViolationException("uk_member_device"))

            val documentFilter = document("fcm-token/register", "DATA_INTEGRITY_VIOLATION")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/fcm-tokens")
                .then()
                .statusCode(409)
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
                            fieldWithPath("data[]").type(JsonFieldType.ARRAY).description(
                                "로그인 회원 본인의 FCM 토큰이 등록된 기기 목록 (토큰 문자열은 포함하지 않음. " +
                                    "없으면 빈 배열, 정렬 순서 보장 안 됨)"
                            ),
                            fieldWithPath("data[].id").type(JsonFieldType.NUMBER).description(TOKEN_ID_DESCRIPTION),
                            fieldWithPath("data[].deviceId").type(JsonFieldType.STRING)
                                .description("기기 식별자 (등록 시 보낸 deviceId). 현재 기기의 등록 여부는 이 값으로 확인"),
                            fieldWithPath("data[].deviceType").type(JsonFieldType.STRING)
                                .description("기기 타입. 등록할 때 deviceType을 보내지 않았으면 null").optional(),
                            fieldWithPath("data[].createdAt").type(JsonFieldType.STRING)
                                .description(CREATED_AT_DESCRIPTION)
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
                parameterWithName("deviceId").description("FCM 토큰 등록 시 전달한 deviceId (URL 인코딩하여 전달)")
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
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("항상 빈 객체({})")
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
