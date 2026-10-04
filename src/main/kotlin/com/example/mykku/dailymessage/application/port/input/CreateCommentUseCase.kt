package com.example.mykku.dailymessage.application.port.input

import com.example.mykku.comment.application.dto.CommentResult
import com.example.mykku.dailymessage.application.dto.CreateCommentCommand

interface CreateCommentUseCase {
    fun execute(command: CreateCommentCommand): CommentResult
}
