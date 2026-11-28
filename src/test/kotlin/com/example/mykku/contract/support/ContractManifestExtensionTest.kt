package com.example.mykku.contract.support

import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.nio.file.Path

@DisplayName("계약 MANIFEST 확장: 갱신 모드 JVM 잠금")
class ContractManifestExtensionTest {

    @TempDir
    lateinit var root: Path

    @Test
    fun `갱신 모드에서는 다른 쪽이 잠금을 쥐고 있으면 클래스 시작에서 실패한다`() {
        val path = root.resolve("contract-update.lock")
        val holder = ContractUpdateLock(path).apply { hold() }
        try {
            assertThatThrownBy { extension(updating = true, path).beforeAll(context()) }
                .isInstanceOf(ContractViolationError::class.java)
        } finally {
            holder.release()
        }
    }

    @Test
    fun `검증 모드에서는 잠금을 보지 않는다`() {
        val path = root.resolve("contract-update.lock")
        val holder = ContractUpdateLock(path).apply { hold() }
        try {
            assertThatCode { extension(updating = false, path).beforeAll(context()) }.doesNotThrowAnyException()
        } finally {
            holder.release()
        }
    }

    private fun extension(updating: Boolean, lockPath: Path) =
        ContractManifestExtension(ContractUpdateMode(enabled = updating, ci = false), ContractUpdateLock(lockPath))

    private fun context(): ExtensionContext {
        val store = mock<ExtensionContext.Store>()
        val rootContext = mock<ExtensionContext> { on { getStore(any()) } doReturn store }
        return mock { on { root } doReturn rootContext }
    }
}
