package com.example.mykku.contract.snapshot

import com.example.mykku.contract.snapshot.SnapshotFixtures.GOOGLE
import com.example.mykku.contract.snapshot.SnapshotFixtures.jsonPart
import com.example.mykku.contract.support.ContractViolationError
import com.example.mykku.contract.support.JsonPath
import io.restassured.response.Response
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class PrecisionProbe(val golden: String, val path: String, val call: SnapshotSession.() -> Response) {
    override fun toString(): String = golden
}

@EnabledOnOs(OS.LINUX)
@DisplayName("실경로 스냅샷: 서버가 만든 시각의 정밀도 프로브")
class PrecisionProbeTest : ContractSnapshotTest() {

    override val fixtures = listOf(SnapshotFixtures.BASE)

    @ParameterizedTest(name = "{0}")
    @MethodSource("probes")
    fun `같은 생성 요청 5번의 최대 소수 자릿수가 7 이상인지는 골든과 같다`(probe: PrecisionProbe) {
        val digits = (1..REPEAT).map { fractionDigits(probe, probe.call(this)) }
        val text = "### ${lastRequestLine()} x$REPEAT ${probe.path}\n" +
            "최대 소수 자릿수 $JVM_CLOCK_DIGITS 이상: ${digits.max() >= JVM_CLOCK_DIGITS}\n"

        golden.verify(SnapshotGoldens.precision(probe.golden), text)
    }

    private fun fractionDigits(probe: PrecisionProbe, response: Response): Int {
        if (response.statusCode != SUCCESS) {
            throw ContractViolationError(
                "[API 계약 위반] 정밀도 프로브 요청 실패: ${lastRequestLine()} -> ${response.statusCode} ${response.asString()}"
            )
        }
        val text = JsonPath.select(jsonOf(response), probe.path).single().asText()
        ServerTime.token(probe.path, text, jvmNow())
        return text.substringAfter('.', "").length
    }

    companion object {
        private const val REPEAT = 5
        private const val JVM_CLOCK_DIGITS = 7
        private const val SUCCESS = 200
        private val PROBE_FEED = mapOf("title" to "프로브 피드", "content" to "프로브 본문", "boardId" to 1)

        @JvmStatic
        fun probes(): List<PrecisionProbe> = listOf(
            PrecisionProbe("feed-create", "/data/createdAt") {
                multipart("POST", "/api/v1/feeds", GOOGLE, listOf(jsonPart("request", PROBE_FEED)))
            },
            PrecisionProbe("feed-comment-create", "/data/createdAt") {
                send("POST", "/api/v1/feeds/1/comments", GOOGLE, mapOf("content" to "프로브 댓글"))
            },
            PrecisionProbe("daily-message-comment-create", "/data/createdAt") {
                send("POST", "/api/v1/daily-messages/1/comments", GOOGLE, mapOf("content" to "프로브 덕담 댓글"))
            }
        )
    }
}
