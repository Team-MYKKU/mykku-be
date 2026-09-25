package com.example.mykku.event.domain.vo

enum class EventStatusFilter(val listFilter: EventListFilter) {
    ACTIVE(EventListFilter.ACTIVE),
    EXPIRED(EventListFilter.EXPIRED),
    ALL(EventListFilter.ALL)
}
