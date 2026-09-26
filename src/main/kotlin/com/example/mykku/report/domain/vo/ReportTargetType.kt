package com.example.mykku.report.domain.vo

enum class ReportTargetType(val description: String) {
    FEED("게시글"),
    FEED_COMMENT("댓글"),
    DAILY_MESSAGE_COMMENT("하루 덕담 댓글")
}
