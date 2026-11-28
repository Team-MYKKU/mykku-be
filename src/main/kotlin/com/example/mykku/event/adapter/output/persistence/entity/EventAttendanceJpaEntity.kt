package com.example.mykku.event.adapter.output.persistence.entity

import com.example.mykku.common.adapter.persistence.BaseJpaEntity
import com.example.mykku.event.domain.entity.EventAttendance
import com.example.mykku.event.domain.vo.EventAttendanceId
import com.example.mykku.event.domain.vo.EventId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "event_attendance")
class EventAttendanceJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "event_id", nullable = false)
    val eventId: Long,

    @Column(name = "member_id")
    val memberId: Long?,

    @Column(name = "attendance_date", nullable = false)
    val attendanceDate: LocalDate
) : BaseJpaEntity() {

    fun toDomain(): EventAttendance {
        return EventAttendance.reconstitute(
            id = EventAttendanceId.of(id!!),
            eventId = EventId.of(eventId),
            memberId = memberId,
            attendanceDate = attendanceDate
        )
    }

    companion object {
        fun fromDomain(attendance: EventAttendance): EventAttendanceJpaEntity {
            return EventAttendanceJpaEntity(
                id = if (attendance.id.value == 0L) null else attendance.id.value,
                eventId = attendance.eventId.value,
                memberId = attendance.memberId,
                attendanceDate = attendance.attendanceDate
            )
        }
    }
}
