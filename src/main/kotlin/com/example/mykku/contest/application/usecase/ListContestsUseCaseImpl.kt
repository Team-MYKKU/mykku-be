package com.example.mykku.contest.application.usecase

import com.example.mykku.common.util.PageableValidator
import com.example.mykku.contest.application.dto.ContestListQuery
import com.example.mykku.contest.application.dto.ContestListResult
import com.example.mykku.contest.application.dto.PagedContestsResult
import com.example.mykku.contest.application.port.input.ListContestsUseCase
import com.example.mykku.contest.application.port.output.ContestImageRepository
import com.example.mykku.contest.application.port.output.ContestRepository
import com.example.mykku.contest.application.port.output.ContestTagRepository
import com.example.mykku.contest.domain.entity.Contest
import com.example.mykku.contest.domain.entity.ContestTag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ListContestsUseCaseImpl(
    private val contestRepository: ContestRepository,
    private val contestImageRepository: ContestImageRepository,
    private val contestTagRepository: ContestTagRepository
) : ListContestsUseCase {

    @Transactional(readOnly = true)
    override fun execute(query: ContestListQuery): PagedContestsResult {
        val pageable = PageableValidator.validateAndCreate(query.page, query.size)
        val now = LocalDateTime.now()

        val contestPage = contestRepository.findWithPagination(query.filter, query.sortType, pageable, now)

        val contestIds = contestPage.content.map { it.id }
        val tagsByContestId = contestTagRepository.findByContestIds(contestIds)
            .groupBy { it.contestId.value }

        val contestListResults = contestPage.content.map { contest ->
            toContestListResult(contest, tagsByContestId, now)
        }
        return PagedContestsResult.of(contestPage, contestListResults)
    }

    private fun toContestListResult(
        contest: Contest,
        tagsByContestId: Map<Long, List<ContestTag>>,
        now: LocalDateTime
    ): ContestListResult {
        val tags = tagsByContestId[contest.id.value] ?: emptyList()

        return ContestListResult(
            id = contest.id.value,
            title = contest.title,
            startedAt = contest.startedAt,
            expiredAt = contest.expiredAt,
            status = contest.resolveStatus(now),
            thumbnailUrl = contest.thumbnailUrl,
            tags = tags.map { it.title }
        )
    }
}
