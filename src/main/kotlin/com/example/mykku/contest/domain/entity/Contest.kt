package com.example.mykku.contest.domain.entity

import com.example.mykku.contest.domain.vo.ContestContent
import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.domain.vo.ContestStatusType
import com.example.mykku.contest.exception.ContestException
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class Contest private constructor(
    val id: ContestId,
    title: String,
    description: String?,
    startedAt: LocalDateTime,
    expiredAt: LocalDateTime,
    thumbnailUrl: String,
    private var _status: ContestStatusType,
    val createdAt: LocalDateTime,
    updatedAt: LocalDateTime
) {
    var title: String = title
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

    val status: ContestStatusType
        get() = _status

    fun update(content: ContestContent, thumbnailUrl: String?, tagsChanged: Boolean) {
        validatePeriod(content.startedAt, content.expiredAt)
        validateEditable(content, tagsChanged)
        title = content.title
        description = content.description
        startedAt = content.startedAt
        expiredAt = content.expiredAt
        thumbnailUrl?.let { this.thumbnailUrl = it }
        updatedAt = LocalDateTime.now()
    }

    private fun validateEditable(content: ContestContent, tagsChanged: Boolean) {
        if (_status != ContestStatusType.WINNER_SELECTED) return
        val periodChanged = !sameSecond(content.startedAt, startedAt) || !sameSecond(content.expiredAt, expiredAt)
        if (periodChanged || tagsChanged) {
            throw ContestException.periodOrTagsLockedAfterWinnerSelected()
        }
    }

    private fun sameSecond(a: LocalDateTime, b: LocalDateTime): Boolean {
        return a.truncatedTo(ChronoUnit.SECONDS) == b.truncatedTo(ChronoUnit.SECONDS)
    }

    fun updateStatus(status: ContestStatusType) {
        _status = status
    }

    fun resolveStatus(now: LocalDateTime): ContestStatusType {
        if (_status == ContestStatusType.WINNER_SELECTED) return ContestStatusType.WINNER_SELECTED
        if (!expiredAt.isAfter(now)) return ContestStatusType.EXPIRED
        return ContestStatusType.ACTIVE
    }

    companion object {
        const val IMAGE_MAX_COUNT = 10
        const val TAG_MAX_COUNT = 7

        fun validatePeriod(startedAt: LocalDateTime, expiredAt: LocalDateTime) {
            if (startedAt.isAfter(expiredAt)) {
                throw ContestException.invalidContestPeriod()
            }
        }

        fun create(
            title: String,
            description: String?,
            startedAt: LocalDateTime,
            expiredAt: LocalDateTime,
            thumbnailUrl: String
        ): Contest {
            val now = LocalDateTime.now()
            return Contest(
                id = ContestId(0),
                title = title,
                description = description,
                startedAt = startedAt,
                expiredAt = expiredAt,
                thumbnailUrl = thumbnailUrl,
                _status = ContestStatusType.ACTIVE,
                createdAt = now,
                updatedAt = now
            )
        }

        fun reconstitute(
            id: ContestId,
            title: String,
            description: String?,
            startedAt: LocalDateTime,
            expiredAt: LocalDateTime,
            thumbnailUrl: String,
            status: ContestStatusType,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime
        ): Contest {
            return Contest(
                id = id,
                title = title,
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
