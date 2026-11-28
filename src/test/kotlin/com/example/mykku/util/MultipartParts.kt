package com.example.mykku.util

import io.restassured.builder.MultiPartSpecBuilder
import io.restassured.specification.MultiPartSpecification

object MultipartParts {

    fun text(name: String, value: String): MultiPartSpecification {
        return MultiPartSpecBuilder(value).controlName(name).charset(Charsets.UTF_8).build()
    }

    fun image(name: String, fileName: String = "image.png"): MultiPartSpecification {
        return file(name, fileName, "fake-image".toByteArray())
    }

    fun emptyImage(name: String, fileName: String = "empty.png"): MultiPartSpecification {
        return file(name, fileName, ByteArray(0))
    }

    private fun file(name: String, fileName: String, content: ByteArray): MultiPartSpecification {
        return MultiPartSpecBuilder(content)
            .controlName(name)
            .fileName(fileName)
            .mimeType("image/png")
            .build()
    }
}
