package com.example.mykku.achievement

import com.example.mykku.BaseControllerTest
import com.example.mykku.contest.adapter.output.persistence.entity.ContestJpaEntity
import com.example.mykku.contest.adapter.output.persistence.entity.ContestTagJpaEntity
import com.example.mykku.contest.adapter.output.persistence.repository.ContestJpaRepository
import com.example.mykku.contest.adapter.output.persistence.repository.ContestTagJpaRepository
import com.example.mykku.contest.domain.vo.ContestStatusType
import com.example.mykku.fannote.adapter.output.persistence.entity.FanNoteJpaEntity
import com.example.mykku.fannote.adapter.output.persistence.repository.FanNoteJpaRepository
import com.example.mykku.feed.adapter.input.web.dto.CreateFeedRequestDto
import com.example.mykku.role.adapter.output.persistence.entity.RoleJpaEntity
import com.fasterxml.jackson.databind.ObjectMapper
import io.restassured.RestAssured
import io.restassured.builder.MultiPartSpecBuilder
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.SyncTaskExecutor
import org.springframework.core.task.TaskExecutor
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("칭호 자동 획득 통합 테스트")
class TitleAcquisitionIntegrationTest : BaseControllerTest() {

    private val objectMapper = ObjectMapper()

    @Autowired
    private lateinit var contestJpaRepository: ContestJpaRepository

    @Autowired
    private lateinit var contestTagJpaRepository: ContestTagJpaRepository

    @Autowired
    private lateinit var fanNoteJpaRepository: FanNoteJpaRepository

    @Autowired
    @Qualifier("titleAwardExecutor")
    private lateinit var titleAwardExecutor: TaskExecutor

    @BeforeEach
    fun seedTitles() {
        roleJpaRepository.save(RoleJpaEntity(name = "이 몸 등장", description = "게시글 1회 업로드"))
        roleJpaRepository.save(RoleJpaEntity(name = "영역전개", description = "게시글 5회 업로드"))
    }

    @Test
    @DisplayName("피드를 작성하면 게시글 칭호가 자동 부여되고 신규 칭호 조회에 나온다")
    fun awardsFeedUploadTitle() {
        // given
        val member = createAndSaveMember(memberId = "titleacqtester", nickname = "테스터", role = null)
        val board = createAndSaveBoard()

        // when
        createFeed(member.id, board.id!!)

        // then
        requestNewRoles(member.id)
            .body("data", hasSize<Any>(1))
            .body("data[0].role.name", equalTo("이 몸 등장"))
            .body("data[0].isRepresentative", equalTo(true))
    }

    @Test
    @DisplayName("신규 칭호를 조회한 뒤에는 같은 칭호가 다시 나오지 않는다")
    fun newTitleIsReturnedOnlyOnce() {
        // given
        val member = createAndSaveMember(memberId = "titleacqtester", nickname = "테스터", role = null)
        val board = createAndSaveBoard()
        createFeed(member.id, board.id!!)

        requestNewRoles(member.id).body("data", hasSize<Any>(1))

        // when & then
        requestNewRoles(member.id).body("data", hasSize<Any>(0))
    }

    @Test
    @DisplayName("피드를 5회 작성하면 다음 단계 칭호까지 부여된다")
    fun awardsNextTierAtFifthFeed() {
        // given
        val member = createAndSaveMember(memberId = "titleacqtester", nickname = "테스터", role = null)
        val board = createAndSaveBoard()

        // when
        repeat(5) { createFeed(member.id, board.id!!) }

        // then
        requestNewRoles(member.id)
            .body("data", hasSize<Any>(2))
            .body("data.role.name", hasItems("이 몸 등장", "영역전개"))
    }

    @Test
    @DisplayName("콘테스트 태그가 일치하는 피드를 작성하면 콘테스트 칭호가 부여된다")
    fun awardsContestParticipationTitle() {
        // given
        roleJpaRepository.save(RoleJpaEntity(name = "처음의 설레임", description = "콘테스트 1회 참여"))
        val member = createAndSaveMember(memberId = "titleacqtester", nickname = "테스터", role = null)
        val board = createAndSaveBoard()
        val contest = contestJpaRepository.save(
            ContestJpaEntity(
                title = "테스트 콘테스트",
                description = "설명",
                startedAt = LocalDateTime.now().minusDays(1),
                expiredAt = LocalDateTime.now().plusDays(1),
                status = ContestStatusType.ACTIVE,
                thumbnailUrl = "https://example.com/thumbnail.jpg"
            )
        )
        contestTagJpaRepository.save(ContestTagJpaEntity(title = "콘테스트태그", contest = contest))

        // when
        createFeed(member.id, board.id!!, tags = listOf("콘테스트태그"))

        // then
        requestNewRoles(member.id)
            .body("data.role.name", hasItems("이 몸 등장", "처음의 설레임"))
    }

    @Test
    @DisplayName("덕질노트를 조회하면 조회 칭호가 자동 부여되고 신규 칭호 조회에 나온다")
    fun awardsFanNoteViewTitle() {
        // given
        roleJpaRepository.save(RoleJpaEntity(name = "초보 오타쿠", description = "덕질노트 1회 열람"))
        val member = createAndSaveMember(memberId = "titleacqtester", nickname = "테스터", role = null)
        val fanNote = fanNoteJpaRepository.save(
            FanNoteJpaEntity(
                title = "덕질노트",
                subtitle = "서브타이틀",
                content = "내용",
                productionDate = LocalDate.of(2024, 1, 1),
                coverImageUrl = "https://example.com/cover.jpg"
            )
        )

        // when
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .headers(createAuthHeaders(member.id))
            .`when`()
            .get("/api/v1/fan-notes/${fanNote.id}")
            .then()
            .statusCode(200)

        // then
        requestNewRoles(member.id)
            .body("data", hasSize<Any>(1))
            .body("data[0].role.name", equalTo("초보 오타쿠"))
            .body("data[0].isRepresentative", equalTo(true))
    }

    @Test
    @DisplayName("테스트 컨텍스트의 칭호 부여 실행기는 동기 실행기로 대체되어 있다")
    fun titleAwardExecutorIsSynchronousInTests() {
        // when & then
        assertThat(titleAwardExecutor).isInstanceOf(SyncTaskExecutor::class.java)
    }

    private fun createFeed(memberPk: Long, boardId: Long, tags: List<String> = listOf("tag1")) {
        val request = CreateFeedRequestDto(
            title = "테스트 피드",
            content = "테스트 내용",
            boardId = boardId,
            tags = tags
        )

        RestAssured
            .given()
            .headers(createAuthHeaders(memberPk))
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
    }
}
