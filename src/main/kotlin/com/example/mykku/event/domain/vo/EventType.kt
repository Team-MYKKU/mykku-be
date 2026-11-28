package com.example.mykku.event.domain.vo

enum class EventType(val description: String) {
    GENERAL("일반 이벤트(앱 안 참여 없음)"),
    ATTENDANCE("출석체크"),
    GOOGLE_FORM("구글폼 제출"),
    ENTRY("응모")
}
