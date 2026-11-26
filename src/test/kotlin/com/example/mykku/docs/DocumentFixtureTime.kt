package com.example.mykku.docs

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object DocumentFixtureTime {
    val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 24, 15, 52, 36, 744_298_456)
    val NOW_MICROS: LocalDateTime = NOW.truncatedTo(ChronoUnit.MICROS)
    val NOW_SECONDS: LocalDateTime = NOW.truncatedTo(ChronoUnit.SECONDS)
    val TODAY: LocalDate = NOW.toLocalDate()
}
