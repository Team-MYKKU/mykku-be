package com.example.mykku.event.adapter.output.persistence.repository

import com.example.mykku.event.adapter.output.persistence.entity.EventImageJpaEntity
import com.example.mykku.event.adapter.output.persistence.entity.EventJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EventImageJpaRepository : JpaRepository<EventImageJpaEntity, Long> {
    fun findByEventIn(events: List<EventJpaEntity>): List<EventImageJpaEntity>
    fun deleteAllByEvent(event: EventJpaEntity)
}
