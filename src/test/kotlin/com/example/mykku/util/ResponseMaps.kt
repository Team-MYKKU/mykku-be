package com.example.mykku.util

object ResponseMaps {

    private const val MASK = "<masked>"

    fun masked(value: Any?, keys: Set<String>): Any? {
        return when (value) {
            is Map<*, *> -> value.mapValues { (key, item) -> maskedEntry(key, item, keys) }
            is List<*> -> value.map { masked(it, keys) }
            else -> value
        }
    }

    private fun maskedEntry(key: Any?, item: Any?, keys: Set<String>): Any? {
        return if (key in keys && item != null) MASK else masked(item, keys)
    }
}
