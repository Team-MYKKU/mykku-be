package com.example.mykku.contract.mvc

object ConfigKeys {

    private val CAMEL_BOUNDARY = Regex("([a-z0-9])([A-Z])")
    private val LIST_INDEX = Regex("\\[\\d+]")
    private val EXCLUDED_KEYS = setOf(
        "server.port",
        "server.tomcat.basedir",
        "spring.servlet.multipart.location"
    )
    private val EXCLUDED_PREFIXES = listOf("server.tomcat.accesslog.")
    private val SECRET_WORDS = listOf(
        "password",
        "secret",
        "token",
        "credential",
        "credentials",
        "key-store",
        "trust-store",
        "private-key"
    )

    fun canonical(name: String): String = name.split('.').joinToString(".") { element(it) }

    fun identity(key: String): String = key.replace("-", "")

    fun listRoot(key: String): String = LIST_INDEX.find(key)?.let { key.substring(0, it.range.first) } ?: key

    fun isUnderRoot(key: String, root: String): Boolean {
        val id = identity(key)
        val rootId = identity(root)
        val indexed = id.startsWith(rootId) && LIST_INDEX.find(id, rootId.length)?.range?.first == rootId.length
        return id == rootId || indexed
    }

    fun isExcluded(key: String): Boolean =
        key in EXCLUDED_KEYS || EXCLUDED_PREFIXES.any { key.startsWith(it) } || isSecretLike(key)

    private fun isSecretLike(key: String): Boolean {
        val last = "-${key.substringAfterLast('.').substringBefore('[')}-"
        return SECRET_WORDS.any { last.contains("-$it-") }
    }

    private fun element(element: String): String {
        val base = element.substringBefore('[')
        return base.replace(CAMEL_BOUNDARY, "\$1-\$2").replace('_', '-').lowercase() + element.removePrefix(base)
    }
}
