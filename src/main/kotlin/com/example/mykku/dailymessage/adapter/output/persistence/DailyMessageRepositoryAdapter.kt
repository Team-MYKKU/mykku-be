package com.example.mykku.dailymessage.adapter.output.persistence

import com.example.mykku.dailymessage.adapter.output.persistence.entity.DailyMessageJpaEntity
import com.example.mykku.dailymessage.adapter.output.persistence.repository.DailyMessageJpaRepository
import com.example.mykku.dailymessage.application.port.output.DailyMessageRepository
import com.example.mykku.dailymessage.domain.entity.DailyMessage
import com.example.mykku.dailymessage.domain.vo.DailyMessageId
import com.example.mykku.dailymessage.domain.vo.SortDirection
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class DailyMessageRepositoryAdapter(
    private val dailyMessageJpaRepository: DailyMessageJpaRepository
) : DailyMessageRepository {

    override fun save(dailyMessage: DailyMessage): DailyMessage {
        val jpaEntity = if (dailyMessage.id.value == 0L) {
            DailyMessageJpaEntity.fromDomain(dailyMessage)
        } else {
            dailyMessageJpaRepository.findById(dailyMessage.id.value)
                .map { entity ->
                    entity.updateFromDomain(dailyMessage)
                    entity
                }
                .orElseGet { DailyMessageJpaEntity.fromDomain(dailyMessage) }
        }
        return dailyMessageJpaRepository.save(jpaEntity).toDomain()
    }

    override fun findById(id: DailyMessageId): DailyMessage? {
        return dailyMessageJpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findByDate(date: LocalDate): DailyMessage? {
        return dailyMessageJpaRepository.findByDate(date)?.toDomain()
    }

    override fun findByDateBeforeOrEqual(date: LocalDate, pageable: Pageable): Page<DailyMessage> {
        return dailyMessageJpaRepository.findByDateBeforeOrEqual(date, pageable)
            .map { it.toDomain() }
    }

    override fun findByDateBeforeOrEqualWithSort(date: LocalDate, limit: Int, sort: SortDirection): List<DailyMessage> {
        val pageable = PageRequest.of(0, limit)
        return when (sort) {
            SortDirection.ASC -> dailyMessageJpaRepository.findByDateBeforeOrEqualOrderByDateAsc(date, pageable)
            SortDirection.DESC -> dailyMessageJpaRepository.findByDateBeforeOrEqualOrderByDateDesc(date, pageable)
        }.map { it.toDomain() }
    }

    override fun findAll(pageable: Pageable): Page<DailyMessage> {
        return dailyMessageJpaRepository.findAll(pageable)
            .map { it.toDomain() }
    }

    override fun deleteById(id: DailyMessageId) {
        dailyMessageJpaRepository.deleteById(id.value)
    }

    override fun findAllByDate(date: LocalDate): List<DailyMessage> {
        return dailyMessageJpaRepository.findAllByDate(date).map { it.toDomain() }
    }
}
