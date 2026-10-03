package com.example.mykku.dailymessage.adapter.output.persistence.entity

import com.example.mykku.common.adapter.persistence.BaseJpaEntity
import com.example.mykku.dailymessage.domain.entity.DailyMessageComment
import com.example.mykku.dailymessage.domain.vo.DailyMessageCommentId
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "daily_message_comment")
class DailyMessageCommentJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "content")
    var content: String,

    @Column(name = "like_count")
    var likeCount: Int = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_message_id")
    val dailyMessage: DailyMessageJpaEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    val parentComment: DailyMessageCommentJpaEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    val member: MemberJpaEntity? = null
) : BaseJpaEntity() {

    fun toDomain(): DailyMessageComment {
        return DailyMessageComment.reconstitute(
            id = DailyMessageCommentId.of(id!!),
            dailyMessageId = dailyMessage.id!!,
            memberId = member?.id,
            content = content,
            likeCount = likeCount,
            parentCommentId = parentComment?.id,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    fun updateFromDomain(comment: DailyMessageComment) {
        this.content = comment.content
        this.likeCount = comment.likeCount
    }

    companion object {
        fun fromDomain(
            comment: DailyMessageComment,
            dailyMessageJpaEntity: DailyMessageJpaEntity,
            memberEntity: MemberJpaEntity,
            parentCommentEntity: DailyMessageCommentJpaEntity? = null
        ): DailyMessageCommentJpaEntity {
            return DailyMessageCommentJpaEntity(
                id = if (comment.id.value == 0L) null else comment.id.value,
                content = comment.content,
                likeCount = comment.likeCount,
                dailyMessage = dailyMessageJpaEntity,
                parentComment = parentCommentEntity,
                member = memberEntity
            )
        }
    }
}
