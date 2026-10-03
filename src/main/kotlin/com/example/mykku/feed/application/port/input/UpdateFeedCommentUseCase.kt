package com.example.mykku.feed.application.port.input

import com.example.mykku.comment.application.dto.CommentResult
import com.example.mykku.feed.application.dto.UpdateFeedCommentCommand
import com.example.mykku.member.domain.entity.Member

interface UpdateFeedCommentUseCase {
    fun execute(command: UpdateFeedCommentCommand, member: Member): CommentResult
}
