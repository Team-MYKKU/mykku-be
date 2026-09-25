package com.example.mykku.contest.application.usecase

import com.example.mykku.contest.application.dto.ContestImageResult
import com.example.mykku.contest.application.dto.CreateContestCommand
import com.example.mykku.contest.application.dto.CreateContestResult
import com.example.mykku.contest.application.port.input.CreateContestUseCase
import com.example.mykku.contest.application.port.output.ContestImageRepository
import com.example.mykku.contest.application.port.output.ContestRepository
import com.example.mykku.contest.application.port.output.ContestTagRepository
import com.example.mykku.contest.domain.entity.Contest
import com.example.mykku.contest.domain.entity.ContestImage
import com.example.mykku.contest.domain.entity.ContestTag
import com.example.mykku.contest.exception.ContestException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateContestUseCaseImpl(
    private val contestRepository: ContestRepository,
    private val contestImageRepository: ContestImageRepository,
    private val contestTagRepository: ContestTagRepository
) : CreateContestUseCase {

    @Transactional
    override fun execute(command: CreateContestCommand): CreateContestResult {
        Contest.validatePeriod(command.startedAt, command.expiredAt)
        validateImages(command)
        val normalizedTags = ContestTag.normalizeAndValidate(command.tags)

        val contest = createAndSaveContest(command)
        val images = createAndSaveImages(command, contest)
        val tags = createAndSaveTags(normalizedTags, contest)

        return buildResult(contest, images, tags)
    }

    private fun validateImages(command: CreateContestCommand) {
        if (command.images.size > Contest.IMAGE_MAX_COUNT) {
            throw ContestException.contestImageLimitExceeded()
        }
    }

    private fun createAndSaveContest(command: CreateContestCommand): Contest {
        val contest = Contest.create(
            title = command.title,
            description = command.description,
            startedAt = command.startedAt,
            expiredAt = command.expiredAt,
            thumbnailUrl = command.thumbnailUrl
        )
        return contestRepository.save(contest)
    }

    private fun createAndSaveImages(command: CreateContestCommand, contest: Contest): List<ContestImage> {
        val images = command.images.map { imageCommand ->
            ContestImage.create(
                url = imageCommand.url,
                orderIndex = imageCommand.orderIndex,
                contestId = contest.id
            )
        }
        return contestImageRepository.saveAll(images)
    }

    private fun createAndSaveTags(normalizedTags: List<String>, contest: Contest): List<ContestTag> {
        val tags = normalizedTags.map { tagTitle ->
            ContestTag.create(title = tagTitle, contestId = contest.id)
        }
        return contestTagRepository.saveAll(tags)
    }

    private fun buildResult(
        contest: Contest,
        images: List<ContestImage>,
        tags: List<ContestTag>
    ): CreateContestResult {
        return CreateContestResult(
            id = contest.id.value,
            title = contest.title,
            description = contest.description,
            startedAt = contest.startedAt,
            expiredAt = contest.expiredAt,
            thumbnailUrl = contest.thumbnailUrl,
            images = images.sortedBy { it.orderIndex }.map {
                ContestImageResult(url = it.url, orderIndex = it.orderIndex)
            },
            tags = tags.map { it.title },
            createdAt = contest.createdAt
        )
    }
}
