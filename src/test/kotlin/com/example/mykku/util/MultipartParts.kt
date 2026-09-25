package com.example.mykku.util

import io.restassured.builder.MultiPartSpecBuilder
import io.restassured.specification.MultiPartSpecification

object MultipartParts {

    fun text(name: String, value: String): MultiPartSpecification {
        return MultiPartSpecBuilder(value).controlName(name).charset(Charsets.UTF_8).build()
    }

    fun image(name: String, fileName: String = "image.png"): MultiPartSpecification {
        return MultiPartSpecBuilder("fake-image".toByteArray())
            .controlName(name)
            .fileName(fileName)
            .mimeType("image/png")
            .build()
    }
}
