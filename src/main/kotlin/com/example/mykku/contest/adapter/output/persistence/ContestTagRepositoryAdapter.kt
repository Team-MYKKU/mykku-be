package com.example.mykku.contest.adapter.output.persistence

import com.example.mykku.contest.adapter.output.persistence.entity.ContestTagJpaEntity
import com.example.mykku.contest.adapter.output.persistence.repository.ContestJpaRepository
import com.example.mykku.contest.adapter.output.persistence.repository.ContestTagJpaRepository
import com.example.mykku.contest.application.port.output.ContestTagRepository
import com.example.mykku.contest.domain.entity.ContestTag
import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.exception.ContestException
import org.springframework.stereotype.Repository

@Repository
class ContestTagRepositoryAdapter(
    private val contestTagJpaRepository: ContestTagJpaRepository,
    private val contestJpaRepository: ContestJpaRepository
) : ContestTagRepository {

    override fun save(contestTag: ContestTag): ContestTag {
        val contestJpaEntity = contestJpaRepository.findById(contestTag.contestId.value)
            .orElseThrow { ContestException.contestNotFound() }
        val jpaEntity = ContestTagJpaEntity.fromDomain(contestTag, contestJpaEntity)
        return contestTagJpaRepository.save(jpaEntity).toDomain()
    }

    override fun saveAll(contestTags: List<ContestTag>): List<ContestTag> {
        if (contestTags.isEmpty()) return emptyList()

        val contestIds = contestTags.map { it.contestId.value }.distinct()
        val contestJpaEntities = contestJpaRepository.findAllById(contestIds)
        val contestMap = contestJpaEntities.associateBy { it.id }

        val jpaEntities = contestTags.map { tag ->
            val contestJpaEntity = contestMap[tag.contestId.value]
                ?: throw ContestException.contestNotFound()
            ContestTagJpaEntity.fromDomain(tag, contestJpaEntity)
        }

        return contestTagJpaRepository.saveAll(jpaEntities).map { it.toDomain() }
    }

    override fun findByContestIds(contestIds: List<ContestId>): List<ContestTag> {
        if (contestIds.isEmpty()) return emptyList()

        val contestJpaEntities = contestJpaRepository.findAllById(contestIds.map { it.value })
        if (contestJpaEntities.isEmpty()) return emptyList()

        return contestTagJpaRepository.findByContestIn(contestJpaEntities)
            .map { it.toDomain() }
    }

    override fun findAllByTitleIn(titles: Collection<String>): List<ContestTag> {
        if (titles.isEmpty()) return emptyList()

        return contestTagJpaRepository.findAllByTitleIn(titles)
            .map { it.toDomain() }
    }

    override fun deleteAllByContestId(contestId: ContestId) {
        val contestJpaEntity = contestJpaRepository.findById(contestId.value).orElse(null) ?: return
        contestTagJpaRepository.deleteAllByContest(contestJpaEntity)
        contestTagJpaRepository.flush()
    }
}
