package com.example.mykku.contract.matrix

object MatrixGoldens {
    const val DIRECTORY = "wire/matrix"
    const val AUTH = "auth.txt"
    const val PAGINATION = "pagination.txt"
    const val EMPTY_STATE = "empty-state.txt"
    const val NOT_FOUND = "not-found.txt"
    const val GRAY_ZONE_DIRECTORY = "wire/gray-zone"
    const val GRAY_ZONE_PREFIX = "matrix-"
    val FILES = setOf(AUTH, PAGINATION, EMPTY_STATE, NOT_FOUND)

    fun path(fileName: String): String = "$DIRECTORY/$fileName"
}
