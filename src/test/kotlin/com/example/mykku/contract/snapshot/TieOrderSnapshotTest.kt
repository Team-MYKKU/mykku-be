package com.example.mykku.contract.snapshot

import com.example.mykku.contract.snapshot.SnapshotFixtures.setFeedTime
import com.example.mykku.contract.support.ContractViolationError
import com.example.mykku.contract.support.JsonPath
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.core.io.ClassPathResource
import java.nio.charset.StandardCharsets.UTF_8

@DisplayName("실경로 스냅샷: 동률 정렬")
class TieOrderSnapshotTest : ContractSnapshotTest() {

    override val fixtures = emptyList<String>()

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    fun `동률 행의 관측 순서와 페이지 간 중복·누락은 골든과 같다`(case: TieCase) {
        val log = prepared(case)
        val observed = case.sections.map { observe(case, it, log) }
        val text = observed.joinToString("\n") { it.golden }
        log.write("ties/${case.golden}.txt", observed.joinToString("\n") { it.explain })

        golden.violationOf(SnapshotGoldens.tie(case.golden), text)?.let {
            throw ContractViolationError("$it\n- MySQL VERSION(): ${log.mysqlVersion()}\n--- 관측 전체 ---\n$text")
        }
    }

    private fun prepared(case: TieCase): StatementLog {
        loadFixtures(listOf(case.fixture))
        case.prepare(this)
        return StatementLog(jdbcTemplate).also { it.analyze(tablesOf(case.fixture)) }
    }

    private fun observe(case: TieCase, section: TieSection, log: StatementLog): TieObservation {
        val title = TieOrder.title(section, case.fixture)
        val pagings = if (section.paged) TieOrder.COMPARED_PAGES + TieOrder.FULL_PAGE else listOf(null)
        val captured = pagings.map { paging -> log.capture { page(section, paging, log) } }
        val pages = captured.map { it.first }
        val compared = if (section.paged) pages.dropLast(1) else pages
        val explain = log.explain(title, captured.flatMap { it.second })
        return TieObservation(TieOrder.render(title, pages, compared), explain)
    }

    private fun page(section: TieSection, paging: TiePaging?, log: StatementLog): TiePage {
        val response = get(section.path, section.caller, section.query + paging?.query.orEmpty())
        if (response.statusCode != SUCCESS) {
            throw ContractViolationError(
                "[API 계약 위반] 동률 요청 실패: ${lastRequestLine()} -> ${response.statusCode} ${response.asString()}" +
                    "\n- MySQL VERSION(): ${log.mysqlVersion()}"
            )
        }
        val ids = JsonPath.select(jsonOf(response), section.idPath).map { it.asText() }
        return TiePage(paging?.label ?: "전체", ids)
    }

    private fun tablesOf(fixture: String): Set<String> {
        val sql = ClassPathResource("contract/fixtures/$fixture").inputStream.use { String(it.readAllBytes(), UTF_8) }
        return INSERT_TABLE.findAll(sql).map { it.groupValues[1] }.toSet()
    }

    companion object {
        private const val SUCCESS = 200
        private val INSERT_TABLE = Regex("INSERT INTO ([a-z_]+)")
        private val VIEWER = Caller.Member(1)
        private val BLOCKER = Caller.Member(2)
        private val KEYWORD = mapOf("keyword" to "동률")
        private val SORT_TYPES = listOf("LATEST", "OLDEST", "POPULAR")
        private const val NO_SORT_KEY = "101~105(정렬 키 없음)"
        private const val RANKS = "winner_rank 101=1 102~104=2 105=3"
        private const val POPULAR_ROWS = "좋아요 101=1 102~104=2 105=3, 작성 시각 101=D-3 102~104=D-2 105=D-1"
        private const val COMMENTS = "/api/v1/feeds/101/comments"
        private val FEEDS = fixture("feeds")
        private val CONTENTS = fixture("contents")
        private val MEMBER_LISTS = fixture("member-lists")
        private val POPULAR = TieSection(
            "/api/v1/boards/101/feeds/popular", "/data/feeds/*/id", paged = false, rows = POPULAR_ROWS
        )

        @JvmStatic
        fun cases(): List<TieCase> = feedCases() + contentCases() + listCases() + awardCases()

        private fun feedCases(): List<TieCase> = listOf(
            TieCase("board-feeds", FEEDS, listOf(TieSection("/api/v1/boards/101/feeds", "/data/feeds/*/id"))),
            TieCase("my-feeds", FEEDS, listOf(TieSection("/api/v1/members/me/feeds", "/data/feeds/*/id", VIEWER))),
            TieCase("feed-comments", FEEDS, listOf(
                TieSection(COMMENTS, "/data/comments/*/id"),
                TieSection(COMMENTS, "/data/comments/*/id", BLOCKER)
            )),
            TieCase("popular-feeds", FEEDS, listOf(POPULAR), prepare = { moveTieFeedsIntoWindow() }),
            TieCase("search", FEEDS, listOf(
                TieSection("/api/v1/search/feeds/boards/101", "/data/feeds/*/id", query = KEYWORD),
                TieSection("/api/v1/search/feeds", "/data/boards/*/feeds/*/id", query = KEYWORD, paged = false)
            ))
        )

        private fun contentCases(): List<TieCase> = listOf(
            TieCase("daily-message-comments", CONTENTS, listOf(
                TieSection("/api/v1/daily-messages/101/comments", "/data/comments/*/id"),
                TieSection("/api/v1/daily-messages/101/comments", "/data/comments/*/id", BLOCKER)
            )),
            TieCase("notifications", CONTENTS, listOf(
                TieSection("/api/v1/notifications", "/data/content/*/id", VIEWER),
                TieSection("/api/v1/notifications/unread", "/data/content/*/id", VIEWER)
            )),
            TieCase("fan-notes", CONTENTS, listOf(TieSection("/api/v1/fan-notes", "/data/content/*/id"))),
            memberListCase("favorite-boards", "/api/v1/likes/boards", "/data/content/*/id"),
            memberListCase("member-blocks", "/api/v1/blocks/members", "/data/blocks/*/id"),
            memberListCase("keyword-blocks", "/api/v1/blocks/keywords", "/data/blocks/*/id")
        )

        private fun listCases(): List<TieCase> = listOf(
            statusCase("contests-active", "/api/v1/contests", listOf("ACTIVE", "ALL")),
            statusCase("contests-expired", "/api/v1/contests", listOf("EXPIRED")),
            statusCase("events-active", "/api/v1/events", listOf("ACTIVE", "ALL")),
            statusCase("events-expired", "/api/v1/events", listOf("EXPIRED"))
        )

        private fun awardCases(): List<TieCase> = winnerCases() + listOf(
            TieCase("my-contests", fixture("my-contests"), listOf(
                TieSection("/api/v1/members/me/contests", "/data/content/*/id", VIEWER)
            )),
            TieCase("my-events", fixture("my-events"), listOf(
                TieSection("/api/v1/members/me/events", "/data/content/*/id", VIEWER),
                TieSection("/api/v1/events/my-awards", "/data/content/*/eventId", VIEWER)
            )),
            TieCase("my-award-feeds", fixture("my-award-feeds"), listOf(
                TieSection("/api/v1/contests/my-awards/feeds", "/data/feeds/*/id", VIEWER),
                TieSection("/api/v1/contests/my-awards", "/data/content/*/contestId", VIEWER)
            ))
        )

        private fun winnerCases(): List<TieCase> = listOf(
            TieCase("contest-winners", fixture("contest-winners"), listOf(
                TieSection("/api/v1/contests/winners", "/data/contests/*/contestId", paged = false, rows = NO_SORT_KEY),
                TieSection("/api/v1/contests/101/winners", "/data/winners/*/winnerId", paged = false, rows = RANKS)
            )),
            TieCase("event-winners", fixture("event-winners"), listOf(
                TieSection("/api/v1/events/101/winners", "/data/winners/*/winnerId", paged = false, rows = NO_SORT_KEY)
            ))
        )

        private fun statusCase(golden: String, path: String, statuses: List<String>): TieCase =
            TieCase(golden, fixture(golden), statuses.flatMap { status ->
                SORT_TYPES.map { sortType ->
                    TieSection(path, "/data/content/*/id", VIEWER, mapOf("status" to status, "sortType" to sortType))
                }
            })

        private fun memberListCase(golden: String, path: String, idPath: String): TieCase =
            TieCase(golden, MEMBER_LISTS, listOf(TieSection(path, idPath, VIEWER)))

        private fun fixture(name: String): String = "snapshot-ties-$name.sql"

        private fun SnapshotSession.moveTieFeedsIntoWindow() {
            val daysAgo = mapOf(101L to 3L, 102L to 2L, 103L to 2L, 104L to 2L, 105L to 1L)
            daysAgo.forEach { (feedId, days) -> setFeedTime(feedId, fixtureDate.minusDays(days).atTime(10, 0)) }
        }
    }
}
