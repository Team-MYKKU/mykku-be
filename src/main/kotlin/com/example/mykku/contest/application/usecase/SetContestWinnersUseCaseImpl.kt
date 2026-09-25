package com.example.mykku.contest.application.usecase

import com.example.mykku.contest.application.dto.SetContestWinnersCommand
import com.example.mykku.contest.application.dto.SetContestWinnersResult
import com.example.mykku.contest.application.dto.WinnerInfoResult
import com.example.mykku.contest.application.dto.WinnerSelectionCommand
import com.example.mykku.contest.application.port.input.SetContestWinnersUseCase
import com.example.mykku.contest.application.port.output.ContestParticipationRepository
import com.example.mykku.contest.application.port.output.ContestRepository
import com.example.mykku.contest.application.port.output.ContestWinnerRepository
import com.example.mykku.contest.domain.entity.Contest
import com.example.mykku.contest.domain.entity.ContestParticipation
import com.example.mykku.contest.domain.entity.ContestWinner
import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.domain.vo.ContestParticipationId
import com.example.mykku.contest.domain.vo.ContestStatusType
import com.example.mykku.contest.exception.ContestException
import com.example.mykku.feed.application.port.output.FeedRepository
import com.example.mykku.feed.domain.vo.FeedId
import com.example.mykku.member.application.port.output.MemberRepository
import com.example.mykku.member.domain.vo.MemberPk
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SetContestWinnersUseCaseImpl(
    private val contestRepository: ContestRepository,
    private val contestParticipationRepository: ContestParticipationRepository,
    private val contestWinnerRepository: ContestWinnerRepository,
    private val feedRepository: FeedRepository,
    private val memberRepository: MemberRepository
) : SetContestWinnersUseCase {

    @Transactional
    override fun execute(command: SetContestWinnersCommand): SetContestWinnersResult {
        val contestId = ContestId.of(command.contestId)
        val contest = contestRepository.findById(contestId)
            ?: throw ContestException.contestNotFound()

        validateContestExpired(contest)
        validateSelections(command.winners)
        val participationsMap = fetchAndValidateParticipations(contestId, command.winners)

        val savedWinners = replaceWinners(contestId, command.winners)
        contest.updateStatus(ContestStatusType.WINNER_SELECTED)
        contestRepository.save(contest)

        return buildResult(contest, savedWinners, participationsMap)
    }

    private fun validateContestExpired(contest: Contest) {
        if (contest.expiredAt.isAfter(LocalDateTime.now())) {
            throw ContestException.contestNotExpired()
        }
    }

    private fun validateSelections(selections: List<WinnerSelectionCommand>) {
        val ranks = selections.map { it.winnerRank }
        if (ranks.any { it !in 1..3 }) {
            throw ContestException.invalidWinnerRank()
        }
        if (ranks.size != ranks.toSet().size) {
            throw ContestException.duplicateWinnerRank()
        }
        val participationIds = selections.map { it.participationId }
        if (participationIds.size != participationIds.toSet().size) {
            throw ContestException.duplicateWinnerParticipation()
        }
    }

    private fun fetchAndValidateParticipations(
        contestId: ContestId,
        selections: List<WinnerSelectionCommand>
    ): Map<Long, ContestParticipation> {
        val participationIds = selections.map { ContestParticipationId.of(it.participationId) }
        val participations = contestParticipationRepository.findAllByIdIn(participationIds)
        if (participations.size != participationIds.size) {
            throw ContestException.participationNotFound()
        }
        if (participations.any { it.contestId != contestId }) {
            throw ContestException.participationNotBelongToContest()
        }
        validateOneAwardPerMember(participations)
        return participations.associateBy { it.id.value }
    }

    private fun validateOneAwardPerMember(participations: List<ContestParticipation>) {
        val memberIds = participations.mapNotNull { it.memberId }
        if (memberIds.size != memberIds.toSet().size) {
            throw ContestException.duplicateWinnerMember()
        }
    }

    private fun replaceWinners(contestId: ContestId, selections: List<WinnerSelectionCommand>): List<ContestWinner> {
        val currentWinners = contestWinnerRepository.findByContestId(contestId)
            .associateBy { it.participationId.value }
        val selectedIds = selections.map { it.participationId }.toSet()
        val removedIds = currentWinners.values
            .filter { it.participationId.value !in selectedIds }
            .map { it.participationId }
        contestWinnerRepository.deleteAllByParticipationIds(removedIds)
        val winners = selections.map { toWinner(contestId, it, currentWinners[it.participationId]) }
        return contestWinnerRepository.saveAll(winners)
    }

    private fun toWinner(
        contestId: ContestId,
        selection: WinnerSelectionCommand,
        currentWinner: ContestWinner?
    ): ContestWinner {
        if (currentWinner != null) {
            return currentWinner.reassign(selection.winnerRank, selection.awardTitle, selection.description)
        }
        return ContestWinner.create(
            winnerRank = selection.winnerRank,
            awardTitle = selection.awardTitle,
            description = selection.description,
            contestId = contestId,
            participationId = ContestParticipationId.of(selection.participationId)
        )
    }

    private fun buildResult(
        contest: Contest,
        winners: List<ContestWinner>,
        participationsMap: Map<Long, ContestParticipation>
    ): SetContestWinnersResult {
        val feedTitles = resolveFeedTitles(participationsMap.values)
        val nicknames = resolveNicknames(participationsMap.values)
        val winnerInfos = winners.map { winner ->
            val participation = participationsMap.getValue(winner.participationId.value)
            WinnerInfoResult(
                winnerId = winner.id.value,
                winnerRank = winner.winnerRank,
                feedId = participation.feedId,
                feedTitle = feedTitles[participation.feedId] ?: "",
                authorNickname = participation.memberId?.let { nicknames[it] } ?: ""
            )
        }
        return SetContestWinnersResult(contest.id.value, contest.title, winnerInfos)
    }

    private fun resolveFeedTitles(participations: Collection<ContestParticipation>): Map<Long, String> {
        return feedRepository.findAllByIds(participations.map { FeedId(it.feedId) })
            .associate { it.id!!.value to it.title }
    }

    private fun resolveNicknames(participations: Collection<ContestParticipation>): Map<Long, String?> {
        return memberRepository.findByIds(participations.mapNotNull { it.memberId }.map { MemberPk.of(it) })
            .associate { it.id.value to it.nickname }
    }
}
