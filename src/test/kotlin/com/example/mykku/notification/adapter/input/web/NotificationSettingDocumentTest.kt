package com.example.mykku.notification.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.notification.application.dto.NotificationSettingResult
import com.example.mykku.notification.domain.vo.NotificationType
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath

class NotificationSettingDocumentTest : BaseDocumentTest() {

    private val notificationTypeValues = NotificationType.entries
        .joinToString { "${it.name}: ${it.description} 알림" }

    private val settingIdDescription =
        "알림 설정 ID (서버 내부 식별자. 변경 API는 이 값이 아니라 notificationType으로 설정을 식별)"

    @Nested
    @DisplayName("알림 설정 목록 조회")
    inner class GetNotificationSettingList {

        private val apiConfig = ApiRequestConfig(
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val settings = listOf(
                NotificationSettingResult(
                    id = 1L,
                    notificationType = NotificationType.FEED_LIKE,
                    isEnabled = true
                ),
                NotificationSettingResult(
                    id = 2L,
                    notificationType = NotificationType.FEED_COMMENT,
                    isEnabled = false
                )
            )

            `when`(manageNotificationSettingUseCase.getOrCreateSettings(any())).thenReturn(settings)

            val documentFilter = document("notification-setting/list", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data[]").type(JsonFieldType.ARRAY)
                                .description("알림 타입별 수신 설정 목록 (모든 알림 타입이 1건씩 포함, 순서 보장 안 됨)"),
                            fieldWithPath("data[].id").type(JsonFieldType.NUMBER).description(settingIdDescription),
                            fieldWithPath("data[].notificationType").type(JsonFieldType.STRING)
                                .description(
                                    "알림 타입 ($notificationTypeValues). " +
                                        "새 알림 타입이 추가되면 목록에 자동 포함되므로 모르는 값은 무시하거나 기본 라벨로 표시"
                                ),
                            fieldWithPath("data[].isEnabled").type(JsonFieldType.BOOLEAN)
                                .description("알림 수신 여부 (true: 수신, false: 수신 안 함). 자동 생성된 설정은 true")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/notification-settings")
                .then()
                .statusCode(200)
        }
    }

    @Nested
    @DisplayName("알림 설정 변경")
    inner class UpdateNotificationSetting {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("notificationType").type(JsonFieldType.STRING)
                    .description(
                        "변경할 알림 타입 ($notificationTypeValues). " +
                            "대소문자를 구분하며 목록에 없는 값은 400 C101"
                    ),
                fieldWithPath("isEnabled").type(JsonFieldType.BOOLEAN)
                    .description(
                        "알림 수신 여부 (true: 해당 타입 알림 받기, false: 해당 타입 알림 끄기). " +
                            "false이면 해당 타입 알림은 알림 목록에 저장되지 않고 푸시도 발송되지 않음. " +
                            "필드를 생략하거나 null을 보내면 400이 아니라 false로 처리되므로 항상 true/false를 명시"
                    )
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val request = UpdateNotificationSettingRequest(
                notificationType = NotificationType.FEED_LIKE,
                isEnabled = false
            )

            val response = NotificationSettingResult(
                id = 1L,
                notificationType = NotificationType.FEED_LIKE,
                isEnabled = false
            )

            `when`(manageNotificationSettingUseCase.updateSetting(any())).thenReturn(response)

            val documentFilter = document("notification-setting/update", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT)
                                .description("변경이 반영된 해당 타입의 알림 설정"),
                            fieldWithPath("data.id").type(JsonFieldType.NUMBER).description(settingIdDescription),
                            fieldWithPath("data.notificationType").type(JsonFieldType.STRING)
                                .description("변경된 알림 타입. 요청한 notificationType과 같음 ($notificationTypeValues)"),
                            fieldWithPath("data.isEnabled").type(JsonFieldType.BOOLEAN)
                                .description("변경 후 알림 수신 여부 (true: 수신, false: 수신 안 함). 요청한 isEnabled와 같음")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .patch("/api/v1/notification-settings")
                .then()
                .statusCode(200)
        }
    }
}
