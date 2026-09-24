package com.example.mykku.contest.domain.vo

enum class ContestStatusFilter(val listFilter: ContestListFilter) {
    ACTIVE(ContestListFilter.ACTIVE),
    EXPIRED(ContestListFilter.EXPIRED),
    ALL(ContestListFilter.ALL)
}
