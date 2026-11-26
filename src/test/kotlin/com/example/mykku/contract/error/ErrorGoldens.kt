package com.example.mykku.contract.error

object ErrorGoldens {
    const val DIRECTORY = "wire/errors"
    const val FRAMEWORK_MAPPING = "framework-mapping.txt"
    const val FRAMEWORK_HTTP = "framework-http.txt"
    const val GRAY_ZONE_DIRECTORY = "wire/gray-zone"
    const val GRAY_ZONE_PREFIX = "framework-"
    const val GRAY_ZONE_FRAMEWORK_HTTP = "framework-http.txt"
    val FILES = setOf(FRAMEWORK_MAPPING, FRAMEWORK_HTTP)
    val GRAY_ZONE_FILES = setOf(GRAY_ZONE_FRAMEWORK_HTTP)

    fun path(fileName: String): String = "$DIRECTORY/$fileName"

    fun grayZonePath(fileName: String): String = "$GRAY_ZONE_DIRECTORY/$fileName"
}
