package com.example.mykku.contract.matrix

import com.example.mykku.contract.support.Endpoint
import com.example.mykku.contract.support.JsonRules
import io.restassured.response.Response

object MatrixRules {

    private val SUCCESS = 200..299

    val PAGE_IMPL_ENDPOINTS = setOf(
        "GET /api/v1/daily-messages",
        "GET /api/v1/fan-notes",
        "GET /api/v1/likes/boards",
        "GET /api/v1/notifications",
        "GET /api/v1/notifications/unread"
    )

    fun of(endpoint: Endpoint, response: Response): JsonRules {
        val pageImpl = keyOf(endpoint) in PAGE_IMPL_ENDPOINTS
        return if (pageImpl && response.statusCode in SUCCESS) JsonRules.pageImpl() else JsonRules()
    }

    fun keyOf(endpoint: Endpoint): String = "${endpoint.method} ${endpoint.pattern}"
}
