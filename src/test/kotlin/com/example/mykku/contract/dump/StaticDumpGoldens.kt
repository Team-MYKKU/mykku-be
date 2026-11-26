package com.example.mykku.contract.dump

import com.example.mykku.contract.endpoint.EndpointContractTest
import com.example.mykku.contract.jackson.JacksonConfigContractTest
import com.example.mykku.contract.jackson.JacksonProbeContractTest
import com.example.mykku.contract.mvc.MvcConfigContractTest
import com.example.mykku.contract.mvc.ProfileHttpConfigContractTest
import com.example.mykku.contract.schema.RequestValidationContractTest
import com.example.mykku.contract.schema.ResponseSchemaContractTest

object StaticDumpGoldens {

    fun filesByDirectory(): Map<String, Set<String>> = verifiedGoldens()
        .groupBy({ it.substringBeforeLast('/') }, { it.substringAfterLast('/') })
        .mapValues { it.value.toSet() }

    private fun verifiedGoldens(): List<String> = listOf(
        EndpointContractTest.GOLDENS,
        ResponseSchemaContractTest.GOLDENS,
        RequestValidationContractTest.GOLDENS,
        JacksonConfigContractTest.GOLDENS,
        JacksonProbeContractTest.GOLDENS,
        MvcConfigContractTest.GOLDENS,
        ProfileHttpConfigContractTest.GOLDENS
    ).flatten()
}
