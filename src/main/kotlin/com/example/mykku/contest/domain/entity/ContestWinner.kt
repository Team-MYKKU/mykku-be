package com.example.mykku.contest.domain.entity

import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.domain.vo.ContestParticipationId
import com.example.mykku.contest.domain.vo.ContestWinnerId
import java.time.LocalDateTime

class ContestWinner private constructor(
    val id: ContestWinnerId,
    val winnerRank: Int,
    val awardTitle: String?,
    val description: String,
    private var _acceptanceSpeech: String,
    val contestId: ContestId,
    val participationId: ContestParticipationId,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    val acceptanceSpeech: String
        get() = _acceptanceSpeech

    fun updateAcceptanceSpeech(speech: String) {
        _acceptanceSpeech = speech
    }

    fun reassign(winnerRank: Int, awardTitle: String?, description: String): ContestWinner {
        return reconstitute(
            id = id,
            winnerRank = winnerRank,
            awardTitle = awardTitle,
            description = description,
            acceptanceSpeech = _acceptanceSpeech,
            contestId = contestId,
            participationId = participationId,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun create(
            winnerRank: Int,
            awardTitle: String?,
            description: String,
            contestId: ContestId,
            participationId: ContestParticipationId
        ): ContestWinner {
            val now = LocalDateTime.now()
            return ContestWinner(
                id = ContestWinnerId(0),
                winnerRank = winnerRank,
                awardTitle = awardTitle,
                description = description,
                _acceptanceSpeech = "",
                contestId = contestId,
                participationId = participationId,
                createdAt = now,
                updatedAt = now
            )
        }

        fun reconstitute(
            id: ContestWinnerId,
            winnerRank: Int,
            awardTitle: String?,
            description: String,
            acceptanceSpeech: String,
            contestId: ContestId,
            participationId: ContestParticipationId,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime
        ): ContestWinner {
            return ContestWinner(
                id = id,
                winnerRank = winnerRank,
                awardTitle = awardTitle,
                description = description,
                _acceptanceSpeech = acceptanceSpeech,
                contestId = contestId,
                participationId = participationId,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
    }
}
