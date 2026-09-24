package com.example.mykku.feed.domain.vo

import com.example.mykku.feed.exception.FeedException

@JvmInline
value class SearchKeyword private constructor(val value: String) {
    companion object {
        const val MAX_LENGTH = 50

        fun of(raw: String): SearchKeyword {
            val normalized = raw.trim().lowercase()
            if (normalized.isEmpty()) throw FeedException.searchKeywordEmpty()
            if (normalized.length > MAX_LENGTH) throw FeedException.searchKeywordTooLong()
            return SearchKeyword(normalized)
        }
    }
}
