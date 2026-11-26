package com.example.mykku.contract.snapshot

import com.example.mykku.contract.support.ContractViolationError
import com.example.mykku.contract.support.JsonMask
import com.fasterxml.jackson.databind.JsonNode
import java.time.Duration
import java.time.LocalDateTime

interface SnapshotClock {
    fun jvmNow(): LocalDateTime

    fun dbNow(): LocalDateTime
}

class SnapshotMask(val path: String, private val token: (String, SnapshotClock) -> String) {

    fun bind(clock: SnapshotClock): JsonMask = JsonMask(path) { token(textOf(it), clock) }

    private fun textOf(node: JsonNode): String =
        node.textValue() ?: throw MaskViolation.of(path, "문자열이 아닙니다", node.toString())

    companion object {
        fun jvmTime(path: String) = SnapshotMask(path) { text, clock -> ServerTime.token(path, text, clock.jvmNow()) }

        fun dbTime(path: String) = SnapshotMask(path) { text, clock -> ServerTime.token(path, text, clock.dbNow()) }

        fun jwt(path: String) = SnapshotMask(path) { text, _ -> JwtToken.token(path, text) }

        fun uploadUrl(path: String) = SnapshotMask(path) { text, _ -> UploadUrl.token(path, text) }
    }
}

private object MaskViolation {
    fun of(path: String, reason: String, text: String) =
        ContractViolationError("[API 계약 위반] 마스크 경로 $path: $reason: '$text'")
}

internal object ServerTime {
    private val TOLERANCE: Duration = Duration.ofMinutes(10)
    private val FORMAT = Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,9})?")

    fun token(path: String, text: String, reference: LocalDateTime): String {
        if (!FORMAT.matches(text)) {
            throw MaskViolation.of(path, "LocalDateTime ISO 형식이 아닙니다", text)
        }
        if (Duration.between(LocalDateTime.parse(text), reference).abs() > TOLERANCE) {
            throw MaskViolation.of(path, "기준 시각 $reference 에서 10분 넘게 떨어져 있습니다", text)
        }
        return if (text.contains('.')) "<LDT.F>" else "<LDT>"
    }
}

private object JwtToken {
    private val FORMAT = Regex("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")

    fun token(path: String, text: String): String {
        if (!FORMAT.matches(text)) {
            throw MaskViolation.of(path, "JWT 형식이 아닙니다", text)
        }
        return "<JWT>"
    }
}

private object UploadUrl {
    private val FORMAT = Regex("(https://test-bucket\\.s3\\.amazonaws\\.com/.+-)\\d+(\\.jpg)")

    fun token(path: String, text: String): String {
        val match = FORMAT.matchEntire(text) ?: throw MaskViolation.of(path, "테스트 업로드 URL 형식이 아닙니다", text)
        return match.groupValues[1] + "<N>" + match.groupValues[2]
    }
}
