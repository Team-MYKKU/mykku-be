package com.example.mykku.contract.snapshot

import com.example.mykku.contract.support.ContractGolden
import java.nio.file.Files

object SnapshotGoldens {
    const val RESPONSES = "wire/responses"
    const val TIES = "$RESPONSES/ties"
    const val PRECISION = "$RESPONSES/precision"
    const val GRAY_ZONE = "wire/gray-zone"
    const val GRAY_ZONE_PREFIX = "snapshot-"

    fun response(name: String): String = "$RESPONSES/$name.http"

    fun claims(name: String): String = "$RESPONSES/$name.claims"

    fun tie(name: String): String = "$TIES/$name.txt"

    fun precision(name: String): String = "$PRECISION/$name.txt"

    fun grayZone(name: String): String = "$GRAY_ZONE/$GRAY_ZONE_PREFIX$name.http"

    fun of(case: SnapshotCase): List<String> =
        listOf(response(case.golden)) + if (case.tokens.isEmpty()) emptyList() else listOf(claims(case.golden))

    fun filesUnder(relativeDir: String): Set<String> {
        val root = ContractGolden.CONTRACT_ROOT
        val dir = root.resolve(relativeDir)
        if (!Files.isDirectory(dir)) {
            return emptySet()
        }
        return Files.walk(dir).use { paths ->
            paths.filter { ContractGolden.isGoldenFile(it) }
                .map { root.relativize(it).joinToString("/") }
                .toList()
                .toSet()
        }
    }
}
