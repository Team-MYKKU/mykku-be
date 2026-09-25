package com.example.mykku.dailymessage.application.port.output

import com.example.mykku.dailymessage.domain.entity.DailyMessage
import com.example.mykku.dailymessage.domain.vo.DailyMessageId
import com.example.mykku.dailymessage.domain.vo.SortDirection
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface DailyMessageRepository {
    fun save(dailyMessage: DailyMessage): DailyMessage
    fun findById(id: DailyMessageId): DailyMessage?
    fun findByDate(date: LocalDate): DailyMessage?
    fun findByDateBeforeOrEqual(date: LocalDate, pageable: Pageable): Page<DailyMessage>
    fun findByDateBeforeOrEqualWithSort(date: LocalDate, limit: Int, sort: SortDirection): List<DailyMessage>
    fun findAll(pageable: Pageable): Page<DailyMessage>
    fun deleteById(id: DailyMessageId)
    fun findAllByDate(date: LocalDate): List<DailyMessage>
}
