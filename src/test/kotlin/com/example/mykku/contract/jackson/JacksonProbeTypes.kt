package com.example.mykku.contract.jackson

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

enum class ProbeStatus(private val label: String) {
    FIRST("첫째"),
    SECOND("둘째");

    override fun toString(): String = label
}

enum class ProbeLevel { LOW, HIGH }

data class BooleanNamesProbe(val isRead: Boolean, val isActive: Boolean?, val isbn: String, val hasNext: Boolean)

data class NestedProbe(val id: Long)

data class NullsProbe(val text: String?, val number: Long?, val list: List<String>?, val nested: NestedProbe?)

data class EmptiesProbe(
    val list: List<String>,
    val map: Map<String, Long>,
    val text: String,
    val nested: List<NestedProbe>
)

data class DateTimesProbe(
    val fraction0: LocalDateTime,
    val fraction2: LocalDateTime,
    val fraction6: LocalDateTime,
    val fraction9: LocalDateTime,
    val secondsZero: LocalDateTime,
    val date: LocalDate
)

data class EnumsProbe(val status: ProbeStatus, val statuses: List<ProbeStatus>, val missing: ProbeStatus?)

data class NumbersProbe(
    val longMax: Long,
    val longMin: Long,
    val intMax: Int,
    val integralDouble: Double,
    val fractionDouble: Double,
    val scaledDecimal: BigDecimal
)

data class PageItemProbe(val id: Long, val name: String)

data class TextProbe(val value: String)

data class NullableTextProbe(val value: String?)

data class DefaultTextProbe(val value: String = "기본값")

data class LongProbe(val value: Long)

data class NullableLongProbe(val value: Long?)

data class IntProbe(val value: Int)

data class EnumProbe(val value: ProbeLevel)

data class LabeledEnumProbe(val value: ProbeStatus)

data class DateTimeProbe(val value: LocalDateTime)

data class ListProbe(val values: List<String>)

data class BooleanNameProbe(val isActive: Boolean)
