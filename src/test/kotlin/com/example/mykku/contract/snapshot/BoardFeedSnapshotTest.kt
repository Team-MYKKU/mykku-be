package com.example.mykku.contract.snapshot

import com.example.mykku.contract.snapshot.SnapshotFixtures.EMAIL_MEMBER
import com.example.mykku.contract.snapshot.SnapshotFixtures.GOOGLE
import com.example.mykku.contract.snapshot.SnapshotFixtures.imagePart
import com.example.mykku.contract.snapshot.SnapshotFixtures.jsonPart
import com.example.mykku.contract.snapshot.SnapshotFixtures.movePopularFeedsIntoWindow
import com.example.mykku.contract.snapshot.SnapshotMask.Companion.jvmTime
import com.example.mykku.contract.snapshot.SnapshotMask.Companion.uploadUrl
import io.restassured.response.Response
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("실경로 스냅샷: 게시판·피드·검색")
class BoardFeedSnapshotTest : ContractSnapshotTest() {

    override val fixtures = listOf(SnapshotFixtures.BASE)

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    fun `실경로 응답은 골든과 같다`(case: SnapshotCase) = verify(case)

    companion object {
        private const val SEARCH = "/api/v1/search/feeds"
        private const val FIRST_IMAGE_URL = "/data/images/0/url"
        private val KEYWORD = mapOf("keyword" to "스냅샷")
        private val NEW_FEED = mapOf(
            "title" to "새 피드",
            "content" to "새 피드 본문",
            "boardId" to 1,
            "tags" to listOf(" 공모태그 ", "새태그", "공모태그")
        )
        private val FEED_EDIT = mapOf(
            "title" to "수정된 첫 피드",
            "content" to "수정된 본문",
            "tags" to listOf("덕질", "새태그"),
            "deleteImageIds" to listOf(2)
        )

        @JvmStatic
        fun cases(): List<SnapshotCase> = boards() + feeds() + writes() + searches()

        private fun boards(): List<SnapshotCase> = listOf(
            SnapshotCase("board/list") { get("/api/v1/boards") },
            SnapshotCase("board/feeds-anonymous") { get("/api/v1/boards/1/feeds") },
            SnapshotCase("board/feeds-member") { get("/api/v1/boards/1/feeds", GOOGLE) },
            SnapshotCase("board/feeds-liker") { get("/api/v1/boards/1/feeds", Caller.Member(EMAIL_MEMBER)) },
            SnapshotCase("board/popular-anonymous") { popular(Caller.Anonymous) },
            SnapshotCase("board/popular-member") { popular(GOOGLE) },
            SnapshotCase("like/boards", SnapshotRules.PAGE) { get("/api/v1/likes/boards", GOOGLE) }
        )

        private fun feeds(): List<SnapshotCase> = listOf(
            SnapshotCase("feed/detail-anonymous") { get("/api/v1/feeds/1") },
            SnapshotCase("feed/detail-member") { get("/api/v1/feeds/1", Caller.Member(EMAIL_MEMBER)) },
            SnapshotCase("feed/detail-withdrawn-author") { get("/api/v1/feeds/2") },
            SnapshotCase("feed/comments-anonymous") { get("/api/v1/feeds/1/comments") },
            SnapshotCase("feed/comments-member") { get("/api/v1/feeds/1/comments", GOOGLE) }
        )

        private fun writes(): List<SnapshotCase> = listOf(
            SnapshotCase("feed/create", SnapshotRules.masks(jvmTime("/data/createdAt"), uploadUrl(FIRST_IMAGE_URL))) {
                multipart("POST", "/api/v1/feeds", GOOGLE, listOf(jsonPart("request", NEW_FEED), imagePart()))
            },
            SnapshotCase("feed/update", SnapshotRules.masks(uploadUrl("/data/images/1/url"))) {
                multipart("PATCH", "/api/v1/feeds/1", GOOGLE, listOf(jsonPart("request", FEED_EDIT), imagePart()))
            }
        )

        private fun searches(): List<SnapshotCase> = listOf(
            SnapshotCase("search/feeds-anonymous") { get(SEARCH, query = KEYWORD) },
            SnapshotCase("search/feeds-member") { get(SEARCH, GOOGLE, KEYWORD) },
            SnapshotCase("search/board-feeds-anonymous") { get("$SEARCH/boards/1", query = KEYWORD) }
        )

        private fun SnapshotSession.popular(caller: Caller): Response {
            movePopularFeedsIntoWindow()
            return get("/api/v1/boards/2/feeds/popular", caller)
        }
    }
}
