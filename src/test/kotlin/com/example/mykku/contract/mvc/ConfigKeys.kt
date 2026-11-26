package com.example.mykku.contract.mvc

object ConfigKeys {

    private val CAMEL_BOUNDARY = Regex("([a-z0-9])([A-Z])")
    private val LIST_INDEX = Regex("\\[\\d+]")
    private val EXCLUDED_KEYS = listOf(
        "server.port",
        "server.tomcat.basedir",
        "server.servlet.session.store-dir",
        "server.ssl.certificate",
        "server.ssl.trust-certificate",
        "spring.servlet.multipart.location"
    )
    private val EXCLUDED_ROOTS = listOf("server.tomcat.accesslog")
    private val SECRET_WORDS = listOf(
        "password",
        "secret",
        "token",
        "credential",
        "keystore",
        "truststore",
        "privatekey"
    )
    private val SAFE_KEYS = listOf("spring.jackson.deserialization.fail-on-trailing-tokens")

    fun canonical(name: String): String = name.split('.').joinToString(".") { element(it) }

    fun identity(key: String): String = key.replace("-", "")

    fun hasPrefix(key: String, prefix: String): Boolean = identity(key).startsWith(identity(prefix))

    fun listRoot(key: String): String = LIST_INDEX.find(key)?.let { key.substring(0, it.range.first) } ?: key

    fun isUnderRoot(key: String, root: String): Boolean {
        val id = identity(key)
        val rootId = identity(root)
        val indexed = id.startsWith(rootId) && LIST_INDEX.find(id, rootId.length)?.range?.first == rootId.length
        return id == rootId || indexed
    }

    fun isAtOrBelow(key: String, root: String): Boolean = isUnderRoot(key, root) || hasPrefix(key, "$root.")

    fun isExcluded(key: String): Boolean =
        EXCLUDED_KEYS.any { isUnderRoot(key, it) } || EXCLUDED_ROOTS.any { isAtOrBelow(key, it) } || isSecretLike(key)

    private fun isSecretLike(key: String): Boolean {
        val id = wordIdentity(key)
        return SAFE_KEYS.none { wordIdentity(it) == id } && SECRET_WORDS.any { id.contains(it) }
    }

    private fun wordIdentity(key: String): String = identity(
        key.replace(CAMEL_BOUNDARY, "\$1-\$2").replace('_', '-').lowercase().replace('[', '.').replace("]", "")
    )

    private fun element(element: String): String {
        val base = element.substringBefore('[')
        return base.replace(CAMEL_BOUNDARY, "\$1-\$2").replace('_', '-').lowercase() + element.removePrefix(base)
    }
}
