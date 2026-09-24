package com.example.mykku.contest.application.usecase

import com.example.mykku.contest.application.dto.ContestDetailResult
import com.example.mykku.contest.application.dto.ContestImageResult
import com.example.mykku.contest.application.port.input.GetContestUseCase
import com.example.mykku.contest.application.port.output.ContestImageRepository
import com.example.mykku.contest.application.port.output.ContestRepository
import com.example.mykku.contest.application.port.output.ContestTagRepository
import com.example.mykku.contest.domain.entity.Contest
import com.example.mykku.contest.domain.entity.ContestImage
import com.example.mykku.contest.domain.entity.ContestTag
import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.exception.ContestException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class GetContestUseCaseImpl(
    private val contestRepository: ContestRepository,
    private val contestImageRepository: ContestImageRepository,
    private val contestTagRepository: ContestTagRepository
) : GetContestUseCase {

    @Transactional(readOnly = true)
    override fun execute(contestId: Long, memberId: Long): ContestDetailResult {
        val contest = contestRepository.findById(ContestId.of(contestId))
            ?: throw ContestException.contestNotFound()

        val images = contestImageRepository.findByContestIds(listOf(contest.id))
        val tags = contestTagRepository.findByContestIds(listOf(contest.id))

        return toDetailResult(contest, images, tags)
    }

    private fun toDetailResult(
        contest: Contest,
        images: List<ContestImage>,
        tags: List<ContestTag>
    ): ContestDetailResult {
        return ContestDetailResult(
            id = contest.id.value,
            title = contest.title,
            description = contest.description,
            startedAt = contest.startedAt,
            expiredAt = contest.expiredAt,
            status = contest.resolveStatus(LocalDateTime.now()),
            thumbnailUrl = contest.thumbnailUrl,
            images = images.sortedBy { it.orderIndex }.map {
                ContestImageResult(url = it.url, orderIndex = it.orderIndex)
            },
            tags = tags.map { it.title },
            createdAt = contest.createdAt
        )
    }
}
