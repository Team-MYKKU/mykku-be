package com.example.mykku.event.domain.entity

import com.example.mykku.event.domain.vo.EventContent
import com.example.mykku.event.domain.vo.EventId
import com.example.mykku.event.domain.vo.EventStatusType
import com.example.mykku.event.exception.EventException
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class Event private constructor(
    val id: EventId,
    title: String,
    subTitle: String?,
    description: String?,
    startedAt: LocalDateTime,
    expiredAt: LocalDateTime,
    thumbnailUrl: String,
    private var _status: EventStatusType,
    val createdAt: LocalDateTime,
    updatedAt: LocalDateTime
) {
    var title: String = title
        private set

    var subTitle: String? = subTitle
        private set

    var description: String? = description
        private set

    var startedAt: LocalDateTime = startedAt
        private set

    var expiredAt: LocalDateTime = expiredAt
        private set

    var thumbnailUrl: String = thumbnailUrl
        private set

    var updatedAt: LocalDateTime = updatedAt
        private set

    val status: EventStatusType
        get() = _status

    fun update(content: EventContent, thumbnailUrl: String?) {
        validatePeriod(content.startedAt, content.expiredAt)
        validatePeriodEditable(content.startedAt, content.expiredAt)
        title = content.title
        subTitle = content.subTitle
        description = content.description
        startedAt = content.startedAt
        expiredAt = content.expiredAt
        thumbnailUrl?.let { this.thumbnailUrl = it }
        updatedAt = LocalDateTime.now()
    }

    private fun validatePeriodEditable(newStartedAt: LocalDateTime, newExpiredAt: LocalDateTime) {
        if (_status != EventStatusType.WINNER_SELECTED) return
        if (!sameSecond(newStartedAt, startedAt) || !sameSecond(newExpiredAt, expiredAt)) {
            throw EventException.periodLockedAfterWinnerSelected()
        }
    }

    private fun sameSecond(a: LocalDateTime, b: LocalDateTime): Boolean {
        return a.truncatedTo(ChronoUnit.SECONDS) == b.truncatedTo(ChronoUnit.SECONDS)
    }

    fun updateStatus(status: EventStatusType) {
        _status = status
    }

    fun resolveStatus(now: LocalDateTime): EventStatusType {
        if (_status == EventStatusType.WINNER_SELECTED) return EventStatusType.WINNER_SELECTED
        if (!expiredAt.isAfter(now)) return EventStatusType.EXPIRED
        return EventStatusType.ACTIVE
    }

    companion object {
        const val IMAGE_MAX_COUNT = 10

        fun validatePeriod(startedAt: LocalDateTime, expiredAt: LocalDateTime) {
            if (startedAt.isAfter(expiredAt)) {
                throw EventException.invalidEventPeriod()
            }
        }

        fun create(
            title: String,
            subTitle: String?,
            description: String?,
            startedAt: LocalDateTime,
            expiredAt: LocalDateTime,
            thumbnailUrl: String
        ): Event {
            val now = LocalDateTime.now()
            return Event(
                id = EventId(0),
                title = title,
                subTitle = subTitle,
                description = description,
                startedAt = startedAt,
                expiredAt = expiredAt,
                thumbnailUrl = thumbnailUrl,
                _status = EventStatusType.ACTIVE,
                createdAt = now,
                updatedAt = now
            )
        }

        fun reconstitute(
            id: EventId,
            title: String,
            subTitle: String?,
            description: String?,
            startedAt: LocalDateTime,
            expiredAt: LocalDateTime,
            thumbnailUrl: String,
            status: EventStatusType,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime
        ): Event {
            return Event(
                id = id,
                title = title,
                subTitle = subTitle,
                description = description,
                startedAt = startedAt,
                expiredAt = expiredAt,
                thumbnailUrl = thumbnailUrl,
                _status = status,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
    }
}
