package com.example.mykku.board.adapter.output.persistence

import com.example.mykku.board.adapter.output.persistence.entity.BoardJpaEntity
import com.example.mykku.board.application.port.output.BoardRepository
import com.example.mykku.board.domain.entity.Board
import com.example.mykku.board.domain.vo.BoardId
import org.springframework.stereotype.Repository

@Repository
class BoardRepositoryAdapter(
    private val boardJpaRepository: BoardJpaRepository
) : BoardRepository {

    override fun save(board: Board): Board {
        val entity = if (board.id.value == 0L) {
            BoardJpaEntity.fromDomain(board)
        } else {
            boardJpaRepository.findById(board.id.value)
                .map { it.apply { updateFromDomain(board) } }
                .orElseGet { BoardJpaEntity.fromDomain(board) }
        }
        return boardJpaRepository.save(entity).toDomain()
    }

    override fun findById(id: BoardId): Board? {
        return boardJpaRepository.findById(id.value).orElse(null)?.toDomain()
    }

    override fun findByIds(ids: List<BoardId>): List<Board> {
        if (ids.isEmpty()) return emptyList()
        return boardJpaRepository.findAllById(ids.map { it.value }).map { it.toDomain() }
    }

    override fun findAll(): List<Board> {
        return boardJpaRepository.findAll().map { it.toDomain() }
    }

    override fun delete(board: Board) {
        boardJpaRepository.deleteById(board.id.value)
    }

    override fun existsById(id: BoardId): Boolean {
        return boardJpaRepository.existsById(id.value)
    }
}
