package com.example.mykku.contract.header

object HeaderGoldens {
    const val DIRECTORY = "wire/headers"
    const val RESPONSE_HEADERS = "response-headers.txt"
    val FILES = setOf(RESPONSE_HEADERS)

    fun path(fileName: String): String = "$DIRECTORY/$fileName"
}
