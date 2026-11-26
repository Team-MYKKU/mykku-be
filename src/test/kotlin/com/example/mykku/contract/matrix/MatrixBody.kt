package com.example.mykku.contract.matrix

import io.restassured.specification.RequestSpecification

sealed interface MatrixBody {
    val label: String

    fun applyTo(spec: RequestSpecification): RequestSpecification
}

data object NoBody : MatrixBody {
    override val label: String = ""

    override fun applyTo(spec: RequestSpecification): RequestSpecification = spec
}

data class JsonBody(private val json: String) : MatrixBody {
    override val label: String = "body=$json"

    override fun applyTo(spec: RequestSpecification): RequestSpecification =
        spec.contentType(JSON_MEDIA_TYPE).body(json)
}

data class JsonPartBody(private val part: String, private val json: String) : MatrixBody {
    override val label: String = "multipart[$part=$json]"

    override fun applyTo(spec: RequestSpecification): RequestSpecification =
        spec.multiPart(part, json, JSON_MEDIA_TYPE)
}

private const val JSON_MEDIA_TYPE = "application/json"
