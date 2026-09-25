package com.example.mykku.contest.adapter.output.persistence

import com.example.mykku.contest.adapter.output.persistence.entity.ContestImageJpaEntity
import com.example.mykku.contest.adapter.output.persistence.repository.ContestImageJpaRepository
import com.example.mykku.contest.adapter.output.persistence.repository.ContestJpaRepository
import com.example.mykku.contest.application.port.output.ContestImageRepository
import com.example.mykku.contest.domain.entity.ContestImage
import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.exception.ContestException
import org.springframework.stereotype.Repository

@Repository
class ContestImageRepositoryAdapter(
    private val contestImageJpaRepository: ContestImageJpaRepository,
    private val contestJpaRepository: ContestJpaRepository
) : ContestImageRepository {

    override fun save(contestImage: ContestImage): ContestImage {
        val contestJpaEntity = contestJpaRepository.findById(contestImage.contestId.value)
            .orElseThrow { ContestException.contestNotFound() }
        val jpaEntity = ContestImageJpaEntity.fromDomain(contestImage, contestJpaEntity)
        return contestImageJpaRepository.save(jpaEntity).toDomain()
    }

    override fun saveAll(contestImages: List<ContestImage>): List<ContestImage> {
        if (contestImages.isEmpty()) return emptyList()

        val contestIds = contestImages.map { it.contestId.value }.distinct()
        val contestJpaEntities = contestJpaRepository.findAllById(contestIds)
        val contestMap = contestJpaEntities.associateBy { it.id }

        val jpaEntities = contestImages.map { image ->
            val contestJpaEntity = contestMap[image.contestId.value]
                ?: throw ContestException.contestNotFound()
            ContestImageJpaEntity.fromDomain(image, contestJpaEntity)
        }

        return contestImageJpaRepository.saveAll(jpaEntities).map { it.toDomain() }
    }

    override fun findByContestIds(contestIds: List<ContestId>): List<ContestImage> {
        if (contestIds.isEmpty()) return emptyList()

        val contestJpaEntities = contestJpaRepository.findAllById(contestIds.map { it.value })
        if (contestJpaEntities.isEmpty()) return emptyList()

        return contestImageJpaRepository.findByContestIn(contestJpaEntities)
            .map { it.toDomain() }
    }

    override fun deleteAllByContestId(contestId: ContestId) {
        val contestJpaEntity = contestJpaRepository.findById(contestId.value).orElse(null) ?: return
        contestImageJpaRepository.deleteAllByContest(contestJpaEntity)
        contestImageJpaRepository.flush()
    }
}
