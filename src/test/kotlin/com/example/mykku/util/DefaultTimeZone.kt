package com.example.mykku.util

import java.time.ZoneId
import java.util.TimeZone

object DefaultTimeZone {

    fun <T> with(zone: String, action: () -> T): T {
        val original = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone(zone))
        try {
            check(ZoneId.systemDefault() == ZoneId.of(zone)) { "JVM 기본 시간대를 $zone 으로 바꾸지 못했다" }
            return action()
        } finally {
            TimeZone.setDefault(original)
        }
    }
}
