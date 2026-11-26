package com.example.mykku.contract.matrix

object MatrixFixtures {
    const val MEMBERS = "matrix-members.sql"
    const val PARENTS = "matrix-parents.sql"
    const val FAN_NOTE = "matrix-fan-note.sql"
    const val MEMBER_PK = 9101L
    const val WITHDRAWN_PK = 9103L
    const val LOGIN_EMAIL = "matrix-login@example.com"
    const val LOGIN_PASSWORD = "contract-login-1!"
    const val ABSENT_ID = "999999"
    val HEADER_WITHDRAWAL_PKS = listOf(9111L, 9112L, 9113L, 9114L, 9115L)

    private val PARENT_IDS = mapOf(
        "boardId" to "9201",
        "feedId" to "9301",
        "dailyMessageId" to "9401",
        "id" to "9401",
        "fanNoteId" to "9501"
    )

    private val QUERY_VALUES = mapOf(
        "date" to "2001-01-01",
        "keyword" to "nomatch"
    )

    fun parentId(pathVariable: String): String =
        PARENT_IDS[pathVariable] ?: error("부모 픽스처가 없는 경로 변수입니다: $pathVariable")

    fun queryValue(name: String): String =
        QUERY_VALUES[name] ?: error("필수 쿼리 파라미터의 예시 값이 없습니다: $name")
}
