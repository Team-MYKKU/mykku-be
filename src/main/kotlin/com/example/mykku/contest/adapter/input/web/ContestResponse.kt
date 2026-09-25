package com.example.mykku.contest.adapter.input.web

import com.example.mykku.contest.application.dto.ContestDetailResult
import com.example.mykku.contest.application.dto.ContestImageResult
import com.example.mykku.contest.application.dto.ContestListResult
import com.example.mykku.contest.application.dto.ContestPreviewResult
import com.example.mykku.contest.application.dto.ContestWinnerAnnouncementResult
import com.example.mykku.contest.application.dto.ContestWinnerDetailResult
import com.example.mykku.contest.application.dto.ContestWinnerPreviewResult
import com.example.mykku.contest.application.dto.ContestWinnersListResult
import com.example.mykku.contest.application.dto.CreateContestResult
import com.example.mykku.contest.application.dto.MyAwardContestResult
import com.example.mykku.contest.application.dto.MyAwardPreviewResult
import com.example.mykku.contest.application.dto.PagedContestsResult
import com.example.mykku.contest.application.dto.PagedMyAwardsResult
import com.example.mykku.contest.application.dto.SetContestWinnersResult
import com.example.mykku.contest.application.dto.MyWinnerStatusResult
import com.example.mykku.contest.application.dto.UpdateAcceptanceSpeechResult
import com.example.mykku.contest.application.dto.WinnerDetailResult
import com.example.mykku.contest.application.dto.WinnerInfoResult
import com.example.mykku.contest.application.dto.WinnerThumbnailResult
import com.example.mykku.contest.domain.vo.ContestStatusType
import com.example.mykku.contest.domain.vo.ContestWinnerStatus
import java.time.LocalDate
import java.time.LocalDateTime

data class CreateContestResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val startedAt: LocalDateTime,
    val expiredAt: LocalDateTime,
    val thumbnailUrl: String,
    val images: List<ContestImageResponse>,
    val tags: List<String>,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(result: CreateContestResult): CreateContestResponse {
            return CreateContestResponse(
                id = result.id,
                title = result.title,
                description = result.description,
                startedAt = result.startedAt,
                expiredAt = result.expiredAt,
                thumbnailUrl = result.thumbnailUrl,
                images = result.images.map { ContestImageResponse.from(it) },
                tags = result.tags,
                createdAt = result.createdAt
            )
        }
    }
}

data class ContestImageResponse(
    val url: String,
    val orderIndex: Int
) {
    companion object {
        fun from(result: ContestImageResult): ContestImageResponse {
            return ContestImageResponse(
                url = result.url,
                orderIndex = result.orderIndex
            )
        }
    }
}

data class ContestListResponse(
    val id: Long,
    val title: String,
    val startedAt: LocalDateTime,
    val expiredAt: LocalDateTime,
    val status: ContestStatusType,
    val thumbnailUrl: String,
    val tags: List<String>,
    val winnerStatus: ContestWinnerStatus,
    val winnerRank: Int?
) {
    companion object {
        private val WINNER_STATUSES = setOf(ContestStatusType.WINNER_SELECTING, ContestStatusType.WINNER_SELECTED)

        fun from(result: ContestListResult): ContestListResponse {
            return ContestListResponse(
                id = result.id,
                title = result.title,
                startedAt = result.startedAt,
                expiredAt = result.expiredAt,
                status = if (result.status in WINNER_STATUSES) ContestStatusType.EXPIRED else result.status,
                thumbnailUrl = result.thumbnailUrl,
                tags = result.tags,
                winnerStatus = result.winnerStatus,
                winnerRank = result.winnerRank
            )
        }
    }
}

data class PagedContestsResponse(
    val content: List<ContestListResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val isLast: Boolean
) {
    companion object {
        fun from(result: PagedContestsResult): PagedContestsResponse {
            return PagedContestsResponse(
                content = result.content.map { ContestListResponse.from(it) },
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
                isLast = result.isLast
            )
        }
    }
}

data class ContestDetailResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val startedAt: LocalDateTime,
    val expiredAt: LocalDateTime,
    val status: ContestStatusType,
    val thumbnailUrl: String,
    val images: List<ContestImageResponse>,
    val tags: List<String>,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(result: ContestDetailResult): ContestDetailResponse {
            return ContestDetailResponse(
                id = result.id,
                title = result.title,
                description = result.description,
                startedAt = result.startedAt,
                expiredAt = result.expiredAt,
                status = result.status,
                thumbnailUrl = result.thumbnailUrl,
                images = result.images.map { ContestImageResponse.from(it) },
                tags = result.tags,
                createdAt = result.createdAt
            )
        }
    }
}

data class ContestPreviewResponse(
    val id: Long,
    val title: String,
    val thumbnailUrl: String
) {
    companion object {
        fun from(result: ContestPreviewResult): ContestPreviewResponse {
            return ContestPreviewResponse(
                id = result.id,
                title = result.title,
                thumbnailUrl = result.thumbnailUrl
            )
        }
    }
}

data class SetContestWinnersResponse(
    val contestId: Long,
    val contestTitle: String,
    val winners: List<WinnerInfoResponse>
) {
    companion object {
        fun from(result: SetContestWinnersResult): SetContestWinnersResponse {
            return SetContestWinnersResponse(
                contestId = result.contestId,
                contestTitle = result.contestTitle,
                winners = result.winners.map { WinnerInfoResponse.from(it) }
            )
        }
    }
}

data class WinnerInfoResponse(
    val winnerId: Long,
    val winnerRank: Int,
    val feedId: Long,
    val feedTitle: String,
    val authorNickname: String
) {
    companion object {
        fun from(result: WinnerInfoResult): WinnerInfoResponse {
            return WinnerInfoResponse(
                winnerId = result.winnerId,
                winnerRank = result.winnerRank,
                feedId = result.feedId,
                feedTitle = result.feedTitle,
                authorNickname = result.authorNickname
            )
        }
    }
}

data class ContestWinnersListResponse(
    val contests: List<ContestWinnerPreviewResponse>
) {
    companion object {
        fun from(result: ContestWinnersListResult): ContestWinnersListResponse {
            return ContestWinnersListResponse(
                contests = result.contests.map { ContestWinnerPreviewResponse.from(it) }
            )
        }
    }
}

data class ContestWinnerPreviewResponse(
    val contestId: Long,
    val contestTitle: String,
    val startedAt: LocalDateTime,
    val expiredAt: LocalDateTime,
    val winners: List<WinnerThumbnailResponse>
) {
    companion object {
        fun from(result: ContestWinnerPreviewResult): ContestWinnerPreviewResponse {
            return ContestWinnerPreviewResponse(
                contestId = result.contestId,
                contestTitle = result.contestTitle,
                startedAt = result.startedAt,
                expiredAt = result.expiredAt,
                winners = result.winners.map { WinnerThumbnailResponse.from(it) }
            )
        }
    }
}

data class WinnerThumbnailResponse(
    val winnerId: Long,
    val winnerRank: Int,
    val feedImageUrl: String
) {
    companion object {
        fun from(result: WinnerThumbnailResult): WinnerThumbnailResponse {
            return WinnerThumbnailResponse(
                winnerId = result.winnerId,
                winnerRank = result.winnerRank,
                feedImageUrl = result.feedImageUrl
            )
        }
    }
}

data class ContestWinnerDetailResponse(
    val contestId: Long,
    val contestTitle: String,
    val winners: List<WinnerDetailResponse>
) {
    companion object {
        fun from(result: ContestWinnerDetailResult): ContestWinnerDetailResponse {
            return ContestWinnerDetailResponse(
                contestId = result.contestId,
                contestTitle = result.contestTitle,
                winners = result.winners.map { WinnerDetailResponse.from(it) }
            )
        }
    }
}

data class WinnerDetailResponse(
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
) {
    companion object {
        fun from(result: WinnerDetailResult): WinnerDetailResponse {
            return WinnerDetailResponse(
                winnerId = result.winnerId,
                winnerRank = result.winnerRank,
                awardTitle = result.awardTitle,
                feedId = result.feedId,
                feedTitle = result.feedTitle,
                feedImageUrl = result.feedImageUrl,
                authorNickname = result.authorNickname,
                authorProfileImage = result.authorProfileImage,
                description = result.description,
                acceptanceSpeech = result.acceptanceSpeech
            )
        }
    }
}

data class UpdateAcceptanceSpeechResponse(
    val winnerId: Long,
    val acceptanceSpeech: String
) {
    companion object {
        fun from(result: UpdateAcceptanceSpeechResult): UpdateAcceptanceSpeechResponse {
            return UpdateAcceptanceSpeechResponse(
                winnerId = result.winnerId,
                acceptanceSpeech = result.acceptanceSpeech
            )
        }
    }
}

data class MyWinnerStatusResponse(
    val isWinner: Boolean,
    val winnerId: Long?,
    val winnerRank: Int?
) {
    companion object {
        fun from(result: MyWinnerStatusResult): MyWinnerStatusResponse {
            return MyWinnerStatusResponse(
                isWinner = result.isWinner,
                winnerId = result.winnerId,
                winnerRank = result.winnerRank
            )
        }
    }
}

data class MyAwardContestResponse(
    val contestId: Long,
    val contestTitle: String,
    val thumbnailUrl: String,
    val winnerRank: Int,
    val awardTitle: String?,
    val acceptanceSpeech: String,
    val feedId: Long,
    val feedTitle: String,
    val feedImageUrl: String?
) {
    companion object {
        fun from(result: MyAwardContestResult): MyAwardContestResponse {
            return MyAwardContestResponse(
                contestId = result.contestId,
                contestTitle = result.contestTitle,
                thumbnailUrl = result.thumbnailUrl,
                winnerRank = result.winnerRank,
                awardTitle = result.awardTitle,
                acceptanceSpeech = result.acceptanceSpeech,
                feedId = result.feedId,
                feedTitle = result.feedTitle,
                feedImageUrl = result.feedImageUrl
            )
        }
    }
}

data class PagedMyAwardsResponse(
    val content: List<MyAwardContestResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val isLast: Boolean
) {
    companion object {
        fun from(result: PagedMyAwardsResult): PagedMyAwardsResponse {
            return PagedMyAwardsResponse(
                content = result.content.map { MyAwardContestResponse.from(it) },
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
                isLast = result.isLast
            )
        }
    }
}

data class MyAwardPreviewResponse(
    val contestId: Long,
    val thumbnailUrl: String
) {
    companion object {
        fun from(result: MyAwardPreviewResult): MyAwardPreviewResponse {
            return MyAwardPreviewResponse(
                contestId = result.contestId,
                thumbnailUrl = result.thumbnailUrl
            )
        }
    }
}

data class ContestWinnerAnnouncementResponse(
    val contestId: Long,
    val contestTitle: String,
    val title: String,
    val content: String,
    val announcedAt: LocalDate
) {
    companion object {
        fun from(result: ContestWinnerAnnouncementResult): ContestWinnerAnnouncementResponse {
            return ContestWinnerAnnouncementResponse(
                contestId = result.contestId,
                contestTitle = result.contestTitle,
                title = result.title,
                content = result.content,
                announcedAt = result.announcedAt
            )
        }
    }
}
