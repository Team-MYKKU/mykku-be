package com.example.mykku.contract.schema

import com.example.mykku.contract.endpoint.ArgumentTokens
import com.example.mykku.contract.endpoint.EndpointKeys
import com.example.mykku.contract.support.ArgumentKind
import com.example.mykku.contract.support.Endpoint
import com.example.mykku.contract.support.HandlerArgument
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType

enum class RequestBinding { JSON, FORM }

data class RequestTarget(
    val header: String,
    val type: KType,
    val binding: RequestBinding,
    val validated: Boolean
)

class RequestTargets(private val tokens: ArgumentTokens) {

    fun of(endpoint: Endpoint): List<RequestTarget> = endpoint.arguments.mapNotNull { argument ->
        bindingOf(argument)?.let { targetOf(endpoint, argument, it) }
    }

    private fun targetOf(endpoint: Endpoint, argument: HandlerArgument, binding: RequestBinding): RequestTarget =
        RequestTarget(headerOf(endpoint, argument), typeOf(argument), binding, argument.validated)

    private fun headerOf(endpoint: Endpoint, argument: HandlerArgument): String =
        "${EndpointKeys.of(endpoint)} ${tokens.of(argument)}"

    private fun bindingOf(argument: HandlerArgument): RequestBinding? = when {
        argument.kind == ArgumentKind.BODY -> RequestBinding.JSON
        argument.kind == ArgumentKind.PART && !argument.file -> RequestBinding.JSON
        argument.kind == ArgumentKind.MODEL -> RequestBinding.FORM
        else -> null
    }

    private fun typeOf(argument: HandlerArgument): KType =
        argument.kotlinType ?: argument.parameter.nestedParameterType.kotlin.starProjectedType
}
