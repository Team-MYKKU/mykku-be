package com.example.mykku.common.util

import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

object KstTime {
    val KST: ZoneId = ZoneId.of("Asia/Seoul")

    fun now(clock: Clock): LocalDateTime = LocalDateTime.ofInstant(clock.instant(), KST)
}
