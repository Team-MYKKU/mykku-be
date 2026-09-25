package com.example.mykku.event.application.port.output

import com.example.mykku.event.domain.entity.EventImage
import com.example.mykku.event.domain.vo.EventId

interface EventImageRepository {
    fun save(eventImage: EventImage): EventImage
    fun saveAll(eventImages: List<EventImage>): List<EventImage>
    fun findByEventIds(eventIds: List<EventId>): List<EventImage>
    fun deleteAllByEventId(eventId: EventId)
}
