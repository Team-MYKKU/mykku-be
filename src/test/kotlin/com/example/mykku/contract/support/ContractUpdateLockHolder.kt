package com.example.mykku.contract.support

import java.nio.file.Path
import java.time.Duration

object ContractUpdateLockHolder {

    const val HELD = "contract-update-lock-held"
    private val HOLD_LIMIT: Duration = Duration.ofMinutes(1)

    @JvmStatic
    fun main(args: Array<String>) {
        ContractUpdateLock(Path.of(args.single())).hold()
        println(HELD)
        Thread.sleep(HOLD_LIMIT.toMillis())
    }
}
