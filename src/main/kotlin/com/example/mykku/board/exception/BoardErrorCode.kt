package com.example.mykku.board.exception

import com.example.mykku.common.exception.DomainErrorCode
import org.springframework.http.HttpStatus

enum class BoardErrorCode(
    override val code: String,
    override val status: HttpStatus,
    override val message: String
) : DomainErrorCode {
    BOARD_NOT_FOUND("BO001", HttpStatus.NOT_FOUND, "게시판을 찾을 수 없습니다"),

    MOVE_TARGET_SAME_BOARD("BO101", HttpStatus.BAD_REQUEST, "글을 옮길 게시판은 삭제할 게시판과 달라야 합니다"),
    MOVE_TARGET_BOARD_NOT_FOUND("BO102", HttpStatus.BAD_REQUEST, "글을 옮길 게시판을 지정하지 않았거나 찾을 수 없습니다"),

    BOARD_HAS_FEEDS("BO301", HttpStatus.CONFLICT, "게시글이 있는 게시판은 글 처리 방식을 정해야 삭제할 수 있습니다")
}
