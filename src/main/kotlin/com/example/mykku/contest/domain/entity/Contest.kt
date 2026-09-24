package com.example.mykku.contest.domain.entity

import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.domain.vo.ContestStatusType
import java.time.LocalDateTime

class Contest private constructor(
    val id: ContestId,
    val title: String,
    val description: String?,
    val startedAt: LocalDateTime,
    val expiredAt: LocalDateTime,
    val thumbnailUrl: String,
    private var _status: ContestStatusType,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    val status: ContestStatusType
        get() = _status

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
