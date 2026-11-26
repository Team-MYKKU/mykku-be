package com.example.mykku.contract.endpoint

import com.example.mykku.contract.support.AuthLevel
import com.example.mykku.contract.support.Endpoint

class EndpointLines(
    private val adminSession: AdminSessionProbe,
    private val argumentTokens: ArgumentTokens
) {

    fun render(endpoint: Endpoint): String =
        listOf(EndpointKeys.of(endpoint), "auth=${auth(endpoint)}", arguments(endpoint)).joinToString(" ")

    private fun auth(endpoint: Endpoint): String {
        val levels = listOfNotNull(
            ADMIN_SESSION.takeIf { adminSession.requires(endpoint) },
            endpoint.auth.takeUnless { it == AuthLevel.NONE }?.name?.lowercase()
        )
        return levels.ifEmpty { listOf(AuthLevel.NONE.name.lowercase()) }.joinToString("+")
    }

    private fun arguments(endpoint: Endpoint): String =
        endpoint.arguments.mapNotNull { argumentTokens.of(it) }.joinToString(", ", "(", ")")

    private companion object {
        const val ADMIN_SESSION = "admin-session"
    }
}
