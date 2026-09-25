package com.example.mykku.event.application.usecase

import com.example.mykku.event.application.dto.EventWinnerEntryResult
import com.example.mykku.event.application.dto.SetEventWinnersCommand
import com.example.mykku.event.application.dto.SetEventWinnersResult
import com.example.mykku.event.application.port.input.SetEventWinnersUseCase
import com.example.mykku.event.application.port.output.EventParticipationRepository
import com.example.mykku.event.application.port.output.EventRepository
import com.example.mykku.event.application.port.output.EventWinnerRepository
import com.example.mykku.event.domain.entity.Event
import com.example.mykku.event.domain.entity.EventParticipation
import com.example.mykku.event.domain.entity.EventWinner
import com.example.mykku.event.domain.vo.EventId
import com.example.mykku.event.domain.vo.EventStatusType
import com.example.mykku.event.domain.vo.EventWinnerEntryStatus
import com.example.mykku.event.exception.EventException
import com.example.mykku.event.exception.InvalidWinnerMemberIdsException
import com.example.mykku.member.application.port.output.MemberRepository
import com.example.mykku.member.domain.entity.Member
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SetEventWinnersUseCaseImpl(
    private val eventRepository: EventRepository,
    private val eventParticipationRepository: EventParticipationRepository,
    private val eventWinnerRepository: EventWinnerRepository,
    private val memberRepository: MemberRepository
) : SetEventWinnersUseCase {

    @Transactional
    override fun execute(command: SetEventWinnersCommand): SetEventWinnersResult {
        val event = eventRepository.findById(EventId.of(command.eventId))
            ?: throw EventException.eventNotFound()
        validateEventExpired(event)
        val entries = resolveEntries(normalizeInputs(command.memberIds))
        val plan = planChanges(event.id, entries)
        if (!command.dryRun) {
            validateEntries(entries)
            applyChanges(event, plan)
        }
        return buildResult(event, command.dryRun, entries, plan)
    }

    private fun validateEventExpired(event: Event) {
        if (event.expiredAt.isAfter(LocalDateTime.now())) {
            throw EventException.eventNotExpired()
        }
    }

    private fun normalizeInputs(memberIds: List<String>): List<String> {
        val inputs = memberIds.map { it.trim() }.filter { it.isNotEmpty() }
        if (inputs.isEmpty()) {
            throw EventException.emptyWinners()
        }
        return inputs
    }

    private fun resolveEntries(inputs: List<String>): List<WinnerEntry> {
        val membersByKey = memberRepository.findByMemberIds(inputs.distinctBy { it.lowercase() })
            .filter { it.memberId != null }
            .associateBy { it.memberId!!.lowercase() }
        val selectedPks = mutableSetOf<Long>()
        return inputs.map { input ->
            val member = membersByKey[input.lowercase()]
            WinnerEntry(input, member, resolveStatus(member, selectedPks))
        }
    }

    private fun resolveStatus(member: Member?, selectedPks: MutableSet<Long>): EventWinnerEntryStatus {
        return when {
            member == null -> EventWinnerEntryStatus.NOT_FOUND
            !member.isProfileComplete -> EventWinnerEntryStatus.PROFILE_INCOMPLETE
            !selectedPks.add(member.id.value) -> EventWinnerEntryStatus.DUPLICATE
            else -> EventWinnerEntryStatus.OK
        }
    }

    private fun validateEntries(entries: List<WinnerEntry>) {
        val invalidInputs = entries.filter { it.status in INVALID_STATUSES }.map { it.input }.distinct()
        if (invalidInputs.isNotEmpty()) {
            throw InvalidWinnerMemberIdsException(invalidInputs)
        }
        if (entries.any { it.status == EventWinnerEntryStatus.DUPLICATE }) {
            throw EventException.duplicateWinner()
        }
    }

    private fun planChanges(eventId: EventId, entries: List<WinnerEntry>): WinnerChangePlan {
        val selectedPks = entries.filter { it.status == EventWinnerEntryStatus.OK }.map { it.member!!.id.value }
        val selectedPkSet = selectedPks.toSet()
        val participations = eventParticipationRepository.findAllByEventId(eventId)
        val winnerParticipationIds = eventWinnerRepository.findByEventId(eventId).map { it.participationId }.toSet()
        val participationsByMember = participations.filter { it.memberId != null }.associateBy { it.memberId!! }
        val selectedParticipations = selectedPks.mapNotNull { participationsByMember[it] }
        return WinnerChangePlan(
            removedParticipations = participationsByMember.filterKeys { it !in selectedPkSet }.values.toList(),
            membersWithoutParticipation = selectedPks.filter { it !in participationsByMember },
            participationsWithoutWinner = selectedParticipations.filter { it.id !in winnerParticipationIds },
            keptCount = selectedParticipations.count { it.id in winnerParticipationIds },
            withdrawnKeptCount = participations.count { it.memberId == null && it.id in winnerParticipationIds }
        )
    }

    private fun applyChanges(event: Event, plan: WinnerChangePlan) {
        val removedIds = plan.removedParticipations.map { it.id }
        eventWinnerRepository.deleteAllByParticipationIds(removedIds)
        eventParticipationRepository.deleteAllByIdIn(removedIds)
        val createdParticipations = plan.membersWithoutParticipation.map {
            eventParticipationRepository.save(EventParticipation.create(eventId = event.id, memberId = it))
        }
        val newWinners = (plan.participationsWithoutWinner + createdParticipations).map {
            EventWinner.create(eventId = event.id, participationId = it.id)
        }
        eventWinnerRepository.saveAll(newWinners)
        event.updateStatus(EventStatusType.WINNER_SELECTED)
        eventRepository.save(event)
    }

    private fun buildResult(
        event: Event,
        dryRun: Boolean,
        entries: List<WinnerEntry>,
        plan: WinnerChangePlan
    ): SetEventWinnersResult {
        return SetEventWinnersResult(
            eventId = event.id.value,
            eventTitle = event.title,
            dryRun = dryRun,
            entries = entries.map { toEntryResult(it) },
            addedCount = plan.membersWithoutParticipation.size + plan.participationsWithoutWinner.size,
            keptCount = plan.keptCount,
            removedCount = plan.removedParticipations.size,
            withdrawnKeptCount = plan.withdrawnKeptCount
        )
    }

    private fun toEntryResult(entry: WinnerEntry): EventWinnerEntryResult {
        return EventWinnerEntryResult(
            input = entry.input,
            memberId = entry.member?.memberId,
            nickname = entry.member?.nickname,
            result = entry.status
        )
    }

    companion object {
        private val INVALID_STATUSES = setOf(
            EventWinnerEntryStatus.NOT_FOUND,
            EventWinnerEntryStatus.PROFILE_INCOMPLETE
        )
    }
}

private data class WinnerEntry(
    val input: String,
    val member: Member?,
    val status: EventWinnerEntryStatus
)

private data class WinnerChangePlan(
    val removedParticipations: List<EventParticipation>,
    val membersWithoutParticipation: List<Long>,
    val participationsWithoutWinner: List<EventParticipation>,
    val keptCount: Int,
    val withdrawnKeptCount: Int
)
