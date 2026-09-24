package com.example.mykku.contest.application.dto

import com.example.mykku.contest.domain.vo.ContestListFilter
import com.example.mykku.contest.domain.vo.ContestSortType
import org.springframework.data.domain.Pageable
import java.time.LocalDate
import java.time.LocalDateTime

data class CreateContestCommand(
    val title: String,
    val description: String?,
    val startedAt: LocalDateTime,
    val expiredAt: LocalDateTime,
    val thumbnailUrl: String,
    val images: List<ContestImageCommand>,
    val tags: List<String>
)

data class ContestImageCommand(
    val url: String,
    val orderIndex: Int
)

data class ContestListQuery(
    val filter: ContestListFilter,
    val sortType: ContestSortType,
    val page: Int,
    val size: Int
)

data class SetContestWinnersCommand(
    val contestId: Long,
    val winners: List<WinnerSelectionCommand>
)

data class WinnerSelectionCommand(
    val participationId: Long,
    val winnerRank: Int,
    val awardTitle: String?,
    val description: String
)

data class UpdateAcceptanceSpeechCommand(
    val winnerId: Long,
    val memberId: Long?,
    val acceptanceSpeech: String
)

data class GetMyWinnerStatusQuery(
    val contestId: Long,
    val memberId: Long
)

data class GetMyAwardsQuery(
    val memberId: Long,
    val pageable: Pageable
)

data class GetMyAwardFeedsQuery(
    val memberId: Long,
    val pageable: Pageable
)

data class GetMyAwardsPreviewQuery(
    val memberId: Long
)

data class UpsertContestWinnerAnnouncementCommand(
    val contestId: Long,
    val title: String,
    val content: String,
    val announcedAt: LocalDate
)
