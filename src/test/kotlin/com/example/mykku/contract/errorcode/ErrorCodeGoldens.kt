package com.example.mykku.contract.errorcode

object ErrorCodeGoldens {
    const val DIRECTORY = "wire/error-codes"
    const val RESPONSES = "responses.txt"

    fun path(fileName: String): String = "$DIRECTORY/$fileName"
}
