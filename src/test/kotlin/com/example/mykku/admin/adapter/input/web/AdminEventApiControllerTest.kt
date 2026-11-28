package com.example.mykku.admin.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.admin.exception.AdminErrorCode
import com.example.mykku.common.exception.CommonErrorCode
import com.example.mykku.event.adapter.output.persistence.entity.EventImageJpaEntity
import com.example.mykku.event.adapter.output.persistence.entity.EventJpaEntity
import com.example.mykku.event.adapter.output.persistence.repository.EventImageJpaRepository
import com.example.mykku.event.adapter.output.persistence.repository.EventJpaRepository
import com.example.mykku.event.domain.vo.EventStatusType
import com.example.mykku.event.domain.vo.EventType
import com.example.mykku.event.exception.EventErrorCode
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import com.example.mykku.member.domain.vo.SocialProvider
import com.example.mykku.util.MultipartParts.image
import com.example.mykku.util.MultipartParts.text
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.response.ValidatableResponse
import io.restassured.specification.MultiPartSpecification
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasKey
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime

@DisplayName("AdminEventApiController 통합 테스트")
class AdminEventApiControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var eventJpaRepository: EventJpaRepository

    @Autowired
    private lateinit var eventImageJpaRepository: EventImageJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    @DisplayName("이벤트 생성 - 부제목을 함께 저장한다")
    fun `create - 부제목과 함께 이벤트를 생성한다`() {
        val adminSessionId = getAdminSessionId()

        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType("multipart/form-data")
            .multiPart("title", "New Event")
            .multiPart("subTitle", "Event Sub Title")
            .multiPart("description", "Event Description")
            .multiPart("startedAt", "2026-08-01T00:00:00")
            .multiPart("expiredAt", "2026-09-01T00:00:00")
            .multiPart("thumbnailImage", "thumbnail.jpg", ByteArray(10), "image/jpeg")
            .`when`()
            .post("/admin/api/v1/events")
            .then()
            .statusCode(200)
            .body("data.title", equalTo("New Event"))
            .body("data.subTitle", equalTo("Event Sub Title"))
    }

    @Test
    @DisplayName("이벤트 생성 - 부제목이 255자를 초과하면 실패한다")
    fun `create - 부제목 길이 초과 시 400을 반환한다`() {
        val adminSessionId = getAdminSessionId()

        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType("multipart/form-data")
            .multiPart("title", "New Event")
            .multiPart("subTitle", "a".repeat(256))
            .multiPart("startedAt", "2026-08-01T00:00:00")
            .multiPart("expiredAt", "2026-09-01T00:00:00")
            .multiPart("thumbnailImage", "thumbnail.jpg", ByteArray(10), "image/jpeg")
            .`when`()
            .post("/admin/api/v1/events")
            .then()
            .statusCode(400)
            .body("code", equalTo(CommonErrorCode.INVALID_INPUT.code))
    }

    @Test
    @DisplayName("이벤트 생성 - type을 보내지 않거나 빈 값으로 보내면 GENERAL로 저장하고 응답 모양은 그대로다")
    fun `create - type 생략과 빈 값은 GENERAL`() {
        val omitted = postEvent(createParts("유형 생략 이벤트"))
            .statusCode(200)
            .body("data", not(hasKey("type")))
            .body("data", not(hasKey("googleFormUrl")))
        val blank = postEvent(createParts("유형 빈 값 이벤트") + text("type", "")).statusCode(200)

        assertThat(storedTypeColumns(createdId(omitted))).isEqualTo("GENERAL" to null)
        assertThat(storedTypeColumns(createdId(blank))).isEqualTo("GENERAL" to null)
    }

    @Test
    @DisplayName("이벤트 생성 - GENERAL을 지정하면 GENERAL로 저장한다")
    fun `create - GENERAL 지정`() {
        val response = postEvent(createParts() + text("type", "GENERAL")).statusCode(200)

        assertThat(storedTypeColumns(createdId(response))).isEqualTo("GENERAL" to null)
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @ValueSource(strings = ["FOO", "general"])
    @DisplayName("이벤트 생성 - 정의되지 않은 type 값은 C101이고 이미지를 올리지 않는다")
    fun `create - 정의되지 않은 type은 C101`(type: String) {
        postEvent(createParts() + text("type", type))
            .statusCode(400)
            .body("code", equalTo(CommonErrorCode.INVALID_INPUT.code))

        assertThat(testImageUploadService.uploadedUrls).isEmpty()
        assertThat(eventJpaRepository.count()).isZero()
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @EnumSource(value = EventType::class, names = ["ATTENDANCE", "GOOGLE_FORM", "ENTRY"])
    @DisplayName("이벤트 생성 - 아직 생성을 열지 않은 유형은 EV109이고 이미지를 올리지 않는다")
    fun `create - 생성 허용 목록 밖 유형은 EV109`(type: EventType) {
        val googleFormUrl = listOfNotNull(FORM_URL.takeIf { type == EventType.GOOGLE_FORM })

        postEvent(createParts() + text("type", type.name) + googleFormUrl.map { text("googleFormUrl", it) })
            .statusCode(400)
            .body("code", equalTo(EventErrorCode.INVALID_EVENT_TYPE_SETTING.code))

        assertThat(testImageUploadService.uploadedUrls).isEmpty()
        assertThat(eventJpaRepository.count()).isZero()
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @ValueSource(strings = ["", "   "])
    @DisplayName("이벤트 생성 - GENERAL에 빈 구글폼 링크를 보내면 미입력으로 보고 링크 없이 저장한다")
    fun `create - 빈 구글폼 링크는 null로 저장`(googleFormUrl: String) {
        val response = postEvent(createParts() + text("type", "GENERAL") + text("googleFormUrl", googleFormUrl))
            .statusCode(200)

        assertThat(storedTypeColumns(createdId(response))).isEqualTo("GENERAL" to null)
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @ValueSource(strings = [FORM_URL, "javascript:alert(1)"])
    @DisplayName("이벤트 생성 - GENERAL에 구글폼 링크를 보내면 EV109이고 이미지를 올리지 않는다")
    fun `create - GENERAL에 구글폼 링크는 EV109`(googleFormUrl: String) {
        postEvent(createParts() + text("googleFormUrl", googleFormUrl))
            .statusCode(400)
            .body("code", equalTo(EventErrorCode.INVALID_EVENT_TYPE_SETTING.code))

        assertThat(testImageUploadService.uploadedUrls).isEmpty()
        assertThat(eventJpaRepository.count()).isZero()
    }

    @Test
    @DisplayName("이벤트 생성 - 유형 설정(EV109)을 기간(EV107)·이미지 수(EV101)보다 먼저 검사하고, 모두 이미지를 올리기 전에 거부한다")
    fun `create - 유형 설정 검사가 기간과 이미지 수 검사보다 먼저다`() {
        val invertedPeriod = createParts(startedAt = "2026-12-08T00:00:00")
        val elevenImages = createParts(imageCount = 11)
        val googleFormUrl = text("googleFormUrl", FORM_URL)

        assertCreateRejected(invertedPeriod, EventErrorCode.INVALID_EVENT_PERIOD)
        assertCreateRejected(elevenImages, EventErrorCode.EVENT_IMAGE_LIMIT_EXCEEDED)
        assertCreateRejected(invertedPeriod + googleFormUrl, EventErrorCode.INVALID_EVENT_TYPE_SETTING)
        assertCreateRejected(elevenImages + googleFormUrl, EventErrorCode.INVALID_EVENT_TYPE_SETTING)

        assertThat(testImageUploadService.uploadedUrls).isEmpty()
        assertThat(eventJpaRepository.count()).isZero()
    }

    @Test
    @DisplayName("당첨자 저장 - memberId로 참여와 당첨 기록을 만든다")
    fun `setWinners - 참여와 당첨을 생성한다`() {
        val adminSessionId = getAdminSessionId()
        createAndSaveMember(memberId = "memberA", email = "a@example.com", socialId = "a1")
        val event = createAndSaveEvent()

        putWinners(adminSessionId, event.id!!, listOf("memberA"))
            .statusCode(200)
            .body("message", equalTo("당첨자가 성공적으로 선정되었습니다."))
            .body("data.eventId", equalTo(event.id!!.toInt()))
            .body("data.dryRun", equalTo(false))
            .body("data.entries[0].memberId", equalTo("memberA"))
            .body("data.entries[0].result", equalTo("OK"))
            .body("data.addedCount", equalTo(1))

        assertThat(winnersOf(event.id!!).keys).containsExactly("memberA")
        assertThat(participationCountOf(event.id!!)).isEqualTo(1)
        assertThat(eventJpaRepository.findById(event.id!!).get().status).isEqualTo(EventStatusType.WINNER_SELECTED)
    }

    @Test
    @DisplayName("당첨자 저장 - 기존 당첨자에 새 회원을 더하면 기존 winnerId가 유지된다")
    fun `setWinners - A,B에서 A,B,C로 바꾸면 A,B의 winnerId를 유지한다`() {
        val adminSessionId = getAdminSessionId()
        createThreeMembers()
        val event = createAndSaveEvent()
        putWinners(adminSessionId, event.id!!, listOf("memberA", "memberB")).statusCode(200)
        val before = winnersOf(event.id!!)

        putWinners(adminSessionId, event.id!!, listOf("memberA", "memberB", "memberC"))
            .statusCode(200)
            .body("data.addedCount", equalTo(1))
            .body("data.keptCount", equalTo(2))
            .body("data.removedCount", equalTo(0))

        val after = winnersOf(event.id!!)
        assertThat(after.keys).containsExactlyInAnyOrder("memberA", "memberB", "memberC")
        assertThat(after["memberA"]).isEqualTo(before["memberA"])
        assertThat(after["memberB"]).isEqualTo(before["memberB"])
    }

    @Test
    @DisplayName("당첨자 저장 - 목록에서 빠진 회원의 참여와 당첨 기록을 지운다")
    fun `setWinners - A,B에서 A로 바꾸면 B의 참여와 당첨을 삭제한다`() {
        val adminSessionId = getAdminSessionId()
        createThreeMembers()
        val event = createAndSaveEvent()
        putWinners(adminSessionId, event.id!!, listOf("memberA", "memberB")).statusCode(200)

        putWinners(adminSessionId, event.id!!, listOf("memberA"))
            .statusCode(200)
            .body("data.keptCount", equalTo(1))
            .body("data.removedCount", equalTo(1))

        assertThat(winnersOf(event.id!!).keys).containsExactly("memberA")
        assertThat(participationCountOf(event.id!!)).isEqualTo(1)
    }

    @Test
    @DisplayName("당첨자 저장 - 탈퇴한 당첨자의 참여와 당첨 기록은 유지한다")
    fun `setWinners - 탈퇴 당첨자를 유지한다`() {
        val adminSessionId = getAdminSessionId()
        val (_, memberB, _) = createThreeMembers()
        val event = createAndSaveEvent()
        putWinners(adminSessionId, event.id!!, listOf("memberA", "memberB")).statusCode(200)
        memberJpaRepository.deleteById(memberB.id)

        putWinners(adminSessionId, event.id!!, listOf("memberC"), dryRun = true)
            .statusCode(200)
            .body("data.addedCount", equalTo(1))
            .body("data.removedCount", equalTo(1))
            .body("data.withdrawnKeptCount", equalTo(1))
        putWinners(adminSessionId, event.id!!, listOf("memberC")).statusCode(200)

        assertThat(winnersOf(event.id!!).keys).containsExactlyInAnyOrder(null, "memberC")
        assertThat(participationCountOf(event.id!!)).isEqualTo(2)
    }

    @Test
    @DisplayName("당첨자 저장 - 없는 아이디나 프로필 미완료 회원이 있으면 전체를 거부하고 해당 입력값을 돌려준다")
    fun `setWinners - 없는 ID와 닉네임 없는 회원이면 EV106`() {
        val adminSessionId = getAdminSessionId()
        createAndSaveMember(memberId = "memberA", email = "a@example.com", socialId = "a1")
        saveMemberWithoutNickname(memberId = "noNick")
        val event = createAndSaveEvent()

        putWinners(adminSessionId, event.id!!, listOf("ghost", "memberA", "noNick", "ghost"))
            .statusCode(400)
            .body("code", equalTo(EventErrorCode.INVALID_WINNER_MEMBER_IDS.code))
            .body("invalidMemberIds", contains("ghost", "noNick"))

        assertThat(participationCountOf(event.id!!)).isZero()
    }

    @Test
    @DisplayName("당첨자 저장 - 대소문자만 다른 같은 회원을 두 번 넣으면 중복으로 거부한다")
    fun `setWinners - 대소문자만 다른 중복이면 EV402`() {
        val adminSessionId = getAdminSessionId()
        createAndSaveMember(memberId = "memberA", email = "a@example.com", socialId = "a1")
        val event = createAndSaveEvent()

        putWinners(adminSessionId, event.id!!, listOf("memberA", "MEMBERA"))
            .statusCode(400)
            .body("code", equalTo(EventErrorCode.DUPLICATE_WINNER.code))

        assertThat(participationCountOf(event.id!!)).isZero()
    }

    @Test
    @DisplayName("당첨자 저장 - 앞뒤 공백과 대소문자가 달라도 회원을 찾는다")
    fun `setWinners - 공백 붙은 ID도 정상 처리한다`() {
        val adminSessionId = getAdminSessionId()
        createAndSaveMember(memberId = "memberA", email = "a@example.com", socialId = "a1")
        val event = createAndSaveEvent()

        putWinners(adminSessionId, event.id!!, listOf("  MemberA  ", ""))
            .statusCode(200)
            .body("data.entries.size()", equalTo(1))
            .body("data.entries[0].input", equalTo("MemberA"))
            .body("data.entries[0].memberId", equalTo("memberA"))

        assertThat(winnersOf(event.id!!).keys).containsExactly("memberA")
    }

    @Test
    @DisplayName("당첨자 저장 - 빈 목록이나 공백 줄만 있으면 거부한다")
    fun `setWinners - 빈 입력이면 EV401`() {
        val adminSessionId = getAdminSessionId()
        val event = createAndSaveEvent()

        putWinners(adminSessionId, event.id!!, emptyList())
            .statusCode(400)
            .body("code", equalTo(EventErrorCode.EMPTY_WINNERS.code))
        putWinners(adminSessionId, event.id!!, listOf("", "   "))
            .statusCode(400)
            .body("code", equalTo(EventErrorCode.EMPTY_WINNERS.code))
    }

    @Test
    @DisplayName("당첨자 확인(dryRun) - 입력별 판정을 돌려주고 아무것도 저장하지 않는다")
    fun `setWinners - dryRun은 판정만 하고 저장하지 않는다`() {
        val adminSessionId = getAdminSessionId()
        createAndSaveMember(memberId = "memberA", email = "a@example.com", socialId = "a1")
        saveMemberWithoutNickname(memberId = "noNick")
        val event = createAndSaveEvent()

        putWinners(adminSessionId, event.id!!, listOf("memberA", "ghost", "noNick", "MEMBERA"), dryRun = true)
            .statusCode(200)
            .body("message", equalTo("당첨자 입력을 확인했습니다."))
            .body("data.dryRun", equalTo(true))
            .body("data.entries.result", contains("OK", "NOT_FOUND", "PROFILE_INCOMPLETE", "DUPLICATE"))
            .body("data.entries[1].memberId", nullValue())
            .body("data.addedCount", equalTo(1))

        assertThat(participationCountOf(event.id!!)).isZero()
        assertThat(winnersOf(event.id!!)).isEmpty()
        assertThat(eventJpaRepository.findById(event.id!!).get().status).isEqualTo(EventStatusType.ACTIVE)
    }

    @Test
    @DisplayName("당첨자 저장 - 관리자 인증 없이 접근 불가")
    fun `setWinners - 관리자 인증이 없으면 당첨자를 선정할 수 없다`() {
        createAndSaveMember(memberId = "memberA", email = "a@example.com", socialId = "a1")
        val event = createAndSaveEvent()

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(mapOf("memberIds" to listOf("memberA"), "dryRun" to false))
            .`when`()
            .put("/admin/api/v1/events/{eventId}/winners", event.id)
            .then()
            .statusCode(401)
            .body("code", equalTo(AdminErrorCode.UNAUTHORIZED.code))
    }

    @Test
    @DisplayName("당첨자 저장 - 종료되지 않은 이벤트는 확인도 저장도 할 수 없다")
    fun `setWinners - 기간이 만료되지 않은 이벤트는 당첨자를 선정할 수 없다`() {
        val adminSessionId = getAdminSessionId()
        createAndSaveMember(memberId = "memberA", email = "a@example.com", socialId = "a1")
        val event = createAndSaveEvent(expiredAt = LocalDateTime.now().plusDays(7))

        putWinners(adminSessionId, event.id!!, listOf("memberA"), dryRun = true)
            .statusCode(400)
            .body("code", equalTo(EventErrorCode.EVENT_NOT_EXPIRED.code))
        putWinners(adminSessionId, event.id!!, listOf("memberA"))
            .statusCode(400)
            .body("code", equalTo(EventErrorCode.EVENT_NOT_EXPIRED.code))
    }

    @Test
    @DisplayName("당첨자 저장 - 예전 participationIds POST는 더 이상 받지 않는다")
    fun `setWinners - POST는 405`() {
        val adminSessionId = getAdminSessionId()
        val event = createAndSaveEvent()

        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(mapOf("participationIds" to listOf(1L)))
            .`when`()
            .post("/admin/api/v1/events/{eventId}/winners", event.id)
            .then()
            .statusCode(405)
    }

    @Test
    @DisplayName("이벤트 삭제 - 참여·당첨·공지·이미지를 함께 지운다")
    fun `delete - 딸린 기록을 함께 지운다`() {
        val adminSessionId = getAdminSessionId()
        createAndSaveMember(memberId = "memberA", email = "a@example.com", socialId = "a1")
        val event = createAndSaveEvent()
        putWinners(adminSessionId, event.id!!, listOf("memberA")).statusCode(200)
        jdbcTemplate.update(
            "INSERT INTO event_image (url, order_index, event_id, created_at, updated_at) " +
                "VALUES ('https://test-bucket.s3.amazonaws.com/e.jpg', 0, ?, NOW(), NOW())",
            event.id
        )
        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(mapOf("title" to "발표", "content" to "축하합니다", "announcedAt" to "2025-10-10"))
            .`when`()
            .put("/admin/api/v1/events/{eventId}/winner-announcement", event.id)
            .then()
            .statusCode(200)

        RestAssured.given()
            .sessionId(adminSessionId)
            .`when`()
            .delete("/admin/api/v1/events/{eventId}", event.id)
            .then()
            .statusCode(200)
            .body("message", equalTo("이벤트가 삭제되었습니다"))

        EVENT_CHILD_TABLES.forEach { assertThat(countByEvent(it, event.id!!)).`as`(it).isZero() }
        assertThat(eventJpaRepository.existsById(event.id!!)).isFalse()
    }

    @Test
    @DisplayName("이벤트 삭제 - 그 이벤트의 썸네일과 상세 이미지만 지우고, 다른 이벤트의 이미지는 남긴다")
    fun `delete - 그 이벤트의 이미지만 지운다`() {
        val event = createAndSaveEvent(thumbnailUrl = "$BUCKET_URL/event-thumbnail.jpg")
        val images = saveEventImages(event, "a.jpg", "b.jpg")
        saveEventImages(createAndSaveEvent(title = "남는 이벤트", thumbnailUrl = "$BUCKET_URL/kept.jpg"), "kept-a.jpg")

        RestAssured.given()
            .sessionId(getAdminSessionId())
            .`when`()
            .delete("/admin/api/v1/events/{eventId}", event.id)
            .then()
            .statusCode(200)

        assertThat(testImageUploadService.deletedUrls).containsExactlyInAnyOrderElementsOf(images + event.thumbnailUrl)
    }

    @Test
    @DisplayName("이벤트 삭제 - 없는 이벤트는 EV001, 미인증은 401")
    fun `delete - 없는 이벤트와 미인증`() {
        RestAssured.given()
            .sessionId(getAdminSessionId())
            .`when`()
            .delete("/admin/api/v1/events/{eventId}", 999999L)
            .then()
            .statusCode(404)
            .body("code", equalTo(EventErrorCode.EVENT_NOT_FOUND.code))
        RestAssured.given()
            .`when`()
            .delete("/admin/api/v1/events/{eventId}", 1L)
            .then()
            .statusCode(401)
            .body("code", equalTo(AdminErrorCode.UNAUTHORIZED.code))
    }

    @Test
    @DisplayName("당첨자 발표 공지 저장 - 정상 케이스(신규 생성)")
    fun `upsertWinnerAnnouncement - 관리자가 공지를 신규 저장한다`() {
        val adminSessionId = getAdminSessionId()
        val event = createAndSaveEvent()

        val request = mapOf(
            "title" to "[봄맞이 이벤트] 수상자 발표",
            "content" to "참여해 주신 모든 분들께 감사드립니다.\n대상: OOO",
            "announcedAt" to "2025-10-10"
        )

        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .put("/admin/api/v1/events/{eventId}/winner-announcement", event.id)
            .then()
            .statusCode(200)
            .body("message", equalTo("당첨자 발표 공지가 성공적으로 저장되었습니다."))
            .body("data.eventId", equalTo(event.id!!.toInt()))
            .body("data.title", equalTo("[봄맞이 이벤트] 수상자 발표"))
            .body("data.announcedAt", equalTo("2025-10-10"))
    }

    @Test
    @DisplayName("당첨자 발표 공지 저장 - 재호출 시 수정(upsert)")
    fun `upsertWinnerAnnouncement - 재호출하면 기존 공지를 수정한다`() {
        val adminSessionId = getAdminSessionId()
        val event = createAndSaveEvent()

        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(mapOf("title" to "초안", "content" to "초안 본문", "announcedAt" to "2025-10-10"))
            .`when`()
            .put("/admin/api/v1/events/{eventId}/winner-announcement", event.id)
            .then()
            .statusCode(200)

        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(mapOf("title" to "수정본", "content" to "수정 본문", "announcedAt" to "2025-10-11"))
            .`when`()
            .put("/admin/api/v1/events/{eventId}/winner-announcement", event.id)
            .then()
            .statusCode(200)
            .body("data.title", equalTo("수정본"))
            .body("data.content", equalTo("수정 본문"))
            .body("data.announcedAt", equalTo("2025-10-11"))
    }

    @Test
    @DisplayName("당첨자 발표 공지 저장 - 관리자 인증 없이 접근 불가")
    fun `upsertWinnerAnnouncement - 관리자 인증이 없으면 저장할 수 없다`() {
        val event = createAndSaveEvent()

        val request = mapOf(
            "title" to "제목",
            "content" to "본문",
            "announcedAt" to "2025-10-10"
        )

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .put("/admin/api/v1/events/{eventId}/winner-announcement", event.id)
            .then()
            .statusCode(401)
            .body("code", equalTo(AdminErrorCode.UNAUTHORIZED.code))
    }

    private fun createAndSaveEvent(
        title: String = "테스트 이벤트",
        expiredAt: LocalDateTime = LocalDateTime.now().minusDays(1),
        thumbnailUrl: String = "https://example.com/thumbnail.jpg"
    ): EventJpaEntity {
        val event = EventJpaEntity(
            title = title,
            description = "테스트 이벤트 설명",
            startedAt = LocalDateTime.now().minusDays(30),
            expiredAt = expiredAt,
            status = EventStatusType.ACTIVE,
            thumbnailUrl = thumbnailUrl
        )
        return eventJpaRepository.save(event)
    }

    private fun postEvent(parts: List<MultiPartSpecification>): ValidatableResponse {
        var spec = RestAssured.given()
            .sessionId(getAdminSessionId())
            .contentType("multipart/form-data")
        parts.forEach { spec = spec.multiPart(it) }
        return spec.`when`().post("/admin/api/v1/events").then()
    }

    private fun createParts(
        title: String = "유형 이벤트",
        startedAt: String = "2026-12-01T00:00:00",
        imageCount: Int = 1
    ): List<MultiPartSpecification> {
        return listOf(
            text("title", title),
            text("startedAt", startedAt),
            text("expiredAt", "2026-12-07T23:59:59"),
            image("thumbnailImage")
        ) + (1..imageCount).map { image("images", "image-$it.png") }
    }

    private fun assertCreateRejected(parts: List<MultiPartSpecification>, errorCode: EventErrorCode) {
        postEvent(parts).statusCode(400).body("code", equalTo(errorCode.code))
    }

    private fun createdId(response: ValidatableResponse): Long {
        return response.extract().jsonPath().getLong("data.id")
    }

    private fun storedTypeColumns(eventId: Long): Pair<String, String?> {
        return jdbcTemplate.queryForObject(
            "SELECT type, google_form_url FROM event WHERE id = ?",
            { rs, _ -> rs.getString("type") to rs.getString("google_form_url") },
            eventId
        )!!
    }

    private fun saveEventImages(event: EventJpaEntity, vararg names: String): List<String> {
        return names.mapIndexed { index, name ->
            val image = EventImageJpaEntity(url = "$BUCKET_URL/event-images/$name", orderIndex = index, event = event)
            eventImageJpaRepository.save(image).url
        }
    }

    private fun putWinners(
        adminSessionId: String,
        eventId: Long,
        memberIds: List<String>,
        dryRun: Boolean = false
    ): ValidatableResponse {
        return RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(mapOf("memberIds" to memberIds, "dryRun" to dryRun))
            .`when`()
            .put("/admin/api/v1/events/{eventId}/winners", eventId)
            .then()
    }

    private fun createThreeMembers(): Triple<MemberJpaEntity, MemberJpaEntity, MemberJpaEntity> {
        return Triple(
            createAndSaveMember(memberId = "memberA", email = "a@example.com", socialId = "a1"),
            createAndSaveMember(memberId = "memberB", email = "b@example.com", socialId = "b1"),
            createAndSaveMember(memberId = "memberC", email = "c@example.com", socialId = "c1")
        )
    }

    private fun saveMemberWithoutNickname(memberId: String) {
        memberJpaRepository.save(
            MemberJpaEntity(
                memberId = memberId,
                nickname = null,
                email = "$memberId@example.com",
                socialId = "$memberId-social",
                provider = SocialProvider.GOOGLE,
                profileImage = ""
            )
        )
    }

    private fun winnersOf(eventId: Long): Map<String?, Long> {
        return jdbcTemplate.queryForList(
            "SELECT m.member_id AS member_id, w.id AS winner_id FROM event_winner w " +
                "JOIN event_participation p ON p.id = w.participation_id " +
                "LEFT JOIN member m ON m.id = p.member_id WHERE w.event_id = ?",
            eventId
        ).associate { it["member_id"] as String? to (it["winner_id"] as Number).toLong() }
    }

    private fun countByEvent(table: String, eventId: Long): Int {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM $table WHERE event_id = ?", Int::class.java, eventId)!!
    }

    private fun participationCountOf(eventId: Long): Int {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM event_participation WHERE event_id = ?",
            Int::class.java,
            eventId
        )!!
    }

    companion object {
        private const val BUCKET_URL = "https://test-bucket.s3.amazonaws.com"
        private const val FORM_URL = "https://docs.google.com/forms/d/e/1FAIpQLSc_example/viewform"
        private val EVENT_CHILD_TABLES = listOf(
            "event_participation",
            "event_winner",
            "event_winner_announcement",
            "event_image"
        )
    }
}
