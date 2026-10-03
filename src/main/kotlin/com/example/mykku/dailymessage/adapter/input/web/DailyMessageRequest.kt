package com.example.mykku.dailymessage.adapter.input.web

import com.example.mykku.dailymessage.application.dto.CreateCommentCommand
import com.example.mykku.dailymessage.application.dto.UpdateCommentCommand

data class CreateCommentRequest(
    val content: String,
    val parentCommentId: Long? = null
) {
    fun toCommand(
        dailyMessageId: Long,
        memberId: Long
    ): CreateCommentCommand {
        return CreateCommentCommand(
            dailyMessageId = dailyMessageId,
            memberId = memberId,
            content = content,
            parentCommentId = parentCommentId
        )
    }
}

data class UpdateCommentRequest(
    val content: String
) {
    fun toCommand(commentId: Long, memberId: Long): UpdateCommentCommand {
        return UpdateCommentCommand(
            commentId = commentId,
            memberId = memberId,
            content = content
        )
    }
}
