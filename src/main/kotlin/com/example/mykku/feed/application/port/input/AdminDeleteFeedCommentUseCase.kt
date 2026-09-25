package com.example.mykku.feed.application.port.input

interface AdminDeleteFeedCommentUseCase {
    fun execute(commentId: Long)
}
