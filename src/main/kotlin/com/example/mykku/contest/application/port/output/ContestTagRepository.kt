package com.example.mykku.contest.application.port.output

import com.example.mykku.contest.domain.entity.ContestTag
import com.example.mykku.contest.domain.vo.ContestId

interface ContestTagRepository {
    fun save(contestTag: ContestTag): ContestTag
    fun saveAll(contestTags: List<ContestTag>): List<ContestTag>
    fun findByContestIds(contestIds: List<ContestId>): List<ContestTag>
    fun findAllByTitleIn(titles: Collection<String>): List<ContestTag>
    fun deleteAllByContestId(contestId: ContestId)
}
