package com.example.mykku.contract.snapshot

import com.example.mykku.contract.snapshot.SnapshotFixtures.GOOGLE
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("실경로 스냅샷: 콘테스트·이벤트")
class ContestEventSnapshotTest : ContractSnapshotTest() {

    override val fixtures = listOf(SnapshotFixtures.BASE)

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    fun `실경로 응답은 골든과 같다`(case: SnapshotCase) = verify(case)

    companion object {
        private const val CONTESTS = "/api/v1/contests"
        private const val EVENTS = "/api/v1/events"
        private val ALL_OLDEST = mapOf("status" to "ALL", "sortType" to "OLDEST")

        @JvmStatic
        fun cases(): List<SnapshotCase> = contestLists() + contestAwards() + events()

        private fun contestLists(): List<SnapshotCase> = listOf(
            SnapshotCase("contest/list-default") { get(CONTESTS, GOOGLE) },
            SnapshotCase("contest/list-all-oldest") { get(CONTESTS, GOOGLE, ALL_OLDEST) },
            SnapshotCase("contest/list-expired") { get(CONTESTS, GOOGLE, mapOf("status" to "EXPIRED")) },
            SnapshotCase("contest/detail") { get("$CONTESTS/2", GOOGLE) },
            SnapshotCase("contest/my-participations") { get("/api/v1/members/me/contests", GOOGLE) }
        )

        private fun contestAwards(): List<SnapshotCase> = listOf(
            SnapshotCase("contest/winners") { get("$CONTESTS/winners") },
            SnapshotCase("contest/winner-detail") { get("$CONTESTS/1/winners") },
            SnapshotCase("contest/winner-announcement") { get("$CONTESTS/1/winner-announcement") },
            SnapshotCase("contest/my-winner-status") { get("$CONTESTS/1/my-winner-status", GOOGLE) },
            SnapshotCase("contest/my-awards") { get("$CONTESTS/my-awards", GOOGLE) },
            SnapshotCase("contest/my-award-feeds") { get("$CONTESTS/my-awards/feeds", GOOGLE) }
        )

        private fun events(): List<SnapshotCase> = listOf(
            SnapshotCase("event/list-default") { get(EVENTS, GOOGLE) },
            SnapshotCase("event/list-all-oldest") { get(EVENTS, GOOGLE, ALL_OLDEST) },
            SnapshotCase("event/detail") { get("$EVENTS/1", GOOGLE) },
            SnapshotCase("event/winners") { get("$EVENTS/1/winners") },
            SnapshotCase("event/winner-announcement") { get("$EVENTS/1/winner-announcement") },
            SnapshotCase("event/my-awards") { get("$EVENTS/my-awards", GOOGLE) },
            SnapshotCase("event/my-participations") { get("/api/v1/members/me/events", GOOGLE) }
        )
    }
}
