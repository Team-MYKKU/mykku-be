package com.example.mykku.event.adapter.output.persistence.repository

import com.example.mykku.event.domain.vo.EventType

interface EventTypeRow {
    fun getId(): Long
    fun getType(): EventType
}
