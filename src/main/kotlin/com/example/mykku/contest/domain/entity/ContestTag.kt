package com.example.mykku.contest.domain.entity

import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.domain.vo.ContestTagId
import com.example.mykku.contest.exception.ContestException
import java.time.LocalDateTime

class ContestTag private constructor(
    val id: ContestTagId,
    val title: String,
    val contestId: ContestId,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        const val TITLE_MAX_LENGTH = 20
        val VALID_PATTERN = Regex("^[가-힣a-zA-Z0-9]+$")

        fun create(
            title: String,
            contestId: ContestId
        ): ContestTag {
            validateTitle(title)
            val now = LocalDateTime.now()
            return ContestTag(
                id = ContestTagId(0),
                title = title,
                contestId = contestId,
                createdAt = now,
                updatedAt = now
            )
        }

        fun reconstitute(
            id: ContestTagId,
            title: String,
            contestId: ContestId,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime
        ): ContestTag {
            return ContestTag(
                id = id,
                title = title,
                contestId = contestId,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }

        fun normalizeAndValidate(titles: List<String>): List<String> {
            val normalized = titles.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            if (normalized.isEmpty()) {
                throw ContestException.contestTagRequired()
            }
            if (normalized.size > Contest.TAG_MAX_COUNT) {
                throw ContestException.contestTagLimitExceeded()
            }
            normalized.forEach { validateTitle(it) }
            return normalized
        }

        private fun validateTitle(title: String) {
            if (title.length > TITLE_MAX_LENGTH) {
                throw ContestException.tagTitleTooLong()
            }
            if (!VALID_PATTERN.matches(title)) {
                throw ContestException.tagInvalidFormat()
            }
        }
    }
}
