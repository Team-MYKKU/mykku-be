package com.example.mykku.dailymessage.application.port.input

interface AdminDeleteDailyMessageCommentUseCase {
    fun execute(commentId: Long)
}
