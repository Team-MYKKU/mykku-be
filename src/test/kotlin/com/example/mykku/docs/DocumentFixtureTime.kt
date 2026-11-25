package com.example.mykku.docs

import java.time.LocalDate
import java.time.LocalDateTime

object DocumentFixtureTime {
    val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 24, 15, 52, 36, 744_298_456)
    val NOW_MICROS: LocalDateTime = NOW.withNano(744_298_000)
    val NOW_SECONDS: LocalDateTime = NOW.withNano(0)
    val TODAY: LocalDate = NOW.toLocalDate()
}
