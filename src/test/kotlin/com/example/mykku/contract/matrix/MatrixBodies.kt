package com.example.mykku.contract.matrix

import com.example.mykku.contract.support.ArgumentKind
import com.example.mykku.contract.support.Endpoint

object MatrixBodies {

    const val FEED_CREATE_JSON = """{"title":"matrix","content":"matrix","boardId":9201,"tags":[]}"""
    private const val COMMENT = """{"content":"matrix comment"}"""
    private const val REQUEST_PART = "request"
    private val BODY_KINDS = setOf(ArgumentKind.BODY, ArgumentKind.PART)

    private val VALID: Map<String, MatrixBody> = authAndMemberBodies() + contentBodies() + settingBodies()

    private val CONSTRAINT_VIOLATING: Map<String, MatrixBody> = mapOf(
        "POST /api/v1/feeds multipart/form-data" to
            JsonPartBody(REQUEST_PART, """{"title":"","content":"matrix","boardId":9201,"tags":[]}"""),
        "PATCH /api/v1/feeds/{feedId} multipart/form-data" to
            JsonPartBody(REQUEST_PART, """{"tags":["t1","t2","t3","t4","t5","t6","t7","t8"]}""")
    )

    fun valid(endpoint: Endpoint): MatrixBody {
        if (endpoint.arguments.none { it.kind in BODY_KINDS && !it.file }) {
            return NoBody
        }
        return VALID[keyOf(endpoint)] ?: error("매트릭스 예시 본문이 없는 엔드포인트입니다: ${keyOf(endpoint)}")
    }

    fun constraintViolating(endpoint: Endpoint): MatrixBody? = CONSTRAINT_VIOLATING[keyOf(endpoint)]

    private fun keyOf(endpoint: Endpoint): String =
        (listOf(endpoint.method, endpoint.pattern) + endpoint.consumes).joinToString(" ")

    private fun authAndMemberBodies(): Map<String, MatrixBody> = mapOf(
        "POST /api/v1/auth/logout" to JsonBody("""{"deviceId":"matrix-device"}"""),
        "PATCH /api/v1/members/me application/json" to JsonBody("""{"nickname":"matrix"}"""),
        "PATCH /api/v1/members/me multipart/form-data" to JsonPartBody(REQUEST_PART, """{"nickname":"matrix"}"""),
        "PATCH /api/v1/members/me/member-id" to JsonBody("""{"memberId":"matrixchanged"}"""),
        "PUT /api/v1/members/password" to
            JsonBody("""{"currentPassword":"matrix-current-1!","newPassword":"matrix123!"}"""),
        "POST /api/v1/members/setup-profile" to JsonBody("""{"memberId":"matrixsetup","nickname":"matrix"}""")
    )

    private fun contentBodies(): Map<String, MatrixBody> = mapOf(
        "POST /api/v1/feeds multipart/form-data" to JsonPartBody(REQUEST_PART, FEED_CREATE_JSON),
        "PATCH /api/v1/feeds/{feedId} multipart/form-data" to JsonPartBody(REQUEST_PART, """{"title":"matrix"}"""),
        "POST /api/v1/feeds/{feedId}/comments" to JsonBody(COMMENT),
        "PUT /api/v1/feeds/comments/{commentId}" to JsonBody(COMMENT),
        "POST /api/v1/daily-messages/{dailyMessageId}/comments" to JsonBody(COMMENT),
        "PUT /api/v1/daily-messages/comments/{commentId}" to JsonBody(COMMENT),
        "PATCH /api/v1/contests/winners/{winnerId}/acceptance-speech" to
            JsonBody("""{"acceptanceSpeech":"matrix speech"}"""),
        "POST /api/v1/reports" to JsonBody("""{"targetType":"FEED","targetId":999999,"reason":"SPAM"}""")
    )

    private fun settingBodies(): Map<String, MatrixBody> = mapOf(
        "POST /api/v1/blocks/keywords" to JsonBody("""{"keyword":"matrixkeyword"}"""),
        "POST /api/v1/blocks/members" to JsonBody("""{"memberId":"matrixabsent"}"""),
        "POST /api/v1/fcm-tokens" to JsonBody("""{"token":"matrix-fcm-token","deviceId":"matrix-device"}"""),
        "PATCH /api/v1/notification-settings" to JsonBody("""{"notificationType":"FEED_LIKE","isEnabled":true}"""),
        "POST /api/v1/preferences/genre" to JsonBody("""{"genreTypes":["KPOP"]}"""),
        "POST /api/v1/preferences/goods" to JsonBody("""{"goodsTypes":["UCHIWA"]}"""),
        "POST /api/v1/preferences/mood" to JsonBody("""{"moodTypes":["COZY"]}""")
    )
}
