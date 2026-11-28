package com.example.mykku.event.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.docs.DocumentFixtureTime
import com.example.mykku.docs.RestDocumentationFilterBuilder
import com.example.mykku.docs.RestDocumentationResponse
import com.example.mykku.event.application.dto.AttendanceStatusResult
import com.example.mykku.event.application.dto.MyEventStatusResult
import com.example.mykku.event.domain.vo.EventType
import com.example.mykku.event.exception.EventErrorCode
import com.example.mykku.event.exception.EventException
import com.example.mykku.member.exception.MemberErrorCode
import com.example.mykku.member.exception.MemberException
import io.restassured.http.ContentType
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.restassured.RestDocumentationFilter

class EventAttendanceDocumentTest : BaseDocumentTest() {

    private val apiConfig = ApiRequestConfig(
        pathParameters = listOf(
            parameterWithName("eventId")
                .description("출석할 출석체크(ATTENDANCE) 이벤트 ID (존재하지 않거나 0 이하이면 404 EVENT_NOT_FOUND)")
        ),
        headerDescriptors = AUTH_HEADER_DESCRIPTOR
    )

    @Test
    fun `출석 성공`() {
        whenever(attendEventUseCase.execute(any(), any())).thenReturn(ATTENDED)

        val documentFilter = document("event-type/attendance", 200)
            .request(request().applyConfig(apiConfig))
            .response(response().responseBodyField(*STATUS_FIELDS))
            .build()

        postAttendance(documentFilter, 200)
    }

    @Test
    fun `없는 이벤트`() {
        whenever(attendEventUseCase.execute(any(), any())).thenThrow(EventException(EventErrorCode.EVENT_NOT_FOUND))

        postAttendance(errorFilter(document("event-type/attendance", "EVENT_NOT_FOUND")), 404)
    }

    @Test
    fun `출석체크가 아닌 이벤트`() {
        whenever(attendEventUseCase.execute(any(), any())).thenThrow(EventException(EventErrorCode.EVENT_TYPE_MISMATCH))

        postAttendance(errorFilter(document("event-type/attendance", "EVENT_TYPE_MISMATCH")), 400)
    }

    @Test
    fun `참여 가능 구간 밖`() {
        whenever(attendEventUseCase.execute(any(), any())).thenThrow(EventException(EventErrorCode.EVENT_NOT_ACTIVE))

        postAttendance(errorFilter(document("event-type/attendance", "EVENT_NOT_ACTIVE")), 400)
    }

    @Test
    fun `프로필 미완성`() {
        whenever(attendEventUseCase.execute(any(), any()))
            .thenThrow(MemberException(MemberErrorCode.PROFILE_NOT_COMPLETED))

        postAttendance(errorFilter(document("event-type/attendance", "PROFILE_NOT_COMPLETED")), 403)
    }

    @Test
    fun `오늘 이미 출석`() {
        whenever(attendEventUseCase.execute(any(), any()))
            .thenThrow(EventException(EventErrorCode.ALREADY_ATTENDED_TODAY))

        postAttendance(errorFilter(document("event-type/attendance", "ALREADY_ATTENDED_TODAY")), 409)
    }

    @Test
    fun `처리 중 이벤트 삭제나 회원 탈퇴`() {
        whenever(attendEventUseCase.execute(any(), any()))
            .thenThrow(DataIntegrityViolationException("fk_event_attendance_event"))

        postAttendance(errorFilter(document("event-type/attendance", "DATA_INTEGRITY_VIOLATION")), 409)
    }

    private fun errorFilter(builder: RestDocumentationFilterBuilder): RestDocumentationFilter {
        return builder
            .request(request().applyConfig(apiConfig))
            .response(RestDocumentationResponse.ERROR_RESPONSE)
            .build()
    }

    private fun postAttendance(documentFilter: RestDocumentationFilter, status: Int) {
        given(documentFilter)
            .headers(AUTH_HEADER)
            .contentType(ContentType.JSON)
            .`when`()
            .post("/api/v1/events/{eventId}/attendance", EVENT_ID)
            .then()
            .statusCode(status)
    }

    companion object {
        private const val EVENT_ID = 12L
        private val TODAY = DocumentFixtureTime.TODAY
        private val TYPE_VALUES = EventType.entries.joinToString { "${it.name}: ${it.description}" }

        private val ATTENDED = MyEventStatusResult(
            eventId = EVENT_ID,
            type = EventType.ATTENDANCE,
            isInProgress = true,
            attendance = AttendanceStatusResult(
                today = TODAY,
                periodStartDate = TODAY.minusDays(3),
                periodEndDate = TODAY.plusDays(3),
                hasAttendedToday = true,
                isAttendable = false,
                totalAttendanceDays = 3,
                consecutiveAttendanceDays = 3
            ),
            entry = null
        )

        private val STATUS_FIELDS: Array<FieldDescriptor> = arrayOf(
            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
            fieldWithPath("data").type(JsonFieldType.OBJECT).description("출석한 뒤의 내 이벤트 상태(공통 상태 응답)"),
            fieldWithPath("data.eventId").type(JsonFieldType.NUMBER).description("이벤트 ID"),
            fieldWithPath("data.type").type(JsonFieldType.STRING)
                .description(
                    "이벤트 유형 ($TYPE_VALUES). 출석은 ATTENDANCE 이벤트에서만 성공하므로 항상 ATTENDANCE. " +
                        "앞으로 유형이 늘 수 있으니 모르는 값은 GENERAL처럼 다룸"
                ),
            fieldWithPath("data.isInProgress").type(JsonFieldType.BOOLEAN)
                .description(
                    "서버 기준(KST) 지금이 참여 가능 구간인지 (startedAt ≤ 지금 < expiredAt). " +
                        "목록·상세의 status=ACTIVE는 시작 전도 포함하므로 이 값과 다를 수 있음. 출석 성공 응답에서는 항상 true"
                ),
            fieldWithPath("data.attendance").type(JsonFieldType.OBJECT)
                .description("내 출석 현황. 출석체크(ATTENDANCE) 이벤트에서만 값이 있고 그 밖의 유형은 null. 출석 성공 응답에서는 항상 있음"),
            fieldWithPath("data.attendance.today").type(JsonFieldType.STRING)
                .description("서버 기준 KST 오늘 날짜 (yyyy-MM-dd). 이번 출석은 이 날짜로 기록됨"),
            fieldWithPath("data.attendance.periodStartDate").type(JsonFieldType.STRING)
                .description("출석할 수 있는 첫날 (yyyy-MM-dd, startedAt의 날짜)"),
            fieldWithPath("data.attendance.periodEndDate").type(JsonFieldType.STRING)
                .description(
                    "출석할 수 있는 마지막 날 (yyyy-MM-dd, expiredAt 직전 순간의 날짜). " +
                        "예: expiredAt이 12-08T00:00이면 12-07, 12-07T23:59:59이면 12-07"
                ),
            fieldWithPath("data.attendance.hasAttendedToday").type(JsonFieldType.BOOLEAN)
                .description("오늘(today) 출석이 기록돼 있는지. 출석 성공 응답에서는 항상 true"),
            fieldWithPath("data.attendance.isAttendable").type(JsonFieldType.BOOLEAN)
                .description(
                    "지금 출석할 수 있는지 (참여 가능 구간이고 오늘 출석하지 않았으면 true). " +
                        "아이디·닉네임 설정 여부는 보지 않으므로 미설정 회원은 true여도 출석 요청이 403 MB201. " +
                        "출석 성공 응답에서는 항상 false"
                ),
            fieldWithPath("data.attendance.totalAttendanceDays").type(JsonFieldType.NUMBER)
                .description("총 출석 일수 (출석할 수 있는 날짜 안의 출석 날짜 수, 빠진 날이 있어도 줄지 않음)"),
            fieldWithPath("data.attendance.consecutiveAttendanceDays").type(JsonFieldType.NUMBER)
                .description("연속 출석 일수 (오늘부터 하루씩 거꾸로 끊김 없이 이어진 출석 날짜 수. 하루라도 빠지면 다시 1부터)"),
            fieldWithPath("data.entry").type(JsonFieldType.OBJECT)
                .description("내 응모 상태. 응모(ENTRY) 유형 이벤트에서만 값이 있음(응모 기능 출시 후). 출석 응답에서는 항상 null")
                .optional(),
            fieldWithPath("data.entry.hasEntered").type(JsonFieldType.BOOLEAN)
                .description("이미 응모했는지 (응모 유형에서만)")
                .optional(),
            fieldWithPath("data.entry.isEnterable").type(JsonFieldType.BOOLEAN)
                .description("지금 응모할 수 있는지: 참여 가능 구간이고 아직 응모하지 않았으면 true (응모 유형에서만, 아이디·닉네임 설정 여부는 보지 않음)")
                .optional()
        )
    }
}
