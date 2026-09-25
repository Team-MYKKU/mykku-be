package com.example.mykku.contest.application.usecase

import com.example.mykku.contest.application.dto.ContestEditResult
import com.example.mykku.contest.application.port.input.GetContestForEditUseCase
import com.example.mykku.contest.application.port.output.ContestImageRepository
import com.example.mykku.contest.application.port.output.ContestRepository
import com.example.mykku.contest.application.port.output.ContestTagRepository
import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.exception.ContestException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class GetContestForEditUseCaseImpl(
    private val contestRepository: ContestRepository,
    private val contestImageRepository: ContestImageRepository,
    private val contestTagRepository: ContestTagRepository
) : GetContestForEditUseCase {

    @Transactional(readOnly = true)
    override fun execute(contestId: Long): ContestEditResult {
        val contest = contestRepository.findById(ContestId.of(contestId))
            ?: throw ContestException.contestNotFound()
        val ids = listOf(contest.id)
        return ContestEditResult(
            id = contest.id.value,
            title = contest.title,
            description = contest.description,
            startedAt = contest.startedAt,
            expiredAt = contest.expiredAt,
            status = contest.resolveStatus(LocalDateTime.now()),
            thumbnailUrl = contest.thumbnailUrl,
            imageUrls = contestImageRepository.findByContestIds(ids).sortedBy { it.orderIndex }.map { it.url },
            tags = contestTagRepository.findByContestIds(ids).sortedBy { it.id.value }.map { it.title }
        )
    }
}
