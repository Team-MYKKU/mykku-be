package com.example.mykku.util

import com.example.mykku.common.util.KstTime
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.time.Clock
import java.time.LocalDate

class KstDayGuard(private val clock: Clock) {

    fun today(): LocalDate = KstTime.now(clock).toLocalDate()

    fun assumeStillToday(today: LocalDate) {
        val now = today()
        assumeTrue(now == today) { "KST 자정을 넘겨 오늘 날짜가 바뀌었으므로 건너뛴다: $today → $now" }
    }
}
