package com.example.mykku.report.domain

import com.example.mykku.report.domain.vo.ReportReason
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ReportReason 테스트")
class ReportReasonTest {

    @Test
    @DisplayName("신고 사유는 클라이언트 화면 순서와 문구를 그대로 따른다")
    fun `신고 사유 순서와 문구`() {
        val reasons = ReportReason.entries.map { it.name to it.description }

        assertThat(reasons).containsExactly(
            "ABUSE" to "욕설 및 비방",
            "OBSCENE" to "음란하거나 부적절한 콘텐츠",
            "SPAM" to "반복 게시물, 광고 및 무분별한 홍보",
            "FRAUD" to "사칭 및 허위 정보",
            "PERSONAL_INFO" to "개인정보 노출",
            "COPYRIGHT" to "저작권 침해",
            "ETC" to "기타"
        )
    }

    @Test
    @DisplayName("마지막 사유는 항상 ETC다")
    fun `마지막은 ETC`() {
        assertThat(ReportReason.entries.last()).isEqualTo(ReportReason.ETC)
    }
}
