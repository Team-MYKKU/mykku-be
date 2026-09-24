package com.example.mykku.contest.adapter.output.persistence.entity

import com.example.mykku.common.adapter.persistence.BaseJpaEntity
import com.example.mykku.contest.domain.entity.ContestWinner
import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.domain.vo.ContestParticipationId
import com.example.mykku.contest.domain.vo.ContestWinnerId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.DynamicUpdate

@Entity
@DynamicUpdate
@Table(name = "contest_winner")
class ContestWinnerJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "winner_rank")
    var winnerRank: Int,

    @Column(name = "award_title", length = 50)
    var awardTitle: String? = null,

    @Column(name = "description")
    var description: String = "",

    @Column(name = "acceptance_speech")
    var acceptanceSpeech: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id")
    val contest: ContestJpaEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participation_id")
    val participation: ContestParticipationJpaEntity
) : BaseJpaEntity() {

    fun toDomain(): ContestWinner {
        return ContestWinner.reconstitute(
            id = ContestWinnerId.of(id!!),
            winnerRank = winnerRank,
            awardTitle = awardTitle,
            description = description,
            acceptanceSpeech = acceptanceSpeech,
            contestId = ContestId.of(contest.id!!),
            participationId = ContestParticipationId.of(participation.id!!),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    fun updateAcceptanceSpeech(speech: String) {
        this.acceptanceSpeech = speech
    }

    companion object {
        fun fromDomain(
            winner: ContestWinner,
            contestJpaEntity: ContestJpaEntity,
            participationJpaEntity: ContestParticipationJpaEntity
        ): ContestWinnerJpaEntity {
            return ContestWinnerJpaEntity(
                id = if (winner.id.value == 0L) null else winner.id.value,
                winnerRank = winner.winnerRank,
                awardTitle = winner.awardTitle,
                description = winner.description,
                acceptanceSpeech = winner.acceptanceSpeech,
                contest = contestJpaEntity,
                participation = participationJpaEntity
            )
        }
    }
}
