package com.example.mykku.contract.snapshot

import com.example.mykku.contract.support.ContractUpdateMode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("실경로 스냅샷 골든 완전성")
class SnapshotGoldenCoverageTest {

    @BeforeEach
    fun skipWhileUpdating() {
        assumeFalse(ContractUpdateMode.fromEnvironment().enabled, "갱신 모드에서는 골든을 쓰는 중이라 건너뛴다")
    }

    @Test
    fun `실경로 응답 골든 디렉터리에는 지금 비교하는 파일만 있다`() {
        assertThat(SnapshotGoldens.filesUnder(SnapshotGoldens.RESPONSES))
            .containsExactlyInAnyOrderElementsOf(declaredResponseGoldens())
    }

    @Test
    fun `gray-zone의 snapshot- 접두 골든은 지금 비교하는 파일만 있다`() {
        val declared = GrayZoneSnapshotTest.cases().map { SnapshotGoldens.grayZone(it.golden) }
        val prefix = "${SnapshotGoldens.GRAY_ZONE}/${SnapshotGoldens.GRAY_ZONE_PREFIX}"

        assertThat(SnapshotGoldens.filesUnder(SnapshotGoldens.GRAY_ZONE).filter { it.startsWith(prefix) })
            .containsExactlyInAnyOrderElementsOf(declared)
    }

    @Test
    fun `snapshot- 접두 픽스처는 모두 어떤 시나리오가 읽는다`() {
        val declared = listOf(SnapshotFixtures.BASE) + GrayZoneSnapshotTest.cases().flatMap { it.fixtures } +
            TieOrderSnapshotTest.cases().map { it.fixture }
        val fixtures = SnapshotGoldens.filesUnder(FIXTURES).map { it.removePrefix("$FIXTURES/") }
            .filter { it.startsWith(FIXTURE_PREFIX) }

        assertThat(fixtures).containsExactlyInAnyOrderElementsOf(declared.toSet())
    }

    private fun declaredResponseGoldens(): List<String> =
        SNAPSHOT_CASES.flatMap { SnapshotGoldens.of(it) } +
            PrecisionProbeTest.probes().map { SnapshotGoldens.precision(it.golden) } +
            TieOrderSnapshotTest.cases().map { SnapshotGoldens.tie(it.golden) }

    private companion object {
        const val FIXTURES = "fixtures"
        const val FIXTURE_PREFIX = "snapshot-"
        val SNAPSHOT_CASES: List<SnapshotCase>
            get() = AuthMemberSnapshotTest.cases() + BoardFeedSnapshotTest.cases() +
                ContestEventSnapshotTest.cases() + ContentSnapshotTest.cases()
    }
}
