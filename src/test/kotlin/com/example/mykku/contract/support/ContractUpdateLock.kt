package com.example.mykku.contract.support

import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.WRITE

class ContractUpdateLock(private val path: Path = Path.of(".gradle/contract-update.lock")) {

    private var held: FileLock? = null
    private var refusal: ContractViolationError? = null

    @Synchronized
    fun hold() {
        refusal?.let { throw it }
        if (held != null) {
            return
        }
        Files.createDirectories(path.toAbsolutePath().parent)
        val channel = FileChannel.open(path, CREATE, WRITE)
        val lock = runCatching { tryLock(channel) }.onFailure { channel.close() }.getOrThrow()
        if (lock == null) {
            channel.close()
            throw ContractViolationError(heldMessage()).also { refusal = it }
        }
        held = lock
    }

    @Synchronized
    fun release() {
        held?.channel()?.close()
        held = null
    }

    private fun tryLock(channel: FileChannel): FileLock? = try {
        channel.tryLock()
    } catch (exception: OverlappingFileLockException) {
        null
    }

    private fun heldMessage(): String = listOf(
        "[API 계약] 갱신 모드(-Pcontract.update=true)는 테스트 JVM 하나에서만 돈다. 다른 테스트 JVM이 $path 를 잡고 있다.",
        "- MANIFEST.sha256은 JVM마다 따로 읽고 다시 쓰므로 fork가 여럿이면 서로의 갱신을 덮어쓴다.",
        "- 이 JVM은 남은 계약 테스트도 모두 이 오류로 실패시킨다. 갱신 실행 하나가 끝난 뒤 다시 돌린다.",
        "- build.gradle.kts가 갱신 모드에서 maxParallelForks를 1로 두는지, 같은 worktree에서 Gradle이 하나만 도는지 확인한다."
    ).joinToString("\n")

    companion object {
        val DEFAULT: ContractUpdateLock = ContractUpdateLock()
    }
}
