package com.example.mykku.contract.support

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

@DisplayName("갱신 모드 JVM 잠금")
class ContractUpdateLockTest {

    @TempDir
    lateinit var root: Path

    @Test
    fun `다른 프로세스가 잡은 잠금은 잡지 못하고, 그 프로세스가 끝나면 잡는다`() {
        val path = root.resolve("contract-update.lock")
        val holder = startHolder(path)
        try {
            assertThat(holder.inputReader().readLine()).isEqualTo(ContractUpdateLockHolder.HELD)
            assertThatThrownBy { ContractUpdateLock(path).hold() }.isInstanceOf(ContractViolationError::class.java)
        } finally {
            holder.destroyForcibly().waitFor()
        }
        assertThatCode { ContractUpdateLock(path).apply { hold() }.release() }.doesNotThrowAnyException()
    }

    @Test
    fun `잠금을 잡은 쪽은 다시 잡아도 통과하고, 같은 파일을 여는 다른 쪽은 거절을 기억한다`() {
        val path = root.resolve(".gradle/contract-update.lock")
        val holder = ContractUpdateLock(path)
        val refused = ContractUpdateLock(path)
        try {
            holder.hold()
            holder.hold()
            assertThatThrownBy { refused.hold() }.isInstanceOf(ContractViolationError::class.java)
        } finally {
            holder.release()
        }
        assertThatThrownBy { refused.hold() }.isInstanceOf(ContractViolationError::class.java)
    }

    private fun startHolder(path: Path): Process = ProcessBuilder(
        ProcessHandle.current().info().command().orElseThrow(),
        "-cp",
        System.getProperty("java.class.path"),
        ContractUpdateLockHolder::class.java.name,
        path.toString()
    ).redirectErrorStream(true).start()
}
