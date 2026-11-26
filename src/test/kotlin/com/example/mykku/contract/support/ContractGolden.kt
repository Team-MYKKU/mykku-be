package com.example.mykku.contract.support

import org.assertj.core.util.diff.DiffUtils
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class ContractGolden(
    private val goldenRoot: Path = CONTRACT_ROOT,
    private val actualRoot: Path = Path.of("build/contract-actual"),
    private val updateMode: ContractUpdateMode = ContractUpdateMode.fromEnvironment()
) {

    private val touched: MutableSet<String> = touchedUnder(goldenRoot)

    val updating: Boolean
        get() = updateMode.enabled

    fun verify(relativePath: String, actual: String) {
        violationOf(relativePath, actual)?.let { throw ContractViolationError(it) }
    }

    fun violationOf(relativePath: String, actual: String): String? {
        updateMode.requireAllowed()
        val normalized = actual.replace("\r\n", "\n")
        write(resolve(actualRoot, relativePath), normalized)
        if (updateMode.enabled) {
            write(resolve(goldenRoot, relativePath), normalized)
            markTouched(relativePath)
            return null
        }
        return compare(relativePath, normalized)
    }

    fun goldenFiles(relativeDir: String): Set<String> {
        val dir = resolve(goldenRoot, relativeDir)
        if (!Files.isDirectory(dir)) {
            return emptySet()
        }
        return Files.list(dir).use { paths ->
            paths.filter { isGoldenFile(it) }.map { it.fileName.toString() }.toList().toSet()
        }
    }

    fun removeGolden(relativePath: String) {
        updateMode.requireAllowed()
        check(updateMode.enabled) { "골든 삭제는 갱신 모드(-Pcontract.update=true)에서만 합니다: $relativePath" }
        Files.deleteIfExists(resolve(goldenRoot, relativePath))
        markTouched(relativePath)
    }

    fun markTouched(relativePath: String) {
        touched.add(keyOf(relativePath))
    }

    fun touchedPaths(): Set<String> = touched.toSet()

    private fun compare(relativePath: String, actual: String): String? {
        val golden = resolve(goldenRoot, relativePath)
        if (Files.notExists(golden)) {
            return newContractMessage(relativePath)
        }
        val expected = Files.readAllBytes(golden)
        if (expected.contentEquals(actual.toByteArray(UTF_8))) {
            return null
        }
        return violationMessage(relativePath, String(expected, UTF_8), actual)
    }

    private fun violationMessage(relativePath: String, expected: String, actual: String): String {
        val expectedLines = expected.split("\n")
        val patch = DiffUtils.diff(expectedLines, actual.split("\n"))
        val diff = DiffUtils.generateUnifiedDiff("golden", "actual", expectedLines, patch, DIFF_CONTEXT)
        val header = listOf(
            "[API 계약 위반] $relativePath 와 실제 출력이 다릅니다.",
            "- 리팩터링(규약 보존) PR이면 운영 코드를 고쳐 골든과 같게 만드세요. 골든을 갱신하지 마세요.",
            "- 규약 변경이 승인된 PR(결정 번호 필요)만 로컬에서 $UPDATE_COMMAND 로 갱신합니다. CI에서는 갱신할 수 없습니다.",
            "- 골든: ${resolve(goldenRoot, relativePath)}",
            "- 실제 출력: ${resolve(actualRoot, relativePath)}"
        ) + platformHint()
        return (header + "--- 차이(앞 ${DIFF_LINE_LIMIT}줄) ---" + diff.take(DIFF_LINE_LIMIT)).joinToString("\n")
    }

    private fun newContractMessage(relativePath: String): String = listOf(
        "[새 계약 대상] $relativePath 골든이 없습니다.",
        "- 새 골든은 0단계 PR이나, 운영 코드를 바꾸기 전 커밋에서 develop 코드로 $UPDATE_COMMAND 를 돌려 만듭니다.",
        "- 실제 출력: ${resolve(actualRoot, relativePath)}"
    ).joinToString("\n")

    private fun platformHint(): List<String> =
        if (System.lineSeparator() == "\n") emptyList() else listOf(CRLF_PLATFORM_HINT)

    private fun resolve(root: Path, relativePath: String): Path {
        val path = root.resolve(relativePath).normalize()
        require(!Path.of(relativePath).isAbsolute && path.startsWith(root.normalize())) {
            "골든 경로는 계약 루트 아래 상대 경로여야 합니다: $relativePath"
        }
        return path
    }

    private fun keyOf(relativePath: String): String =
        goldenRoot.normalize().relativize(resolve(goldenRoot, relativePath)).joinToString("/")

    private fun write(path: Path, text: String) {
        Files.createDirectories(path.parent)
        Files.write(path, text.toByteArray(UTF_8))
    }

    companion object {
        val CONTRACT_ROOT: Path = Path.of("src/test/resources/contract")
        const val UPDATE_COMMAND = "./gradlew clean test -Pcontract.update=true"
        private const val DIFF_CONTEXT = 3
        private const val DIFF_LINE_LIMIT = 40
        private const val CRLF_PLATFORM_HINT =
            "- 이 JVM의 줄바꿈은 LF가 아닙니다. REST Docs는 요청 Content-Length를 플랫폼 줄바꿈으로 계산하므로 " +
                "스니펫 골든은 Linux·macOS·WSL에서 비교합니다."

        private val TOUCHED: MutableMap<Path, MutableSet<String>> = ConcurrentHashMap()

        val DEFAULT: ContractGolden = ContractGolden()

        fun isGoldenFile(path: Path): Boolean = Files.isRegularFile(path) && !path.fileName.toString().startsWith(".")

        private fun touchedUnder(root: Path): MutableSet<String> =
            TOUCHED.computeIfAbsent(root.toAbsolutePath().normalize()) { ConcurrentHashMap.newKeySet() }
    }
}
