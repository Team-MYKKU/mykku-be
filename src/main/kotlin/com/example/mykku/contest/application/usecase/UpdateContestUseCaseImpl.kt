package com.example.mykku.contest.application.usecase

import com.example.mykku.contest.application.dto.UpdateContestCommand
import com.example.mykku.contest.application.port.input.UpdateContestUseCase
import com.example.mykku.contest.application.port.output.ContestImageRepository
import com.example.mykku.contest.application.port.output.ContestRepository
import com.example.mykku.contest.application.port.output.ContestTagRepository
import com.example.mykku.contest.domain.entity.Contest
import com.example.mykku.contest.domain.entity.ContestImage
import com.example.mykku.contest.domain.entity.ContestTag
import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.exception.ContestException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateContestUseCaseImpl(
    private val contestRepository: ContestRepository,
    private val contestImageRepository: ContestImageRepository,
    private val contestTagRepository: ContestTagRepository
) : UpdateContestUseCase {

    @Transactional
    override fun execute(command: UpdateContestCommand) {
        val contest = contestRepository.findById(ContestId.of(command.contestId))
            ?: throw ContestException.contestNotFound()
        val tags = ContestTag.normalizeAndValidate(command.tags)
        val currentTags = contestTagRepository.findByContestIds(listOf(contest.id)).map { it.title }
        val existingUrls = contestImageRepository.findByContestIds(listOf(contest.id)).map { it.url }
        validateImages(existingUrls, command)
        val previousThumbnailUrl = contest.thumbnailUrl
        contest.update(command.content, command.thumbnailUrl, tagsChanged = currentTags.toSet() != tags.toSet())
        contestRepository.save(contest)
        replaceImages(contest, command.keepImageUrls + command.newImageUrls)
        replaceTags(contest, currentTags, tags)
        logReplacedImages(contest, previousThumbnailUrl, existingUrls - command.keepImageUrls.toSet())
    }

    private fun validateImages(existingUrls: List<String>, command: UpdateContestCommand) {
        val keep = command.keepImageUrls
        if (keep.size != keep.toSet().size || !existingUrls.containsAll(keep)) {
            throw ContestException.invalidKeepImageUrls()
        }
        if (keep.size + command.newImageUrls.size > Contest.IMAGE_MAX_COUNT) {
            throw ContestException.contestImageLimitExceeded()
        }
    }

    private fun replaceImages(contest: Contest, urls: List<String>) {
        contestImageRepository.deleteAllByContestId(contest.id)
        val images = urls.mapIndexed { index, url ->
            ContestImage.create(url = url, orderIndex = index, contestId = contest.id)
        }
        contestImageRepository.saveAll(images)
    }

    private fun replaceTags(contest: Contest, currentTags: List<String>, tags: List<String>) {
        if (currentTags == tags) return
        contestTagRepository.deleteAllByContestId(contest.id)
        contestTagRepository.saveAll(tags.map { ContestTag.create(title = it, contestId = contest.id) })
    }

    private fun logReplacedImages(contest: Contest, previousThumbnailUrl: String, removedUrls: List<String>) {
        val replacedThumbnail = previousThumbnailUrl.takeIf { it != contest.thumbnailUrl }
        if (replacedThumbnail == null && removedUrls.isEmpty()) return
        log.info(
            "contest images replaced id={}, previousThumbnail={}, removedImages={}",
            contest.id.value,
            replacedThumbnail,
            removedUrls
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(UpdateContestUseCaseImpl::class.java)
    }
}
