package com.example.mykku.contract.matrix

import io.restassured.RestAssured
import io.restassured.response.Response

data class MatrixRequest(
    val method: String,
    val path: String,
    val query: List<Pair<String, String>> = emptyList(),
    val authorization: String? = null,
    val body: MatrixBody = NoBody
) {

    fun send(): Response {
        val spec = body.applyTo(RestAssured.given())
        query.forEach { (name, value) -> spec.queryParam(name, value) }
        authorization?.let { spec.header(AUTHORIZATION_HEADER, it) }
        return spec.request(method, path)
    }

    fun withQuery(name: String, value: String): MatrixRequest =
        copy(query = query.filterNot { it.first == name } + (name to value))

    fun describe(): String = listOf("$method ${target()}", body.label).filter { it.isNotEmpty() }.joinToString(" ")

    private fun target(): String {
        if (query.isEmpty()) {
            return path
        }
        return path + "?" + query.joinToString("&") { (name, value) -> "$name=$value" }
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
    }
}
