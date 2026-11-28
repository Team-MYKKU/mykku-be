package com.example.mykku.config.fork

import java.sql.Connection
import java.time.Duration

class SlotLocks(
    private val connection: Connection,
    private val prefix: String = LOCK_PREFIX,
    private val slots: IntRange = 1..MAX_SLOTS
) {

    fun lockFree(waitLimit: Duration = WAIT_LIMIT, inUse: (Int) -> Boolean = { false }): Int {
        val deadline = System.nanoTime() + waitLimit.toNanos()
        while (true) {
            slots.firstOrNull { tryLock(it) && keepUnlessInUse(it, inUse) }?.let { return it }
            check(System.nanoTime() < deadline) { exhaustedMessage(waitLimit) }
            Thread.sleep(RETRY_INTERVAL.toMillis())
        }
    }

    private fun keepUnlessInUse(slot: Int, inUse: (Int) -> Boolean): Boolean {
        if (!inUse(slot)) {
            return true
        }
        lockFunction("SELECT RELEASE_LOCK(?)", slot)
        return false
    }

    private fun tryLock(slot: Int): Boolean = lockFunction("SELECT GET_LOCK(?, 0)", slot) == 1

    private fun lockFunction(sql: String, slot: Int): Int = connection.prepareStatement(sql).use { statement ->
        statement.setString(1, prefix + slot)
        statement.executeQuery().use { if (it.next()) it.getInt(1) else 0 }
    }

    private fun exhaustedMessage(waitLimit: Duration): String =
        "테스트 fork 슬롯 ${slots.first}~${slots.last}(Redis DB 번호와 같음)이 ${waitLimit.toSeconds()}초 동안 모두 " +
            "사용 중입니다. 같은 MySQL을 쓰는 다른 테스트 실행이 끝나기를 기다리거나 -PtestForks를 줄입니다"

    companion object {
        const val MAX_SLOTS = 15
        const val LOCK_PREFIX = "mykku-test-fork-"
        private val WAIT_LIMIT: Duration = Duration.ofMinutes(10)
        private val RETRY_INTERVAL: Duration = Duration.ofSeconds(1)
    }
}
