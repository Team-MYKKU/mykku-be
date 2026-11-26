package com.example.mykku.contract.matrix

import com.example.mykku.contract.support.ArgumentKind
import com.example.mykku.contract.support.Endpoint
import com.example.mykku.contract.support.HandlerArgument

object MatrixRequests {

    private const val INVALID_VALUE = "abc"
    private const val MALFORMED_JSON = "{"
    private const val NO_TOKEN = "none"
    private val NUMERIC_TYPES = setOf(Long::class, Int::class)

    fun of(endpoint: Endpoint, pathValue: (HandlerArgument) -> String): MatrixRequest = MatrixRequest(
        method = endpoint.method,
        path = path(endpoint, pathValue),
        query = endpoint.arguments.filter { it.kind == ArgumentKind.QUERY && it.required }
            .map { it.name to MatrixFixtures.queryValue(it.name) },
        body = MatrixBodies.valid(endpoint)
    )

    fun precedenceCases(endpoint: Endpoint): List<Pair<String, MatrixRequest>> {
        val valid = of(endpoint) { MatrixFixtures.ABSENT_ID }.copy(authorization = null)
        return endpoint.arguments.takeWhile { it.kind != ArgumentKind.MEMBER }
            .flatMap { argument -> invalidated(endpoint, valid, argument) }
            .map { (label, request) -> "$NO_TOKEN+$label" to request }
    }

    private fun invalidated(
        endpoint: Endpoint,
        valid: MatrixRequest,
        argument: HandlerArgument
    ): List<Pair<String, MatrixRequest>> = when {
        argument.kind == ArgumentKind.PATH && isNumeric(argument) ->
            listOf("path:${argument.name}" to valid.copy(path = invalidPath(endpoint, argument)))
        argument.kind == ArgumentKind.QUERY && !isText(argument) ->
            listOf("query:${argument.name}" to valid.withQuery(argument.name, INVALID_VALUE))
        argument.kind == ArgumentKind.BODY -> bodyCases(endpoint, valid, "body", JsonBody(MALFORMED_JSON))
        argument.kind == ArgumentKind.PART && !argument.file ->
            bodyCases(endpoint, valid, "part:${argument.name}", JsonPartBody(argument.name, MALFORMED_JSON))
        else -> emptyList()
    }

    private fun bodyCases(
        endpoint: Endpoint,
        valid: MatrixRequest,
        label: String,
        malformed: MatrixBody
    ): List<Pair<String, MatrixRequest>> {
        val constraint = MatrixBodies.constraintViolating(endpoint)
            ?.let { listOf("$label:constraint" to valid.copy(body = it)) }.orEmpty()
        return listOf("$label:malformed" to valid.copy(body = malformed)) + constraint
    }

    private fun invalidPath(endpoint: Endpoint, invalid: HandlerArgument): String =
        path(endpoint) { if (it.name == invalid.name) INVALID_VALUE else MatrixFixtures.ABSENT_ID }

    private fun path(endpoint: Endpoint, pathValue: (HandlerArgument) -> String): String {
        val path = endpoint.pathArguments.fold(endpoint.pattern) { pattern, argument ->
            pattern.replace("{${argument.name}}", pathValue(argument))
        }
        check(!path.contains('{')) { "경로 변수를 모두 채우지 못했습니다: ${endpoint.pattern} -> $path" }
        return path
    }

    private fun isNumeric(argument: HandlerArgument): Boolean = argument.kotlinType?.classifier in NUMERIC_TYPES

    private fun isText(argument: HandlerArgument): Boolean = argument.kotlinType?.classifier == String::class
}
