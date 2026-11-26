package com.example.mykku.contract.snippet

import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext

interface SnippetIdentifierSource {
    fun drainSnippetIdentifiers(): List<String>
}

class SnippetGoldenExtension(
    private val snippetGolden: SnippetGolden = SnippetGolden.DEFAULT
) : AfterTestExecutionCallback {

    override fun afterTestExecution(context: ExtensionContext) {
        val sources = context.requiredTestInstances.allInstances.filterIsInstance<SnippetIdentifierSource>()
        val identifiers = sources.flatMap { it.drainSnippetIdentifiers() }.distinct()
        if (context.executionException.isPresent) {
            return
        }
        identifiers.forEach { snippetGolden.verify(it) }
    }
}
