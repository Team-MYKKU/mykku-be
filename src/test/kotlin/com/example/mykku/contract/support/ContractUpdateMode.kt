package com.example.mykku.contract.support

class ContractUpdateMode(
    val enabled: Boolean,
    private val ci: Boolean
) {

    fun requireAllowed() {
        if (enabled && ci) {
            throw ContractViolationError(CI_UPDATE_MESSAGE)
        }
    }

    companion object {
        const val PROPERTY = "contract.update"

        private const val CI_UPDATE_MESSAGE =
            "[API 계약] CI(환경 변수 CI=true)에서는 계약 골든을 갱신할 수 없습니다. " +
                "-Pcontract.update=true는 로컬에서만 씁니다."

        fun fromEnvironment(): ContractUpdateMode = ContractUpdateMode(
            enabled = System.getProperty(PROPERTY).equals("true", ignoreCase = true),
            ci = System.getenv("CI").equals("true", ignoreCase = true)
        )
    }
}
