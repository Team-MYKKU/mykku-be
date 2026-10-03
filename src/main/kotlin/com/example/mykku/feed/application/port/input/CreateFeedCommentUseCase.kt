package com.example.mykku.feed.application.port.input

import com.example.mykku.comment.application.dto.CommentResult
import com.example.mykku.feed.application.dto.CreateFeedCommentCommand
import com.example.mykku.member.domain.entity.Member

interface CreateFeedCommentUseCase {
    fun execute(command: CreateFeedCommentCommand, member: Member): CommentResult
}
