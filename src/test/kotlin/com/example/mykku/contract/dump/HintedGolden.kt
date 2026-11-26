package com.example.mykku.contract.dump

import com.example.mykku.contract.support.ContractGolden
import com.example.mykku.contract.support.ContractViolationError
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files

data class HintedEntry(val key: String, val text: String, val hint: String)

object HintedGolden {

    private const val HINT_LIMIT = 40

    fun verify(relativePath: String, entries: List<HintedEntry>) {
        val violation = ContractGolden.DEFAULT.violationOf(relativePath, entries.joinToString("") { it.text }) ?: return
        throw ContractViolationError(violation + "\n" + hints(relativePath, entries))
    }

    private fun hints(relativePath: String, entries: List<HintedEntry>): String {
        val golden = "\n" + readGolden(relativePath)
        val changed = entries.filterNot { golden.contains("\n" + it.text) }
        val header = "--- 골든과 다른 항목의 핸들러(앞 ${HINT_LIMIT}개, 모두 ${changed.size}개) ---"
        return (listOf(header) + changed.take(HINT_LIMIT).map { "- ${it.key} → ${it.hint}" }).joinToString("\n")
    }

    private fun readGolden(relativePath: String): String {
        val path = ContractGolden.CONTRACT_ROOT.resolve(relativePath)
        return if (Files.exists(path)) String(Files.readAllBytes(path), UTF_8) else ""
    }
}
