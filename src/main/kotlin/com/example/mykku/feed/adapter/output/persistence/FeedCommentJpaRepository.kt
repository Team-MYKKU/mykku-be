package com.example.mykku.feed.adapter.output.persistence

import com.example.mykku.common.adapter.persistence.IdCountRow
import com.example.mykku.feed.adapter.output.persistence.entity.FeedCommentJpaEntity
import com.example.mykku.feed.adapter.output.persistence.entity.FeedJpaEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface FeedCommentJpaRepository : JpaRepository<FeedCommentJpaEntity, Long> {
    @Query(
        value = "SELECT fc FROM FeedCommentJpaEntity fc JOIN FETCH fc.member WHERE fc.feed = :feed AND fc.parentComment IS NULL ORDER BY fc.createdAt DESC",
        countQuery = "SELECT COUNT(fc) FROM FeedCommentJpaEntity fc WHERE fc.feed = :feed AND fc.parentComment IS NULL"
    )
    fun findByFeedAndParentCommentIsNull(@Param("feed") feed: FeedJpaEntity, pageable: Pageable): Page<FeedCommentJpaEntity>

    @Query("""
        SELECT fc FROM FeedCommentJpaEntity fc
        JOIN FETCH fc.member
        WHERE fc.feed.id IN :feedIds
        AND fc.parentComment IS NULL
        AND fc.id = (
            SELECT MIN(fc2.id) FROM FeedCommentJpaEntity fc2
            WHERE fc2.feed.id = fc.feed.id AND fc2.parentComment IS NULL
        )
    """)
    fun findFirstCommentsByFeedIds(@Param("feedIds") feedIds: List<Long>): List<FeedCommentJpaEntity>

    @Query("SELECT fc FROM FeedCommentJpaEntity fc JOIN FETCH fc.member WHERE fc.parentComment = :parentComment ORDER BY fc.createdAt ASC")
    fun findByParentComment(@Param("parentComment") parentComment: FeedCommentJpaEntity): List<FeedCommentJpaEntity>

    @Query("SELECT fc FROM FeedCommentJpaEntity fc JOIN FETCH fc.member WHERE fc.parentComment IN :parentComments ORDER BY fc.parentComment.id, fc.createdAt ASC")
    fun findByParentCommentIn(@Param("parentComments") parentComments: List<FeedCommentJpaEntity>): List<FeedCommentJpaEntity>

    fun countByFeed(feed: FeedJpaEntity): Long

    fun countByFeedId(feedId: Long): Long

    @Query("""
        SELECT fc.feed.id AS entityId, COUNT(fc) AS countValue
        FROM FeedCommentJpaEntity fc
        WHERE fc.feed.id IN :feedIds
        GROUP BY fc.feed.id
    """)
    fun countGroupedByFeedIdIn(@Param("feedIds") feedIds: List<Long>): List<IdCountRow>

    fun findAllByFeed(feed: FeedJpaEntity): List<FeedCommentJpaEntity>

    @Query("SELECT fc.id FROM FeedCommentJpaEntity fc WHERE fc.feed = :feed")
    fun findIdsByFeed(@Param("feed") feed: FeedJpaEntity): List<Long>
}
