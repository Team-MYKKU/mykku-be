package com.example.mykku.contract.endpoint

import com.example.mykku.contract.support.Endpoint

object EndpointKeys {

    fun of(endpoint: Endpoint): String = listOfNotNull(
        endpoint.method,
        endpoint.pattern,
        condition("consumes", endpoint.consumes),
        condition("produces", endpoint.produces),
        condition("params", endpoint.params),
        condition("headers", endpoint.headers)
    ).joinToString(" ")

    private fun condition(name: String, values: List<String>): String? =
        values.takeIf { it.isNotEmpty() }?.joinToString(",", prefix = "$name=")
}
