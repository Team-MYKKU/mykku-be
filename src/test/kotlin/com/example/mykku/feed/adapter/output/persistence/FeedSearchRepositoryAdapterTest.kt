package com.example.mykku.feed.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.block.adapter.output.persistence.entity.KeywordBlockJpaEntity
import com.example.mykku.block.adapter.output.persistence.entity.MemberBlockJpaEntity
import com.example.mykku.block.adapter.output.persistence.repository.KeywordBlockJpaRepository
import com.example.mykku.block.adapter.output.persistence.repository.MemberBlockJpaRepository
import com.example.mykku.board.adapter.output.persistence.entity.BoardJpaEntity
import com.example.mykku.feed.adapter.output.persistence.entity.FeedJpaEntity
import com.example.mykku.feed.application.dto.BoardMatchSummary
import com.example.mykku.feed.application.dto.FeedSearchCondition
import com.example.mykku.feed.application.port.output.FeedSearchRepository
import com.example.mykku.feed.domain.entity.Feed
import com.example.mykku.feed.domain.vo.SearchKeyword
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime

@DisplayName("FeedSearchRepositoryAdapter 통합 테스트")
class FeedSearchRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var feedSearchRepository: FeedSearchRepository

    @Autowired
    private lateinit var feedJpaRepository: FeedJpaRepository

    @Autowired
    private lateinit var memberBlockJpaRepository: MemberBlockJpaRepository

    @Autowired
    private lateinit var keywordBlockJpaRepository: KeywordBlockJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var viewer: MemberJpaEntity
    private lateinit var author: MemberJpaEntity
    private lateinit var board: BoardJpaEntity

    @BeforeEach
    fun setUp() {
        viewer = saveMember("viewer")
        author = saveMember("author")
        board = createAndSaveBoard(title = "검색 게시판")
    }

    @Nested
    @DisplayName("매칭 규칙")
    inner class Matching {

        @Test
        @DisplayName("제목에만 검색어가 있어도 매칭된다")
        fun `매칭 - 제목만 포함`() {
            val feed = saveFeed(title = "아이브 컴백 무대", content = "본문")

            assertThat(searchIds("컴백")).containsExactly(feed.id)
        }

        @Test
        @DisplayName("본문에만 검색어가 있어도 매칭된다")
        fun `매칭 - 본문만 포함`() {
            val feed = saveFeed(title = "제목", content = "오늘 컴백 무대 봤어요")

            assertThat(searchIds("컴백")).containsExactly(feed.id)
        }

        @Test
        @DisplayName("제목과 본문 어디에도 없으면 매칭되지 않는다")
        fun `매칭 - 포함하지 않음`() {
            saveFeed(title = "제목", content = "본문")

            assertThat(searchIds("컴백")).isEmpty()
        }

        @Test
        @DisplayName("대문자 검색어는 소문자로 정규화되어 대소문자 구분 없이 매칭된다")
        fun `매칭 - 대소문자 무시`() {
            val titleFeed = saveFeed(title = "IVE 컴백", content = "본문")
            val contentFeed = saveFeed(title = "제목", content = "IvE Comeback")

            assertThat(searchIds("IVE")).containsExactlyInAnyOrder(titleFeed.id, contentFeed.id)
        }

        @Test
        @DisplayName("1글자 검색어도 매칭된다")
        fun `매칭 - 1글자`() {
            val feed = saveFeed(title = "쿠키", content = "본문")

            assertThat(searchIds("쿠")).containsExactly(feed.id)
        }

        @Test
        @DisplayName("é로 검색하면 cafè는 매칭되지 않는다")
        fun `매칭 - 악센트 구분`() {
            saveFeed(title = "cafè 방문", content = "본문")
            saveFeed(title = "제목", content = "cafè 방문")
            val exact = saveFeed(title = "café 방문", content = "본문")

            assertThat(searchIds("é")).containsExactly(exact.id)
        }

        @Test
        @DisplayName("LIKE 특수문자는 글자 그대로 매칭된다")
        fun `매칭 - 특수문자 리터럴`() {
            val percent = saveFeed(title = "할인 100% 이벤트", content = "본문")
            val underscore = saveFeed(title = "snake_case 이야기", content = "본문")
            val backslash = saveFeed(title = "경로 C:\\temp", content = "본문")
            val quote = saveFeed(title = "it's 컴백", content = "본문")
            saveFeed(title = "할인 1000 이벤트", content = "snakecase tempo its")

            assertThat(searchIds("%")).containsExactly(percent.id)
            assertThat(searchIds("_")).containsExactly(underscore.id)
            assertThat(searchIds("\\")).containsExactly(backslash.id)
            assertThat(searchIds("'")).containsExactly(quote.id)
        }

        @Test
        @DisplayName("내부 공백은 검색어의 일부이고 앞뒤 공백과 줄바꿈은 제거된다")
        fun `매칭 - 공백 처리`() {
            val spaced = saveFeed(title = "아이브 컴백 무대", content = "본문")
            saveFeed(title = "아이브컴백 무대", content = "본문")

            assertThat(searchIds("  아이브 컴백\n")).containsExactly(spaced.id)
        }
    }

    @Nested
    @DisplayName("차단 필터")
    inner class BlockFilter {

        @Test
        @DisplayName("비로그인이면 차단 필터를 적용하지 않는다")
        fun `차단 - 비로그인`() {
            val blockedAuthor = saveMember("blocked")
            val feed = saveFeed(title = "스포 컴백", content = "본문", member = blockedAuthor)
            blockMember(viewer, blockedAuthor)
            blockKeyword(viewer, "스포")

            assertThat(searchIds("컴백", viewerId = null)).containsExactly(feed.id)
        }

        @Test
        @DisplayName("내가 차단한 회원과 나를 차단한 회원의 피드를 제외한다")
        fun `차단 - 회원 차단 양방향`() {
            val blockedByMe = saveMember("blockedbyme")
            val blockingMe = saveMember("blockingme")
            saveFeed(title = "컴백 1", content = "본문", member = blockedByMe)
            saveFeed(title = "컴백 2", content = "본문", member = blockingMe)
            val visible = saveFeed(title = "컴백 3", content = "본문")
            blockMember(viewer, blockedByMe)
            blockMember(blockingMe, viewer)

            assertThat(searchIds("컴백", viewerId = viewer.id)).containsExactly(visible.id)
        }

        @Test
        @DisplayName("차단 키워드가 제목이나 본문에 포함된 피드를 대소문자 구분 없이 제외한다")
        fun `차단 - 키워드 차단`() {
            saveFeed(title = "SPOILER 컴백", content = "본문")
            saveFeed(title = "컴백", content = "SPOILER 포함")
            saveFeed(title = "컴백", content = "스포 있음")
            val visible = saveFeed(title = "컴백 무대", content = "본문")
            blockKeyword(viewer, "spoiler")
            blockKeyword(viewer, "스포")

            assertThat(searchIds("컴백", viewerId = viewer.id)).containsExactly(visible.id)
        }

        @Test
        @DisplayName("다른 회원의 차단 키워드는 적용되지 않는다")
        fun `차단 - 다른 회원의 키워드`() {
            val feed = saveFeed(title = "스포 컴백", content = "본문")
            blockKeyword(author, "스포")

            assertThat(searchIds("컴백", viewerId = viewer.id)).containsExactly(feed.id)
        }

        @Test
        @DisplayName("차단 키워드 é는 cafè 피드를 숨기지 않는다")
        fun `차단 - 키워드 악센트 구분`() {
            val cafe = saveFeed(title = "cafè 방문", content = "본문")
            val contentCafe = saveFeed(title = "후기", content = "cafè 방문")
            blockKeyword(viewer, "é")

            assertThat(searchIds("caf", viewerId = viewer.id)).containsExactlyInAnyOrder(cafe.id, contentCafe.id)
        }

        @Test
        @DisplayName("회원 차단이 있어도 탈퇴 회원의 피드는 포함된다")
        fun `차단 - 탈퇴 회원 피드 포함`() {
            val blockedAuthor = saveMember("blocked")
            saveFeed(title = "컴백 1", content = "본문", member = blockedAuthor)
            val withdrawn = saveFeed(title = "컴백 2", content = "본문", member = null)
            blockMember(viewer, blockedAuthor)
            blockMember(author, viewer)

            val feeds = searchFeeds("컴백", viewerId = viewer.id)

            assertThat(feeds.map { it.id?.value }).containsExactly(withdrawn.id)
            assertThat(feeds.single().memberId).isNull()
        }

        @Test
        @DisplayName("검색어와 같은 차단 키워드가 있으면 결과가 비어 있다")
        fun `차단 - 검색어와 같은 차단 키워드`() {
            saveFeed(title = "컴백", content = "본문")
            blockKeyword(viewer, "컴백")

            assertThat(searchIds("컴백", viewerId = viewer.id)).isEmpty()
        }
    }

    @Nested
    @DisplayName("summarizeByBoard 메서드")
    inner class SummarizeByBoard {

        @Test
        @DisplayName("게시판별 매칭 수와 가장 최근 매칭 피드의 작성 시각을 반환한다")
        fun `게시판별 요약 - 개수와 최신 시각`() {
            val otherBoard = createAndSaveBoard(title = "다른 게시판")
            val latest = LocalDateTime.of(2025, 3, 3, 12, 0)
            val otherLatest = LocalDateTime.of(2025, 3, 2, 12, 0)
            saveFeedAt(board, "컴백 1", LocalDateTime.of(2025, 3, 1, 12, 0))
            saveFeedAt(board, "컴백 2", latest)
            saveFeedAt(board, "무관한 글", LocalDateTime.of(2025, 3, 9, 12, 0))
            saveFeedAt(otherBoard, "컴백 3", otherLatest)
            entityManager.clear()

            val summaries = summarize("컴백", viewerId = null, board, otherBoard)

            assertThat(summaries).containsExactlyInAnyOrder(
                BoardMatchSummary(boardId = board.id!!, count = 2, latestCreatedAt = latest),
                BoardMatchSummary(boardId = otherBoard.id!!, count = 1, latestCreatedAt = otherLatest)
            )
        }

        @Test
        @DisplayName("차단된 피드는 개수와 최신 시각에서 제외한다")
        fun `게시판별 요약 - 차단 반영`() {
            val blockedAuthor = saveMember("blocked")
            val visibleAt = LocalDateTime.of(2025, 3, 1, 12, 0)
            saveFeedAt(board, "컴백 1", visibleAt)
            val blockedFeed = saveFeed(title = "컴백 2", content = "본문", member = blockedAuthor)
            updateCreatedAt(blockedFeed, LocalDateTime.of(2025, 3, 5, 12, 0))
            blockMember(viewer, blockedAuthor)
            entityManager.clear()

            val summaries = summarize("컴백", viewerId = viewer.id, board)

            assertThat(summaries).containsExactly(
                BoardMatchSummary(boardId = board.id!!, count = 1, latestCreatedAt = visibleAt)
            )
        }

        @Test
        @DisplayName("매칭된 피드가 없는 게시판은 반환하지 않는다")
        fun `게시판별 요약 - 매칭 없음`() {
            saveFeed(title = "무관한 글", content = "본문")

            assertThat(summarize("컴백", viewerId = null, board)).isEmpty()
        }
    }

    @Nested
    @DisplayName("findTopByBoard 메서드")
    inner class FindTopByBoard {

        @Test
        @DisplayName("게시판의 매칭 피드를 최신순으로 limit개만 반환한다")
        fun `미리보기 - limit과 최신순`() {
            val oldest = saveFeedAt(board, "컴백 1", LocalDateTime.of(2025, 3, 1, 12, 0))
            val middle = saveFeedAt(board, "컴백 2", LocalDateTime.of(2025, 3, 2, 12, 0))
            val newest = saveFeedAt(board, "컴백 3", LocalDateTime.of(2025, 3, 3, 12, 0))
            entityManager.clear()

            val feeds = feedSearchRepository.findTopByBoard(condition("컴백"), board.id!!, 2)

            assertThat(feeds.map { it.id?.value }).containsExactly(newest.id, middle.id)
            assertThat(oldest.id).isNotIn(feeds.map { it.id?.value })
        }

        @Test
        @DisplayName("작성 시각이 같으면 id가 큰 피드가 먼저 온다")
        fun `미리보기 - 동점은 id 내림차순`() {
            val sameTime = LocalDateTime.of(2025, 3, 1, 12, 0)
            val first = saveFeedAt(board, "컴백 1", sameTime)
            val second = saveFeedAt(board, "컴백 2", sameTime)
            val third = saveFeedAt(board, "컴백 3", sameTime)
            entityManager.clear()

            val feeds = feedSearchRepository.findTopByBoard(condition("컴백"), board.id!!, 3)

            assertThat(feeds.map { it.id?.value }).containsExactly(third.id, second.id, first.id)
        }

        @Test
        @DisplayName("다른 게시판의 매칭 피드는 반환하지 않는다")
        fun `미리보기 - 게시판 한정`() {
            val otherBoard = createAndSaveBoard(title = "다른 게시판")
            saveFeed(title = "컴백", content = "본문", feedBoard = otherBoard)

            assertThat(feedSearchRepository.findTopByBoard(condition("컴백"), board.id!!, 3)).isEmpty()
        }
    }

    @Nested
    @DisplayName("findByBoard 메서드")
    inner class FindByBoard {

        @Test
        @DisplayName("차단을 페이지 계산 전에 적용해 전체 개수와 페이지 수가 정확하다")
        fun `더보기 - 차단 후 페이지 메타`() {
            val blockedAuthor = saveMember("blocked")
            val visible = (1..3).map { saveFeedAt(board, "컴백 $it", LocalDateTime.of(2025, 3, it, 12, 0)) }
            repeat(2) { saveFeed(title = "컴백 차단", content = "본문", member = blockedAuthor) }
            blockMember(viewer, blockedAuthor)
            entityManager.clear()

            val first = feedSearchRepository.findByBoard(condition("컴백", viewer.id), board.id!!, PageRequest.of(0, 2))
            val second = feedSearchRepository.findByBoard(condition("컴백", viewer.id), board.id!!, PageRequest.of(1, 2))

            assertThat(first.totalElements).isEqualTo(3)
            assertThat(first.totalPages).isEqualTo(2)
            assertThat(first.content.map { it.id?.value }).containsExactly(visible[2].id, visible[1].id)
            assertThat(second.content.map { it.id?.value }).containsExactly(visible[0].id)
        }

        @Test
        @DisplayName("비로그인이면 차단된 회원의 피드도 개수에 포함한다")
        fun `더보기 - 비로그인 페이지 메타`() {
            val blockedAuthor = saveMember("blocked")
            repeat(3) { saveFeed(title = "컴백 $it", content = "본문") }
            repeat(2) { saveFeed(title = "컴백 차단", content = "본문", member = blockedAuthor) }
            blockMember(viewer, blockedAuthor)

            val page = feedSearchRepository.findByBoard(condition("컴백"), board.id!!, PageRequest.of(0, 2))

            assertThat(page.totalElements).isEqualTo(5)
            assertThat(page.totalPages).isEqualTo(3)
            assertThat(page.content).hasSize(2)
        }

        @Test
        @DisplayName("마지막 페이지를 넘어가면 빈 목록과 정확한 전체 개수를 반환한다")
        fun `더보기 - 범위 밖 페이지`() {
            repeat(3) { saveFeed(title = "컴백 $it", content = "본문") }

            val page = feedSearchRepository.findByBoard(condition("컴백"), board.id!!, PageRequest.of(5, 2))

            assertThat(page.content).isEmpty()
            assertThat(page.totalElements).isEqualTo(3)
            assertThat(page.totalPages).isEqualTo(2)
        }

        @Test
        @DisplayName("나를 차단한 회원과 차단 키워드도 페이지 계산 전에 적용해 전체 개수와 페이지 수가 정확하다")
        fun `더보기 - 역방향 회원 차단과 키워드 차단 후 페이지 메타`() {
            val blockingMe = saveMember("blockingme")
            repeat(3) { saveFeed(title = "컴백 $it", content = "본문") }
            saveFeed(title = "컴백 역차단", content = "본문", member = blockingMe)
            saveFeed(title = "컴백 스포", content = "본문")
            blockMember(blockingMe, viewer)
            blockKeyword(viewer, "스포")

            val page = feedSearchRepository.findByBoard(condition("컴백", viewer.id), board.id!!, PageRequest.of(0, 2))

            assertThat(page.totalElements).isEqualTo(3)
            assertThat(page.totalPages).isEqualTo(2)
        }

        @Test
        @DisplayName("작성 시각이 같으면 id가 큰 피드가 먼저 오고 페이지 경계에서도 순서가 유지된다")
        fun `더보기 - 동점은 id 내림차순`() {
            val sameTime = LocalDateTime.of(2025, 3, 1, 12, 0)
            val feeds = (1..3).map { saveFeedAt(board, "컴백 $it", sameTime) }
            entityManager.clear()

            val first = feedSearchRepository.findByBoard(condition("컴백"), board.id!!, PageRequest.of(0, 2))
            val second = feedSearchRepository.findByBoard(condition("컴백"), board.id!!, PageRequest.of(1, 2))

            assertThat(first.content.map { it.id?.value }).containsExactly(feeds[2].id, feeds[1].id)
            assertThat(second.content.map { it.id?.value }).containsExactly(feeds[0].id)
        }
    }

    private fun condition(raw: String, viewerId: Long? = null): FeedSearchCondition {
        return FeedSearchCondition(keyword = SearchKeyword.of(raw), viewerId = viewerId)
    }

    private fun searchFeeds(raw: String, viewerId: Long? = null): List<Feed> {
        return feedSearchRepository.findByBoard(condition(raw, viewerId), board.id!!, PageRequest.of(0, 20)).content
    }

    private fun searchIds(raw: String, viewerId: Long? = null): List<Long?> {
        return searchFeeds(raw, viewerId).map { it.id?.value }
    }

    private fun summarize(raw: String, viewerId: Long?, vararg boards: BoardJpaEntity): List<BoardMatchSummary> {
        val boardIds = boards.map { it.id }
        return feedSearchRepository.summarizeByBoard(condition(raw, viewerId)).filter { it.boardId in boardIds }
    }

    private fun saveMember(name: String): MemberJpaEntity {
        val key = "sr-$name"
        return createAndSaveMember(memberId = key, email = "$key@example.com", socialId = "social-$key")
    }

    private fun saveFeed(
        title: String,
        content: String,
        member: MemberJpaEntity? = author,
        feedBoard: BoardJpaEntity = board
    ): FeedJpaEntity {
        return feedJpaRepository.save(
            FeedJpaEntity(title = title, content = content, board = feedBoard, member = member)
        )
    }

    private fun saveFeedAt(feedBoard: BoardJpaEntity, title: String, createdAt: LocalDateTime): FeedJpaEntity {
        val feed = saveFeed(title = title, content = "본문", feedBoard = feedBoard)
        updateCreatedAt(feed, createdAt)
        return feed
    }

    private fun updateCreatedAt(feed: FeedJpaEntity, createdAt: LocalDateTime) {
        entityManager.flush()
        jdbcTemplate.update("UPDATE feed SET created_at = ? WHERE id = ?", createdAt, feed.id)
    }

    private fun blockMember(blocker: MemberJpaEntity, blocked: MemberJpaEntity) {
        memberBlockJpaRepository.save(MemberBlockJpaEntity(blocker = blocker, blocked = blocked))
    }

    private fun blockKeyword(member: MemberJpaEntity, keyword: String) {
        keywordBlockJpaRepository.save(KeywordBlockJpaEntity(member = member, keyword = keyword))
    }
}
