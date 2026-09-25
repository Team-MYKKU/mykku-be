package com.example.mykku.contest.application.usecase

import com.example.mykku.contest.application.port.input.DeleteContestUseCase
import com.example.mykku.contest.application.port.output.ContestImageRepository
import com.example.mykku.contest.application.port.output.ContestParticipationRepository
import com.example.mykku.contest.application.port.output.ContestRepository
import com.example.mykku.contest.application.port.output.ContestWinnerRepository
import com.example.mykku.contest.domain.entity.Contest
import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.exception.ContestException
import com.example.mykku.image.ImageCleanupPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteContestUseCaseImpl(
    private val contestRepository: ContestRepository,
    private val contestImageRepository: ContestImageRepository,
    private val contestParticipationRepository: ContestParticipationRepository,
    private val contestWinnerRepository: ContestWinnerRepository,
    private val imageCleanupPort: ImageCleanupPort
) : DeleteContestUseCase {

    @Transactional
    override fun execute(contestId: Long) {
        val contest = contestRepository.findById(ContestId.of(contestId))
            ?: throw ContestException.contestNotFound()
        val imageUrls = listOf(contest.thumbnailUrl) +
            contestImageRepository.findByContestIds(listOf(contest.id)).map { it.url }
        logDeletion(contest, imageUrls)
        contestRepository.deleteById(contest.id)
        imageCleanupPort.deleteAfterCommit(imageUrls)
    }

    private fun logDeletion(contest: Contest, imageUrls: List<String>) {
        val ids = listOf(contest.id)
        log.info(
            "admin delete contest id={}, title={}, participations={}, winners={}, imageUrls={}",
            contest.id.value,
            contest.title,
            contestParticipationRepository.countByContestIds(ids)[contest.id.value] ?: 0,
            contestWinnerRepository.countByContestIds(ids)[contest.id.value] ?: 0,
            imageUrls
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(DeleteContestUseCaseImpl::class.java)
    }
}
