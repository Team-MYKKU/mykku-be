package com.example.mykku.event.adapter.output.persistence

import com.example.mykku.event.adapter.output.persistence.entity.EventImageJpaEntity
import com.example.mykku.event.adapter.output.persistence.repository.EventImageJpaRepository
import com.example.mykku.event.adapter.output.persistence.repository.EventJpaRepository
import com.example.mykku.event.application.port.output.EventImageRepository
import com.example.mykku.event.domain.entity.EventImage
import com.example.mykku.event.domain.vo.EventId
import com.example.mykku.event.exception.EventException
import org.springframework.stereotype.Repository

@Repository
class EventImageRepositoryAdapter(
    private val eventImageJpaRepository: EventImageJpaRepository,
    private val eventJpaRepository: EventJpaRepository
) : EventImageRepository {

    override fun save(eventImage: EventImage): EventImage {
        val eventJpaEntity = eventJpaRepository.findById(eventImage.eventId.value)
            .orElseThrow { EventException.eventNotFound() }
        val entity = EventImageJpaEntity.fromDomain(eventImage, eventJpaEntity)
        return eventImageJpaRepository.save(entity).toDomain()
    }

    override fun saveAll(eventImages: List<EventImage>): List<EventImage> {
        if (eventImages.isEmpty()) {
            return emptyList()
        }

        val eventIds = eventImages.map { it.eventId.value }.distinct()
        val eventEntities = eventJpaRepository.findAllById(eventIds).associateBy { it.id!! }

        val entities = eventImages.map { eventImage ->
            val eventJpaEntity = eventEntities[eventImage.eventId.value]
                ?: throw EventException.eventNotFound()
            EventImageJpaEntity.fromDomain(eventImage, eventJpaEntity)
        }

        return eventImageJpaRepository.saveAll(entities).map { it.toDomain() }
    }

    override fun findByEventIds(eventIds: List<EventId>): List<EventImage> {
        if (eventIds.isEmpty()) {
            return emptyList()
        }

        val eventEntities = eventJpaRepository.findAllById(eventIds.map { it.value })
        return eventImageJpaRepository.findByEventIn(eventEntities).map { it.toDomain() }
    }

    override fun deleteAllByEventId(eventId: EventId) {
        val eventJpaEntity = eventJpaRepository.findById(eventId.value).orElse(null) ?: return
        eventImageJpaRepository.deleteAllByEvent(eventJpaEntity)
        eventImageJpaRepository.flush()
    }
}
