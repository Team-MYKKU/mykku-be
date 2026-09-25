package com.example.mykku.board.domain.entity

import com.example.mykku.board.domain.vo.BoardId
import java.time.LocalDateTime

class Board private constructor(
    val id: BoardId,
    title: String,
    logo: String,
    val createdAt: LocalDateTime,
    updatedAt: LocalDateTime
) {
    var title: String = title
        private set

    var logo: String = logo
        private set

    var updatedAt: LocalDateTime = updatedAt
        private set

    fun update(newTitle: String, newLogo: String?) {
        title = newTitle
        newLogo?.let { logo = it }
        updatedAt = LocalDateTime.now()
    }

    companion object {
        fun create(
            title: String,
            logo: String
        ): Board {
            val now = LocalDateTime.now()
            return Board(
                id = BoardId(0L),
                title = title,
                logo = logo,
                createdAt = now,
                updatedAt = now
            )
        }

        fun reconstitute(
            id: BoardId,
            title: String,
            logo: String,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime
        ): Board = Board(
            id = id,
            title = title,
            logo = logo,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
