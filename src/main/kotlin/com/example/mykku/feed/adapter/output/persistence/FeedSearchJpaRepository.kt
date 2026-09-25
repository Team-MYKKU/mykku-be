package com.example.mykku.feed.adapter.output.persistence

import com.example.mykku.feed.adapter.output.persistence.entity.FeedJpaEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param

private const val SEARCH_CONDITION = """
    (LOCATE(:keyword, collate(lower(f.title) as utf8mb4_bin)) > 0
        OR LOCATE(:keyword, collate(lower(f.content) as utf8mb4_bin)) > 0)
    AND NOT EXISTS (
        SELECT 1 FROM MemberBlockJpaEntity mb
        WHERE (mb.blocker.id = :viewerId AND mb.blocked = f.member)
            OR (mb.blocked.id = :viewerId AND mb.blocker = f.member)
    )
    AND NOT EXISTS (
        SELECT 1 FROM KeywordBlockJpaEntity kb
        WHERE kb.member.id = :viewerId
            AND (LOCATE(collate(kb.keyword as utf8mb4_bin), collate(lower(f.title) as utf8mb4_bin)) > 0
                OR LOCATE(collate(kb.keyword as utf8mb4_bin), collate(lower(f.content) as utf8mb4_bin)) > 0)
    )
"""

interface FeedSearchJpaRepository : Repository<FeedJpaEntity, Long> {

    @Query("""
        SELECT f.board.id AS boardId, COUNT(f) AS matchCount, MAX(f.createdAt) AS latestCreatedAt
        FROM FeedJpaEntity f
        WHERE $SEARCH_CONDITION
        GROUP BY f.board.id
    """)
    fun summarizeByBoard(
        @Param("keyword") keyword: String,
        @Param("viewerId") viewerId: Long?
    ): List<BoardMatchRow>

    @Query("""
        SELECT f FROM FeedJpaEntity f
        WHERE f.board.id = :boardId AND $SEARCH_CONDITION
        ORDER BY f.createdAt DESC, f.id DESC
    """)
    fun findTopByBoard(
        @Param("keyword") keyword: String,
        @Param("viewerId") viewerId: Long?,
        @Param("boardId") boardId: Long,
        pageable: Pageable
    ): List<FeedJpaEntity>

    @Query(
        value = """
            SELECT f FROM FeedJpaEntity f
            WHERE f.board.id = :boardId AND $SEARCH_CONDITION
            ORDER BY f.createdAt DESC, f.id DESC
        """,
        countQuery = """
            SELECT COUNT(f) FROM FeedJpaEntity f
            WHERE f.board.id = :boardId AND $SEARCH_CONDITION
        """
    )
    fun findByBoard(
        @Param("keyword") keyword: String,
        @Param("viewerId") viewerId: Long?,
        @Param("boardId") boardId: Long,
        pageable: Pageable
    ): Page<FeedJpaEntity>
}
