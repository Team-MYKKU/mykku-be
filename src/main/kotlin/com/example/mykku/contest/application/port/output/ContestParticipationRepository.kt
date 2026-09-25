package com.example.mykku.contest.application.port.output

import com.example.mykku.contest.domain.entity.Contest
import com.example.mykku.contest.domain.entity.ContestParticipation
import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.domain.vo.ContestParticipationId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ContestParticipationRepository {
    fun save(participation: ContestParticipation): ContestParticipation
    fun findById(id: ContestParticipationId): ContestParticipation?
    fun findAllByIdIn(ids: List<ContestParticipationId>): List<ContestParticipation>
    fun findByContestId(contestId: ContestId, pageable: Pageable): Page<ContestParticipation>
    fun findByFeedId(feedId: Long): List<ContestParticipation>
    fun findContestsByMemberId(memberId: Long, pageable: Pageable): Page<Contest>
    fun findByMemberIdAndContestIds(memberId: Long, contestIds: List<ContestId>): List<ContestParticipation>
    fun existsByMemberIdAndContestId(memberId: Long, contestId: ContestId): Boolean
    fun existsByMemberIdAndContestIdAndFeedId(memberId: Long, contestId: ContestId, feedId: Long): Boolean
    fun countByContestId(contestId: ContestId): Long
    fun deleteAll(participations: List<ContestParticipation>)
    fun countByContestIds(contestIds: List<ContestId>): Map<Long, Int>
}
