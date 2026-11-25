package com.example.mykku.contract.support

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

class ContractManifestExtension : BeforeAllCallback {

    override fun beforeAll(context: ExtensionContext) {
        context.root.getStore(NAMESPACE).getOrComputeIfAbsent(ManifestRefresh::class.java.name) { ManifestRefresh() }
    }

    private companion object {
        val NAMESPACE: ExtensionContext.Namespace =
            ExtensionContext.Namespace.create(ContractManifestExtension::class.java)
    }
}

class ManifestRefresh(
    private val manifest: GoldenManifest = GoldenManifest(),
    private val updateMode: ContractUpdateMode = ContractUpdateMode.fromEnvironment(),
    private val baseResolver: () -> String = { GoldenManifest.mergeBaseWithDevelop() }
) : ExtensionContext.Store.CloseableResource {

    override fun close() {
        if (!updateMode.enabled) {
            return
        }
        updateMode.requireAllowed()
        val recordedBase = manifest.read()?.base?.takeIf { GoldenManifest.isCommit(it) }
        manifest.write(recordedBase ?: baseResolver())
    }
}
