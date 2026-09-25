package com.example.mykku.dailymessage.adapter.output.persistence.repository

import com.example.mykku.dailymessage.adapter.output.persistence.entity.DailyMessageJpaEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface DailyMessageJpaRepository : JpaRepository<DailyMessageJpaEntity, Long> {
    fun findByDate(date: LocalDate): DailyMessageJpaEntity?

    @Query("SELECT dm FROM DailyMessageJpaEntity dm WHERE dm.date <= :date ORDER BY dm.date DESC")
    fun findByDateBeforeOrEqualOrderByDateDesc(@Param("date") date: LocalDate, pageable: Pageable): List<DailyMessageJpaEntity>

    @Query("SELECT dm FROM DailyMessageJpaEntity dm WHERE dm.date <= :date ORDER BY dm.date ASC")
    fun findByDateBeforeOrEqualOrderByDateAsc(@Param("date") date: LocalDate, pageable: Pageable): List<DailyMessageJpaEntity>

    @Query(
        value = "SELECT dm FROM DailyMessageJpaEntity dm WHERE dm.date <= :date",
        countQuery = "SELECT COUNT(dm) FROM DailyMessageJpaEntity dm WHERE dm.date <= :date"
    )
    fun findByDateBeforeOrEqual(@Param("date") date: LocalDate, pageable: Pageable): Page<DailyMessageJpaEntity>
    fun findAllByDate(date: LocalDate): List<DailyMessageJpaEntity>
}
