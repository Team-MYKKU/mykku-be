package com.example.mykku.contract.dump

import com.example.mykku.contract.support.EndpointArea

object StaticDumpGoldens {
    const val RESPONSE_SCHEMA = "wire/schema/response.txt"
    const val REQUEST_SCHEMA = "wire/schema/request.txt"
    const val JACKSON_CONFIG = "wire/jackson/config.txt"
    const val JACKSON_SERIALIZE = "wire/jackson/serialize.json"
    const val JACKSON_DESERIALIZE = "wire/jackson/deserialize.txt"
    const val MVC_CONFIG = "wire/mvc/config.txt"
    const val DEV_PROFILE_HTTP = "wire/mvc/application-dev-http.properties"

    private const val ENDPOINTS = "wire/endpoints"

    fun endpoints(area: EndpointArea): String = "$ENDPOINTS/${area.name.lowercase()}.txt"

    fun filesByDirectory(): Map<String, Set<String>> = allPaths()
        .groupBy({ it.substringBeforeLast('/') }, { it.substringAfterLast('/') })
        .mapValues { it.value.toSet() }

    private fun allPaths(): List<String> = EndpointArea.entries.map { endpoints(it) } + listOf(
        RESPONSE_SCHEMA,
        REQUEST_SCHEMA,
        JACKSON_CONFIG,
        JACKSON_SERIALIZE,
        JACKSON_DESERIALIZE,
        MVC_CONFIG,
        DEV_PROFILE_HTTP
    )
}
