package com.example.mykku.contest.application.port.output

import com.example.mykku.contest.domain.entity.ContestImage
import com.example.mykku.contest.domain.vo.ContestId

interface ContestImageRepository {
    fun save(contestImage: ContestImage): ContestImage
    fun saveAll(contestImages: List<ContestImage>): List<ContestImage>
    fun findByContestIds(contestIds: List<ContestId>): List<ContestImage>
    fun deleteAllByContestId(contestId: ContestId)
}
