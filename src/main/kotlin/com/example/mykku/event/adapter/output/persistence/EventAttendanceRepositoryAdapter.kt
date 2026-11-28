package com.example.mykku.event.adapter.output.persistence

import com.example.mykku.common.adapter.persistence.toCountMap
import com.example.mykku.event.adapter.output.persistence.entity.EventAttendanceJpaEntity
import com.example.mykku.event.adapter.output.persistence.repository.EventAttendanceJpaRepository
import com.example.mykku.event.application.port.output.EventAttendanceRepository
import com.example.mykku.event.domain.entity.EventAttendance
import com.example.mykku.event.domain.vo.EventId
import com.example.mykku.event.exception.EventException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class EventAttendanceRepositoryAdapter(
    private val eventAttendanceJpaRepository: EventAttendanceJpaRepository
) : EventAttendanceRepository {

    override fun save(attendance: EventAttendance): EventAttendance {
        val entity = EventAttendanceJpaEntity.fromDomain(attendance)
        try {
            return eventAttendanceJpaRepository.save(entity).toDomain()
        } catch (exception: DataIntegrityViolationException) {
            if (!EventAttendanceUniqueKey.isViolatedBy(exception)) throw exception
            throw EventException.alreadyAttendedToday(exception)
        }
    }

    override fun existsByEventIdAndMemberIdAndDate(eventId: EventId, memberId: Long, date: LocalDate): Boolean {
        return eventAttendanceJpaRepository.existsByEventIdAndMemberIdAndAttendanceDate(eventId.value, memberId, date)
    }

    override fun findDates(eventId: EventId, memberId: Long, from: LocalDate, to: LocalDate): List<LocalDate> {
        return eventAttendanceJpaRepository.findAttendanceDates(eventId.value, memberId, from, to)
    }

    override fun countByEventIds(eventIds: List<EventId>): Map<Long, Int> {
        if (eventIds.isEmpty()) return emptyMap()
        return eventAttendanceJpaRepository.countGroupedByEventIdIn(eventIds.map { it.value }).toCountMap()
    }
}
