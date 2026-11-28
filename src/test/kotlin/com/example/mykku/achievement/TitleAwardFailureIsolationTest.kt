package com.example.mykku.achievement

import com.example.mykku.BaseControllerTest
import com.example.mykku.achievement.application.port.input.AwardTitlesUseCase
import com.example.mykku.achievement.application.port.output.MemberActivityCountRepository
import com.example.mykku.achievement.domain.vo.ActivityType
import com.example.mykku.feed.adapter.input.web.dto.CreateFeedRequestDto
import com.example.mykku.role.adapter.output.persistence.entity.RoleJpaEntity
import com.fasterxml.jackson.databind.ObjectMapper
import io.restassured.RestAssured
import io.restassured.builder.MultiPartSpecBuilder
import io.restassured.http.ContentType
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.support.TransactionTemplate

@DisplayName("칭호 부여 실패 격리 통합 테스트")
class TitleAwardFailureIsolationTest : BaseControllerTest() {

    private val objectMapper = ObjectMapper()

    @MockitoBean
    private lateinit var memberActivityCountRepository: MemberActivityCountRepository

    @Autowired
    private lateinit var awardTitlesUseCase: AwardTitlesUseCase

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @BeforeEach
    fun stubActivityCountFailure() {
        roleJpaRepository.save(RoleJpaEntity(name = "이 몸 등장", description = "게시글 1회 업로드"))
        whenever(memberActivityCountRepository.incrementAndGet(any(), any()))
            .thenThrow(IllegalStateException("member_activity_count 증가 실패"))
    }

    @Test
    @DisplayName("칭호 부여가 실패해도 사용자의 피드 작성은 성공한다")
    fun feedCreationSucceedsWhenTitleAwardFails() {
        // given
        val member = createAndSaveMember(memberId = "awardfailtester", nickname = "테스터", role = null)
        val board = createAndSaveBoard()
        val request = CreateFeedRequestDto(
            title = "테스트 피드",
            content = "테스트 내용",
            boardId = board.id!!,
            tags = listOf("tag1")
        )

        // when & then
        RestAssured
            .given()
            .headers(createAuthHeaders(member.id))
            .contentType(ContentType.MULTIPART)
            .multiPart(
                MultiPartSpecBuilder(objectMapper.writeValueAsString(request))
                    .controlName("request")
                    .mimeType("application/json")
                    .charset("UTF-8")
                    .build()
            )
            .`when`()
            .post("/api/v1/feeds")
            .then()
            .statusCode(200)

        verify(memberActivityCountRepository).incrementAndGet(any(), any())
        requestNewRoles(member.id).body("data", hasSize<Any>(0))
    }

    @Test
    @DisplayName("칭호 부여가 실패해도 호출한 트랜잭션은 롤백 대상이 되지 않는다")
    fun awardFailureDoesNotMarkCallerTransactionRollbackOnly() {
        // given
        val member = createAndSaveMember(memberId = "awardfailtester", nickname = "테스터", role = null)

        // when & then
        transactionTemplate.execute {
            assertThrows<IllegalStateException> {
                awardTitlesUseCase.handleActivity(member.id, ActivityType.FEED_UPLOAD)
            }
            null
        }
    }
}
