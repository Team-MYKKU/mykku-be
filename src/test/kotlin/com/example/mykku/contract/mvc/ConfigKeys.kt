package com.example.mykku.contract.mvc

object ConfigKeys {

    private val CAMEL_BOUNDARY = Regex("([a-z0-9])([A-Z])")
    private val EXCLUDED_KEYS = setOf(
        "server.port",
        "server.address",
        "server.tomcat.basedir",
        "spring.servlet.multipart.location"
    )
    private val EXCLUDED_PREFIXES = listOf("server.ssl.", "server.tomcat.accesslog.")
    private val SECRET_WORDS = listOf(
        "password",
        "secret",
        "token",
        "credential",
        "key-store",
        "trust-store",
        "private-key"
    )

    fun canonical(name: String): String = name.split('.').joinToString(".") { element(it) }

    fun isExcluded(key: String): Boolean = key in EXCLUDED_KEYS ||
        EXCLUDED_PREFIXES.any { key.startsWith(it) } ||
        SECRET_WORDS.any { key.substringAfterLast('.').contains(it) }

    private fun element(element: String): String {
        val base = element.substringBefore('[')
        return base.replace(CAMEL_BOUNDARY, "\$1-\$2").replace('_', '-').lowercase() + element.removePrefix(base)
    }
}
