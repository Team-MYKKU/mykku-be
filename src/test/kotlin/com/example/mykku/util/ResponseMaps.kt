package com.example.mykku.util

object ResponseMaps {

    fun withoutKeys(value: Any?, keys: Set<String>): Any? {
        return when (value) {
            is Map<*, *> -> value.filterKeys { it !in keys }.mapValues { withoutKeys(it.value, keys) }
            is List<*> -> value.map { withoutKeys(it, keys) }
            else -> value
        }
    }
}
