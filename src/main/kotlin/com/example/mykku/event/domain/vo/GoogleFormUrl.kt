package com.example.mykku.event.domain.vo

import com.example.mykku.event.exception.EventException
import java.net.URI
import java.net.URISyntaxException

@JvmInline
value class GoogleFormUrl private constructor(val value: String) {
    companion object {
        const val MAX_LENGTH = 500
        private const val HTTPS = "https"
        private const val FORMS_HOST = "docs.google.com"
        private const val SHORT_LINK_HOST = "forms.gle"
        private const val FORMS_PATH_PREFIX = "/forms/"
        private const val RESPONSE_PATH_SUFFIX = "/viewform"
        private val DOT_SEGMENTS = setOf(".", "..")

        fun normalize(raw: String?): String? = raw?.trim()?.ifEmpty { null }

        fun of(value: String): GoogleFormUrl {
            if (value.length > MAX_LENGTH || !isResponseLink(value)) {
                throw EventException.invalidEventTypeSetting()
            }
            return GoogleFormUrl(value)
        }

        private fun isResponseLink(value: String): Boolean {
            val uri = parse(value) ?: return false
            if (!uri.scheme.equals(HTTPS, ignoreCase = true)) return false
            if (uri.rawUserInfo != null || uri.port != -1) return false
            val path = uri.rawPath?.takeIf { isPlainPath(it) } ?: return false
            return when (uri.host?.lowercase()) {
                FORMS_HOST -> isFormsResponsePath(path)
                SHORT_LINK_HOST -> path.length > 1
                else -> false
            }
        }

        private fun isPlainPath(path: String): Boolean {
            return '%' !in path && "//" !in path && path.split('/').none { it in DOT_SEGMENTS }
        }

        private fun isFormsResponsePath(path: String): Boolean {
            return path.startsWith(FORMS_PATH_PREFIX) && path.removeSuffix("/").endsWith(RESPONSE_PATH_SUFFIX)
        }

        private fun parse(value: String): URI? {
            return try {
                URI(value)
            } catch (e: URISyntaxException) {
                null
            }
        }
    }
}
