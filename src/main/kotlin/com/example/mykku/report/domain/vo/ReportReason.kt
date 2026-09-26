package com.example.mykku.report.domain.vo

enum class ReportReason(val description: String) {
    ABUSE("욕설 및 비방"),
    OBSCENE("음란하거나 부적절한 콘텐츠"),
    SPAM("반복 게시물, 광고 및 무분별한 홍보"),
    FRAUD("사칭 및 허위 정보"),
    PERSONAL_INFO("개인정보 노출"),
    COPYRIGHT("저작권 침해"),
    ETC("기타")
}
