package com.example.mykku.dailymessage.application.port.input

import com.example.mykku.comment.application.dto.CommentPageResult
import org.springframework.data.domain.Pageable

interface GetCommentsUseCase {
    fun execute(dailyMessageId: Long, memberId: Long?, pageable: Pageable): CommentPageResult
}
