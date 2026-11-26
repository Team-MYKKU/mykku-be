package com.example.mykku.contract

import com.example.mykku.contract.support.ContractGolden
import com.example.mykku.contract.support.ContractUpdateMode
import com.example.mykku.contract.support.GoldenManifest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("계약 골든 MANIFEST")
class GoldenManifestTest {

    private val manifest = GoldenManifest()

    @BeforeEach
    fun skipWhileUpdating() {
        assumeFalse(ContractUpdateMode.fromEnvironment().enabled, "갱신 모드가 끝날 때 MANIFEST를 다시 쓴다")
    }

    @Test
    fun `계약 골든·픽스처 파일은 MANIFEST에 적힌 해시와 같다`() {
        val recorded = manifest.read()
        val current = manifest.current()

        assertThat(recorded).`as`("MANIFEST.sha256이 없습니다. ${ContractGolden.UPDATE_COMMAND} 로 만듭니다").isNotNull
        assertThat(current)
            .`as`(MISMATCH_GUIDE)
            .containsExactlyInAnyOrderEntriesOf(recorded!!.entries)
    }

    @Test
    fun `MANIFEST 기준 커밋은 40자리 커밋 해시 형식이다`() {
        assertThat(GoldenManifest.isCommit(manifest.read()?.base.orEmpty())).isTrue()
    }

    @Test
    fun `계약 파일 경로는 대소문자를 무시해도 겹치지 않는다`() {
        assertThat(manifest.current().keys.map { it.lowercase() }).doesNotHaveDuplicates()
    }

    private companion object {
        const val MISMATCH_GUIDE = "골든을 갱신 모드 없이 고쳤거나 MANIFEST가 낡았습니다. 규약 보존 PR이면 골든을 되돌리고, " +
            "승인된 규약 변경이면 로컬에서 ${ContractGolden.UPDATE_COMMAND} 로 다시 만듭니다"
    }
}
