package com.example.mykku.contract.matrix

import io.restassured.RestAssured
import io.restassured.config.EncoderConfig
import io.restassured.config.RestAssuredConfig
import io.restassured.http.ContentType

object ExactContentType {
    const val TEXT_PLAIN = "text/plain"
    const val APPLICATION_XML = "application/xml"
    const val MULTIPART_FORM_DATA = "multipart/form-data"

    val CONFIG: RestAssuredConfig = RestAssured.config().encoderConfig(
        EncoderConfig.encoderConfig()
            .encodeContentTypeAs(MULTIPART_FORM_DATA, ContentType.TEXT)
            .appendDefaultContentCharsetToContentTypeIfUndefined(false)
    )
}
