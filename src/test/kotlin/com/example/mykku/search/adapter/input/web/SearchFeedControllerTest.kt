package com.example.mykku.search.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.block.adapter.output.persistence.entity.KeywordBlockJpaEntity
import com.example.mykku.block.adapter.output.persistence.entity.MemberBlockJpaEntity
import com.example.mykku.block.adapter.output.persistence.repository.KeywordBlockJpaRepository
import com.example.mykku.block.adapter.output.persistence.repository.MemberBlockJpaRepository
import com.example.mykku.board.adapter.output.persistence.entity.BoardJpaEntity
import com.example.mykku.board.exception.BoardErrorCode
import com.example.mykku.common.exception.CommonErrorCode
import com.example.mykku.feed.adapter.output.persistence.FeedImageJpaRepository
import com.example.mykku.feed.adapter.output.persistence.FeedJpaRepository
import com.example.mykku.feed.adapter.output.persistence.entity.FeedImageJpaEntity
import com.example.mykku.feed.adapter.output.persistence.entity.FeedJpaEntity
import com.example.mykku.feed.exception.FeedErrorCode
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import io.restassured.RestAssured
import io.restassured.response.ValidatableResponse
import io.restassured.specification.RequestSpecification
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime

@DisplayName("SearchFeedController 통합 테스트")
class SearchFeedControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var feedJpaRepository: FeedJpaRepository

    @Autowired
    private lateinit var feedImageJpaRepository: FeedImageJpaRepository

    @Autowired
    private lateinit var memberBlockJpaRepository: MemberBlockJpaRepository

    @Autowired
    private lateinit var keywordBlockJpaRepository: KeywordBlockJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var viewer: MemberJpaEntity
    private lateinit var author: MemberJpaEntity
    private lateinit var board: BoardJpaEntity

    @BeforeEach
    fun setUp() {
        viewer = saveMember("viewer")
        author = saveMember("author")
        board = createAndSaveBoard(title = "아이브", logo = "ive_logo.png")
    }

    @Nested
    @DisplayName("게시판별 검색 결과 조회")
    inner class SearchFeeds {

        @Test
        @DisplayName("매칭 수가 많은 게시판부터, 같으면 최근 매칭이 더 최신인 게시판부터 묶어서 반환한다")
        fun `searchFeeds - 게시판별 그룹과 그룹 순서`() {
            val recent = createAndSaveBoard(title = "르세라핌")
            val older = createAndSaveBoard(title = "뉴진스")
            saveFeedsAt(board, 4, BASE_TIME)
            saveFeedsAt(older, 2, BASE_TIME.plusDays(1))
            saveFeedsAt(recent, 2, BASE_TIME.plusDays(2))
            saveFeed(createAndSaveBoard(title = "기타"), title = "무관한 글")

            search("컴백")
                .statusCode(200)
                .body("message", equalTo("피드 검색 결과를 성공적으로 조회했습니다."))
                .body("data.boards.boardId", contains(board.id!!.toInt(), recent.id!!.toInt(), older.id!!.toInt()))
                .body("data.boards.totalCount", contains(4, 2, 2))
                .body("data.boards.hasMore", contains(true, false, false))
        }

        @Test
        @DisplayName("그룹마다 최신순 최대 3건을 미리보기로 주고 게시판 정보를 채운다")
        fun `searchFeeds - 미리보기 3건과 게시판 정보`() {
            val feeds = saveFeedsAt(board, 4, BASE_TIME)

            search("컴백")
                .statusCode(200)
                .body("data.boards[0].boardTitle", equalTo("아이브"))
                .body("data.boards[0].boardLogo", equalTo("ive_logo.png"))
                .body("data.boards[0].feeds.id", contains(*idsOf(feeds.reversed().take(3))))
                .body("data.boards[0].feeds[0].boardId", equalTo(board.id!!.toInt()))
                .body("data.boards[0].feeds[0].boardTitle", equalTo("아이브"))
                .body("data.boards[0].feeds[0].title", equalTo("컴백 3"))
                .body("data.boards[0].feeds[0].content", equalTo("본문"))
        }

        @Test
        @DisplayName("썸네일은 id가 가장 작은 이미지이고 이미지가 없으면 null이다")
        fun `searchFeeds - 썸네일`() {
            val withImages = saveFeedAt(board, "컴백 이미지", BASE_TIME.plusDays(1))
            saveFeedAt(board, "컴백 텍스트", BASE_TIME)
            saveImage(withImages, "first.jpg")
            saveImage(withImages, "second.jpg")

            search("컴백")
                .statusCode(200)
                .body("data.boards[0].feeds.thumbnailUrl", contains("first.jpg", null))
        }

        @Test
        @DisplayName("제목만 또는 본문만 매칭되어도 포함한다")
        fun `searchFeeds - 제목 또는 본문 매칭`() {
            val titleOnly = saveFeedAt(board, "컴백 무대", BASE_TIME.plusDays(1))
            val contentOnly = saveFeed(board, title = "오늘", content = "컴백했어요")
            updateCreatedAt(contentOnly, BASE_TIME)

            search("컴백")
                .statusCode(200)
                .body("data.boards[0].feeds.id", contains(*idsOf(listOf(titleOnly, contentOnly))))
        }

        @Test
        @DisplayName("대문자 검색어 IVE는 소문자 제목 ive 컴백과 매칭되고 1글자 검색도 된다")
        fun `searchFeeds - 대소문자 무시와 1글자`() {
            val feed = saveFeed(board, title = "ive 컴백")

            search("IVE").statusCode(200).body("data.boards[0].feeds.id", contains(feed.id!!.toInt()))
            search("컴").statusCode(200).body("data.boards[0].feeds.id", contains(feed.id!!.toInt()))
        }

        @Test
        @DisplayName("é로 검색하면 cafè는 매칭되지 않고 차단 키워드 é도 cafè를 숨기지 않는다")
        fun `searchFeeds - 악센트 구분`() {
            val cafe = saveFeed(board, title = "café 방문")
            val accent = saveFeed(board, title = "cafè 방문")
            blockKeyword(viewer, "é")

            search("é").statusCode(200).body("data.boards[0].feeds.id", contains(cafe.id!!.toInt()))
            search("cafè", viewer).statusCode(200).body("data.boards[0].feeds.id", contains(accent.id!!.toInt()))
        }

        @Test
        @DisplayName("매칭되는 피드가 없으면 빈 boards를 반환한다")
        fun `searchFeeds - 결과 없음`() {
            saveFeed(board, title = "무관한 글")

            search("컴백")
                .statusCode(200)
                .body("data.boards", empty<Any>())
        }

        @Test
        @DisplayName("앞뒤 공백과 줄바꿈은 제거하고 중간 공백은 구절의 일부로 비교한다")
        fun `searchFeeds - 공백 처리`() {
            val phrase = saveFeed(board, title = "아이브 컴백 무대")
            saveFeed(board, title = "아이브컴백 무대")

            search("  아이브 컴백\n")
                .statusCode(200)
                .body("data.boards[0].totalCount", equalTo(1))
                .body("data.boards[0].feeds.id", contains(phrase.id!!.toInt()))
        }

        @Test
        @DisplayName("비로그인 요청에는 차단 필터가 적용되지 않는다")
        fun `searchFeeds - 비로그인 차단 미적용`() {
            val blocked = saveMember("blocked")
            saveFeed(board, title = "컴백", member = blocked)
            saveFeed(board, title = "컴백 스포")
            blockMember(viewer, blocked)
            blockKeyword(viewer, "스포")

            search("컴백")
                .statusCode(200)
                .body("data.boards[0].totalCount", equalTo(2))
        }

        @Test
        @DisplayName("로그인 요청은 내가 차단한 회원, 나를 차단한 회원, 차단 키워드 피드를 빼고 개수를 센다")
        fun `searchFeeds - 로그인 차단 적용`() {
            val visible = saveFeedAt(board, "컴백 무대", BASE_TIME)
            val iBlocked = saveMember("iblocked")
            val blocksMe = saveMember("blocksme")
            saveFeed(board, title = "컴백", member = iBlocked)
            saveFeed(board, title = "컴백", member = blocksMe)
            saveFeed(board, title = "컴백 스포")
            blockMember(viewer, iBlocked)
            blockMember(blocksMe, viewer)
            blockKeyword(viewer, "스포")

            search("컴백", viewer)
                .statusCode(200)
                .body("data.boards[0].totalCount", equalTo(1))
                .body("data.boards[0].hasMore", equalTo(false))
                .body("data.boards[0].feeds.id", contains(visible.id!!.toInt()))
        }

        @Test
        @DisplayName("차단 키워드와 같은 검색어로 검색하면 빈 boards를 반환한다")
        fun `searchFeeds - 검색어가 차단 키워드`() {
            saveFeed(board, title = "스포 주의")
            blockKeyword(viewer, "스포")

            search("스포", viewer)
                .statusCode(200)
                .body("data.boards", empty<Any>())
        }

        @Test
        @DisplayName("회원 차단이 있는 로그인 사용자에게도 탈퇴 회원의 피드는 포함된다")
        fun `searchFeeds - 탈퇴 회원 피드 포함`() {
            val withdrawn = saveFeed(board, title = "컴백", member = null)
            blockMember(viewer, saveMember("blocked"))
            blockMember(saveMember("blocker"), viewer)

            search("컴백", viewer)
                .statusCode(200)
                .body("data.boards[0].feeds.id", contains(withdrawn.id!!.toInt()))
        }

        @Test
        @DisplayName("keyword가 없으면 C105를 반환한다")
        fun `searchFeeds - 검색어 누락`() {
            RestAssured.given()
                .`when`()
                .get(SEARCH_URL)
                .then()
                .statusCode(400)
                .body("code", equalTo(CommonErrorCode.MISSING_REQUEST_PARAMETER.code))
        }

        @Test
        @DisplayName("공백만 있는 검색어는 FD109를 반환한다")
        fun `searchFeeds - 공백 검색어`() {
            search("   ")
                .statusCode(400)
                .body("code", equalTo(FeedErrorCode.SEARCH_KEYWORD_EMPTY.code))
        }

        @Test
        @DisplayName("51자 검색어는 FD110을 반환하고 50자는 허용한다")
        fun `searchFeeds - 검색어 길이`() {
            search("가".repeat(51))
                .statusCode(400)
                .body("code", equalTo(FeedErrorCode.SEARCH_KEYWORD_TOO_LONG.code))
            search("가".repeat(50)).statusCode(200)
        }
    }

    @Nested
    @DisplayName("게시판 안 검색 결과 조회")
    inner class SearchBoardFeeds {

        @Test
        @DisplayName("한 게시판의 매칭 피드를 최신순으로 페이지네이션한다")
        fun `searchBoardFeeds - 페이지네이션`() {
            val feeds = saveFeedsAt(board, 3, BASE_TIME)
            saveFeed(createAndSaveBoard(title = "다른 게시판"), title = "컴백")

            searchBoard(board.id!!, "컴백", page = 0, size = 2)
                .statusCode(200)
                .body("message", equalTo("게시판 피드 검색 결과를 성공적으로 조회했습니다."))
                .body("data.feeds.id", contains(*idsOf(feeds.reversed().take(2))))
                .body("data.feeds[0].boardTitle", equalTo("아이브"))
                .body("data.currentPage", equalTo(0))
                .body("data.totalPages", equalTo(2))
                .body("data.totalElements", equalTo(3))
                .body("data.size", equalTo(2))
                .body("data.hasNext", equalTo(true))
                .body("data.hasPrevious", equalTo(false))
        }

        @Test
        @DisplayName("마지막 페이지를 넘는 page는 빈 feeds와 실제 전체 개수를 반환한다")
        fun `searchBoardFeeds - 마지막 페이지 초과`() {
            saveFeedsAt(board, 3, BASE_TIME)

            searchBoard(board.id!!, "컴백", page = 5, size = 2)
                .statusCode(200)
                .body("data.feeds", empty<Any>())
                .body("data.currentPage", equalTo(5))
                .body("data.totalElements", equalTo(3))
                .body("data.totalPages", equalTo(2))
                .body("data.hasNext", equalTo(false))
        }

        @Test
        @DisplayName("로그인 요청은 차단 필터를 페이지 전에 적용해 totalElements가 정확하다")
        fun `searchBoardFeeds - 차단 후 개수`() {
            val visible = saveFeed(board, title = "컴백")
            val blocked = saveMember("blocked")
            saveFeed(board, title = "컴백", member = blocked)
            blockMember(viewer, blocked)

            searchBoard(board.id!!, "컴백", viewer = viewer)
                .statusCode(200)
                .body("data.feeds.id", contains(visible.id!!.toInt()))
                .body("data.feeds[0].thumbnailUrl", nullValue())
                .body("data.totalElements", equalTo(1))
                .body("data.hasNext", equalTo(false))
        }

        @Test
        @DisplayName("없는 게시판이면 BO001을 반환한다")
        fun `searchBoardFeeds - 없는 게시판`() {
            searchBoard(999_999L, "컴백")
                .statusCode(404)
                .body("code", equalTo(BoardErrorCode.BOARD_NOT_FOUND.code))
        }

        @Test
        @DisplayName("없는 게시판이어도 검색어 검증이 먼저라 FD109, FD110을 반환한다")
        fun `searchBoardFeeds - 검색어 검증 우선`() {
            searchBoard(999_999L, " ")
                .statusCode(400)
                .body("code", equalTo(FeedErrorCode.SEARCH_KEYWORD_EMPTY.code))
            searchBoard(999_999L, "a".repeat(51))
                .statusCode(400)
                .body("code", equalTo(FeedErrorCode.SEARCH_KEYWORD_TOO_LONG.code))
        }

        @Test
        @DisplayName("keyword가 없으면 C105를 반환한다")
        fun `searchBoardFeeds - 검색어 누락`() {
            RestAssured.given()
                .`when`()
                .get("$SEARCH_URL/boards/{boardId}", board.id)
                .then()
                .statusCode(400)
                .body("code", equalTo(CommonErrorCode.MISSING_REQUEST_PARAMETER.code))
        }
    }

    private fun search(keyword: String, viewer: MemberJpaEntity? = null): ValidatableResponse {
        return givenViewer(viewer)
            .queryParam("keyword", keyword)
            .`when`()
            .get(SEARCH_URL)
            .then()
            .log().ifValidationFails()
    }

    private fun searchBoard(
        boardId: Long,
        keyword: String,
        page: Int = 0,
        size: Int = 20,
        viewer: MemberJpaEntity? = null
    ): ValidatableResponse {
        return givenViewer(viewer)
            .queryParam("keyword", keyword)
            .queryParam("page", page)
            .queryParam("size", size)
            .`when`()
            .get("$SEARCH_URL/boards/{boardId}", boardId)
            .then()
            .log().ifValidationFails()
    }

    private fun givenViewer(viewer: MemberJpaEntity?): RequestSpecification {
        val spec = RestAssured.given()
        return viewer?.let { spec.headers(createAuthHeaders(it.id)) } ?: spec
    }

    private fun idsOf(feeds: List<FeedJpaEntity>): Array<Int> = feeds.map { it.id!!.toInt() }.toTypedArray()

    private fun saveMember(name: String): MemberJpaEntity {
        return createAndSaveMember(memberId = name, email = "$name@example.com", socialId = "social-$name")
    }

    private fun saveFeed(
        feedBoard: BoardJpaEntity,
        title: String,
        content: String = "본문",
        member: MemberJpaEntity? = author
    ): FeedJpaEntity {
        return feedJpaRepository.save(
            FeedJpaEntity(title = title, content = content, board = feedBoard, member = member)
        )
    }

    private fun saveFeedsAt(feedBoard: BoardJpaEntity, count: Int, from: LocalDateTime): List<FeedJpaEntity> {
        return (0 until count).map { saveFeedAt(feedBoard, "컴백 $it", from.plusHours(it.toLong())) }
    }

    private fun saveFeedAt(feedBoard: BoardJpaEntity, title: String, createdAt: LocalDateTime): FeedJpaEntity {
        val feed = saveFeed(feedBoard, title = title)
        updateCreatedAt(feed, createdAt)
        return feed
    }

    private fun updateCreatedAt(feed: FeedJpaEntity, createdAt: LocalDateTime) {
        jdbcTemplate.update("UPDATE feed SET created_at = ? WHERE id = ?", createdAt, feed.id)
    }

    private fun saveImage(feed: FeedJpaEntity, url: String) {
        feedImageJpaRepository.save(FeedImageJpaEntity(url = url, width = 100, height = 100, feed = feed))
    }

    private fun blockMember(blocker: MemberJpaEntity, blocked: MemberJpaEntity) {
        memberBlockJpaRepository.save(MemberBlockJpaEntity(blocker = blocker, blocked = blocked))
    }

    private fun blockKeyword(member: MemberJpaEntity, keyword: String) {
        keywordBlockJpaRepository.save(KeywordBlockJpaEntity(member = member, keyword = keyword))
    }

    companion object {
        private const val SEARCH_URL = "/api/v1/search/feeds"
        private val BASE_TIME: LocalDateTime = LocalDateTime.of(2025, 3, 1, 12, 0)
    }
}
