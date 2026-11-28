package com.example.mykku.event.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.auth.exception.AuthErrorCode
import com.example.mykku.common.exception.CommonErrorCode
import com.example.mykku.common.util.KstTime
import com.example.mykku.event.adapter.output.persistence.entity.EventJpaEntity
import com.example.mykku.event.adapter.output.persistence.repository.EventJpaRepository
import com.example.mykku.event.domain.vo.EventType
import com.example.mykku.event.exception.EventErrorCode
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import com.example.mykku.member.exception.MemberErrorCode
import com.example.mykku.util.KstDayGuard
import io.restassured.RestAssured
import io.restassured.response.ValidatableResponse
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("EventAttendanceController 통합 테스트")
class EventAttendanceControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var eventJpaRepository: EventJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var clock: Clock

    private val day: KstDayGuard by lazy { KstDayGuard(clock) }

    @BeforeEach
    fun separateMemberIdsFromEventIds() {
        createAndSaveMember(memberId = "idspacer", email = "idspacer@example.com", socialId = "idspacer-social")
    }

    @Test
    @DisplayName("첫 출석은 200이고 KST 오늘로 기록한 뒤의 상태를 공통 상태 응답으로 준다")
    fun `첫 출석`() {
        val member = attender()
        val today = day.today()
        val event = saveEvent(today)

        val response = attend(event.id!!, member)

        day.assumeStillToday(today)
        response.statusCode(200).body("message", equalTo("출석을 완료했습니다."))
        assertThat(dataOf(response)).isEqualTo(expectedStatus(event, today, total = 1, consecutive = 1))
        assertThat(storedAttendances(event)).containsExactly(member.id to today)
    }

    @Test
    @DisplayName("같은 날 두 번째 요청은 409 EV304이고 출석 기록은 하나뿐이다")
    fun `같은 날 두 번째 출석`() {
        val member = attender()
        val today = day.today()
        val event = saveEvent(today)

        val first = attend(event.id!!, member)
        val second = attend(event.id!!, member)

        day.assumeStillToday(today)
        first.statusCode(200)
        second.statusCode(409).body("code", equalTo(EventErrorCode.ALREADY_ATTENDED_TODAY.code))
        assertThat(storedAttendances(event)).containsExactly(member.id to today)
    }

    @Test
    @DisplayName("그제·어제 출석한 회원이 오늘 출석하면 총 3일, 연속 3일이다")
    fun `사흘 연속 출석`() {
        val member = attender()
        val today = day.today()
        val event = saveEvent(today)
        insertAttendances(event, member, today.minusDays(2), today.minusDays(1))

        val response = attend(event.id!!, member)

        day.assumeStillToday(today)
        assertThat(dataOf(response)).isEqualTo(expectedStatus(event, today, total = 3, consecutive = 3))
    }

    @Test
    @DisplayName("어제를 빠진 회원이 오늘 출석하면 총 일수는 이어서 세고 연속 일수는 1이다")
    fun `하루 빠진 뒤 출석`() {
        val member = attender()
        val today = day.today()
        val event = saveEvent(today)
        insertAttendances(event, member, today.minusDays(2))

        val response = attend(event.id!!, member)

        day.assumeStillToday(today)
        assertThat(dataOf(response)).isEqualTo(expectedStatus(event, today, total = 2, consecutive = 1))
    }

    @Test
    @DisplayName("토큰이 없으면 401 AU201이고 기록하지 않는다")
    fun `토큰 없음`() {
        val event = saveEvent(day.today())

        attend(event.id!!, null)
            .statusCode(401)
            .body("code", equalTo(AuthErrorCode.UNAUTHORIZED.code))

        assertThat(storedAttendances(event)).isEmpty()
    }

    @Test
    @DisplayName("없는 이벤트는 404 EV001, 숫자가 아닌 이벤트 ID는 400 C106이다")
    fun `없는 이벤트와 형식 오류`() {
        val member = attender()

        attend(999999L, member).statusCode(404).body("code", equalTo(EventErrorCode.EVENT_NOT_FOUND.code))
        attend("abc", member).statusCode(400).body("code", equalTo(CommonErrorCode.INVALID_PARAMETER_TYPE.code))
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @EnumSource(value = EventType::class, names = ["ATTENDANCE"], mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("출석체크가 아닌 이벤트(JPA로 저장한 유형 포함)는 400 EV108이고 기록하지 않는다")
    fun `유형 불일치`(type: EventType) {
        val event = saveEvent(day.today(), type)

        attend(event.id!!, attender())
            .statusCode(400)
            .body("code", equalTo(EventErrorCode.EVENT_TYPE_MISMATCH.code))

        assertThat(storedAttendances(event)).isEmpty()
    }

    @ParameterizedTest(name = "{displayName} [시작 {0}일, 종료 {1}일]")
    @CsvSource("1, 8", "-8, -1")
    @DisplayName("시작 전이거나 종료된 이벤트는 400 EV102이고 기록하지 않는다")
    fun `기간 밖`(startOffsetDays: Long, endOffsetDays: Long) {
        val now = KstTime.now(clock)
        val event = saveEventBetween(now.plusDays(startOffsetDays), now.plusDays(endOffsetDays))

        attend(event.id!!, attender())
            .statusCode(400)
            .body("code", equalTo(EventErrorCode.EVENT_NOT_ACTIVE.code))

        assertThat(storedAttendances(event)).isEmpty()
    }

    @Test
    @DisplayName("아이디가 없는 소셜 회원과 프로필을 설정하지 않은 이메일 회원은 403 MB201이고 기록하지 않는다")
    fun `프로필 미완성`() {
        val event = saveEvent(day.today())
        val socialWithoutId = createAndSaveMember(memberId = null, email = "noid@example.com", socialId = "noid-social")
        val emailMember = createAndSaveEmailMember(email = "email@example.com", password = "encoded-password")

        listOf(socialWithoutId, emailMember).forEach {
            attend(event.id!!, it)
                .statusCode(403)
                .body("code", equalTo(MemberErrorCode.PROFILE_NOT_COMPLETED.code))
        }

        assertThat(storedAttendances(event)).isEmpty()
    }

    @Test
    @DisplayName("출석만 한 이벤트는 내가 참여한 이벤트 목록에 나오지 않고, 상세의 당첨 여부도 그대로 false다(D19)")
    fun `출석과 기존 이벤트 API`() {
        val member = attender()
        val event = saveEvent(day.today())
        attend(event.id!!, member).statusCode(200)

        RestAssured.given().headers(createAuthHeaders(member.id))
            .`when`().get("/api/v1/members/me/events")
            .then().statusCode(200).body("data.totalElements", equalTo(0)).body("data.content", empty<Any>())
        RestAssured.given().headers(createAuthHeaders(member.id))
            .`when`().get("/api/v1/events/{eventId}", event.id)
            .then().statusCode(200).body("data.isWinner", equalTo(false))
    }

    private fun attend(eventId: Any, member: MemberJpaEntity?): ValidatableResponse {
        assertThat(eventId).describedAs("이벤트 id와 회원 PK가 같으면 뒤바뀐 인자를 잡지 못한다").isNotEqualTo(member?.id)
        val request = RestAssured.given()
        member?.let { request.headers(createAuthHeaders(it.id)) }
        return request.`when`().post("/api/v1/events/{eventId}/attendance", eventId).then()
    }

    private fun attender(): MemberJpaEntity {
        return createAndSaveMember(memberId = "attender", email = "attender@example.com", socialId = "attender-social")
    }

    private fun saveEvent(today: LocalDate, type: EventType = EventType.ATTENDANCE): EventJpaEntity {
        return saveEventBetween(today.minusDays(3).atStartOfDay(), today.plusDays(4).atStartOfDay(), type)
    }

    private fun saveEventBetween(
        startedAt: LocalDateTime,
        expiredAt: LocalDateTime,
        type: EventType = EventType.ATTENDANCE
    ): EventJpaEntity {
        return eventJpaRepository.save(
            EventJpaEntity(
                title = "${type.name} 이벤트",
                startedAt = startedAt,
                expiredAt = expiredAt,
                thumbnailUrl = "https://example.com/thumbnail.jpg",
                type = type,
                googleFormUrl = FORM_URL.takeIf { type == EventType.GOOGLE_FORM }
            )
        )
    }

    private fun insertAttendances(event: EventJpaEntity, member: MemberJpaEntity, vararg dates: LocalDate) {
        dates.forEach {
            jdbcTemplate.update(
                "INSERT INTO event_attendance (event_id, member_id, attendance_date, created_at, updated_at) " +
                    "VALUES (?, ?, ?, NOW(6), NOW(6))",
                event.id,
                member.id,
                it
            )
        }
    }

    private fun storedAttendances(event: EventJpaEntity): List<Pair<Long?, LocalDate>> {
        return jdbcTemplate.query(
            "SELECT member_id, attendance_date FROM event_attendance WHERE event_id = ? ORDER BY attendance_date",
            { rs, _ -> rs.getObject("member_id", MEMBER_PK_TYPE) to rs.getObject("attendance_date", DATE_TYPE) },
            event.id
        )
    }

    private fun dataOf(response: ValidatableResponse): Map<String, Any?> {
        return response.statusCode(200).extract().jsonPath().getMap("data")
    }

    private fun expectedStatus(
        event: EventJpaEntity,
        today: LocalDate,
        total: Int,
        consecutive: Int
    ): Map<String, Any?> {
        return mapOf(
            "eventId" to event.id!!.toInt(),
            "type" to "ATTENDANCE",
            "isInProgress" to true,
            "attendance" to mapOf(
                "today" to today.toString(),
                "periodStartDate" to today.minusDays(3).toString(),
                "periodEndDate" to today.plusDays(3).toString(),
                "hasAttendedToday" to true,
                "isAttendable" to false,
                "totalAttendanceDays" to total,
                "consecutiveAttendanceDays" to consecutive
            ),
            "entry" to null
        )
    }

    companion object {
        private const val FORM_URL = "https://docs.google.com/forms/d/e/1FAIpQLSc_example/viewform"
        private val MEMBER_PK_TYPE = Long::class.javaObjectType
        private val DATE_TYPE = LocalDate::class.java
    }
}
