package com.example.mykku.contest.application.dto

import com.example.mykku.contest.domain.vo.ContestStatusType
import com.example.mykku.contest.domain.vo.ContestWinnerStatus
import org.springframework.data.domain.Page
import java.time.LocalDate
import java.time.LocalDateTime

data class CreateContestResult(
    val id: Long,
    val title: String,
    val description: String?,
    val startedAt: LocalDateTime,
    val expiredAt: LocalDateTime,
    val thumbnailUrl: String,
    val images: List<ContestImageResult>,
    val tags: List<String>,
    val createdAt: LocalDateTime
)

data class ContestImageResult(
    val url: String,
    val orderIndex: Int
)

data class ContestListResult(
    val id: Long,
    val title: String,
    val startedAt: LocalDateTime,
    val expiredAt: LocalDateTime,
    val status: ContestStatusType,
    val thumbnailUrl: String,
    val tags: List<String>,
    val winnerStatus: ContestWinnerStatus = ContestWinnerStatus.PENDING,
    val winnerRank: Int? = null
)

data class PagedContestsResult(
    val content: List<ContestListResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val isLast: Boolean
) {
    companion object {
        fun <T> of(page: Page<T>, content: List<ContestListResult>): PagedContestsResult {
            return PagedContestsResult(
                content = content,
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                isLast = page.isLast
            )
        }
    }
}

data class ContestDetailResult(
    val id: Long,
    val title: String,
    val description: String?,
    val startedAt: LocalDateTime,
    val expiredAt: LocalDateTime,
    val status: ContestStatusType,
    val thumbnailUrl: String,
    val images: List<ContestImageResult>,
    val tags: List<String>,
    val createdAt: LocalDateTime
)

data class ContestPreviewResult(
    val id: Long,
    val title: String,
    val thumbnailUrl: String
)

data class SetContestWinnersResult(
    val contestId: Long,
    val contestTitle: String,
    val winners: List<WinnerInfoResult>
)

data class WinnerInfoResult(
    val winnerId: Long,
    val winnerRank: Int,
    val feedId: Long,
    val feedTitle: String,
    val authorNickname: String
)

data class ContestWinnersListResult(
    val contests: List<ContestWinnerPreviewResult>
)

data class ContestWinnerPreviewResult(
    val contestId: Long,
    val contestTitle: String,
    val startedAt: LocalDateTime,
    val expiredAt: LocalDateTime,
    val winners: List<WinnerThumbnailResult>
)

data class WinnerThumbnailResult(
    val winnerId: Long,
    val winnerRank: Int,
    val feedImageUrl: String
)

data class ContestWinnerDetailResult(
    val contestId: Long,
    val contestTitle: String,
    val winners: List<WinnerDetailResult>
)

data class WinnerDetailResult(
    val winnerId: Long,
    val winnerRank: Int,
    val awardTitle: String?,
    val feedId: Long,
    val feedTitle: String,
    val feedImageUrl: String,
    val authorNickname: String,
    val authorProfileImage: String,
    val description: String,
    val acceptanceSpeech: String
)

data class UpdateAcceptanceSpeechResult(
    val winnerId: Long,
    val acceptanceSpeech: String
)

data class MyWinnerStatusResult(
    val isWinner: Boolean,
    val winnerId: Long?,
    val winnerRank: Int?
)

data class ContestWinnerAnnouncementResult(
    val contestId: Long,
    val contestTitle: String,
    val title: String,
    val content: String,
    val announcedAt: LocalDate
)

data class MyAwardContestResult(
    val contestId: Long,
    val contestTitle: String,
    val thumbnailUrl: String,
    val winnerRank: Int,
    val awardTitle: String?,
    val acceptanceSpeech: String,
    val feedId: Long,
    val feedTitle: String,
    val feedImageUrl: String?
)

data class PagedMyAwardsResult(
    val content: List<MyAwardContestResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val isLast: Boolean
)

data class MyAwardPreviewResult(
    val contestId: Long,
    val thumbnailUrl: String
)
