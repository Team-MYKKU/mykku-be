package com.example.mykku.event.application.usecase

import com.example.mykku.common.util.KstTime
import com.example.mykku.event.application.dto.AttendEventCommand
import com.example.mykku.event.application.dto.AttendanceStatusResult
import com.example.mykku.event.application.dto.MyEventStatusResult
import com.example.mykku.event.application.port.input.AttendEventUseCase
import com.example.mykku.event.application.port.output.EventAttendanceRepository
import com.example.mykku.event.application.port.output.EventRepository
import com.example.mykku.event.domain.entity.Event
import com.example.mykku.event.domain.entity.EventAttendance
import com.example.mykku.event.domain.vo.EventId
import com.example.mykku.event.domain.vo.EventType
import com.example.mykku.event.exception.EventException
import com.example.mykku.member.domain.entity.Member
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class AttendEventUseCaseImpl(
    private val clock: Clock,
    private val eventRepository: EventRepository,
    private val eventAttendanceRepository: EventAttendanceRepository
) : AttendEventUseCase {

    @Transactional
    override fun execute(command: AttendEventCommand, member: Member): MyEventStatusResult {
        val now = KstTime.now(clock)
        val event = findAttendableEvent(command.eventId, now)
        member.requireProfileCompleted()
        attendToday(event, member.id.value, now.toLocalDate())
        val attendedDates = findAttendedDates(event, member.id.value)
        return MyEventStatusResult.of(event, now, attendance = AttendanceStatusResult.of(event, now, attendedDates))
    }

    private fun findAttendableEvent(eventId: Long, now: LocalDateTime): Event {
        val event = eventRepository.findById(EventId.of(eventId)) ?: throw EventException.eventNotFound()
        event.requireType(EventType.ATTENDANCE)
        event.requireInProgress(now)
        return event
    }

    private fun attendToday(event: Event, memberPk: Long, today: LocalDate) {
        if (eventAttendanceRepository.existsByEventIdAndMemberIdAndDate(event.id, memberPk, today)) {
            throw EventException.alreadyAttendedToday()
        }
        eventAttendanceRepository.save(EventAttendance.create(event.id, memberPk, today))
    }

    private fun findAttendedDates(event: Event, memberPk: Long): List<LocalDate> {
        val period = event.attendancePeriod()
        val from = period.startDate ?: return emptyList()
        val to = period.endDate ?: return emptyList()
        return eventAttendanceRepository.findDates(event.id, memberPk, from, to)
    }
}
