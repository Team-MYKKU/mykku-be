package com.example.mykku.contract.schema

object SchemaBlocks {

    fun render(header: String, lines: List<String>): String =
        (listOf("## $header") + lines).joinToString("\n", postfix = "\n\n")
}
