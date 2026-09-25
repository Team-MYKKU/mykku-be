package com.example.mykku.event.adapter.output.persistence.entity

import com.example.mykku.common.adapter.persistence.BaseJpaEntity
import com.example.mykku.event.domain.entity.Event
import com.example.mykku.event.domain.vo.EventId
import com.example.mykku.event.domain.vo.EventStatusType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "event")
class EventJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "title")
    var title: String,

    @Column(name = "sub_title")
    var subTitle: String? = null,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "started_at")
    var startedAt: LocalDateTime,

    @Column(name = "expired_at")
    var expiredAt: LocalDateTime,

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    var status: EventStatusType = EventStatusType.ACTIVE,

    @Column(name = "thumbnail_url", nullable = false)
    var thumbnailUrl: String
) : BaseJpaEntity() {

    fun toDomain(): Event {
        return Event.reconstitute(
            id = EventId.of(id!!),
            title = title,
            subTitle = subTitle,
            description = description,
            startedAt = startedAt,
            expiredAt = expiredAt,
            thumbnailUrl = thumbnailUrl,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    fun updateFromDomain(event: Event) {
        this.title = event.title
        this.subTitle = event.subTitle
        this.description = event.description
        this.startedAt = event.startedAt
        this.expiredAt = event.expiredAt
        this.status = event.status
        this.thumbnailUrl = event.thumbnailUrl
    }

    companion object {
        const val IMAGE_MAX_COUNT = 10

        fun fromDomain(event: Event): EventJpaEntity {
            return EventJpaEntity(
                id = if (event.id.value == 0L) null else event.id.value,
                title = event.title,
                subTitle = event.subTitle,
                description = event.description,
                startedAt = event.startedAt,
                expiredAt = event.expiredAt,
                status = event.status,
                thumbnailUrl = event.thumbnailUrl
            )
        }
    }
}
