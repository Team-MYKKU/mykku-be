package com.example.mykku.contract.support

import org.assertj.core.util.diff.DiffUtils
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path

class ContractGolden(
    private val goldenRoot: Path = Path.of("src/test/resources/contract"),
    private val actualRoot: Path = Path.of("build/contract-actual"),
    private val updateMode: ContractUpdateMode = ContractUpdateMode.fromEnvironment()
) {

    val updating: Boolean
        get() = updateMode.enabled

    fun verify(relativePath: String, actual: String) {
        violationOf(relativePath, actual)?.let { throw AssertionError(it) }
    }

    fun violationOf(relativePath: String, actual: String): String? {
        updateMode.requireAllowed()
        val normalized = actual.replace("\r\n", "\n")
        write(resolve(actualRoot, relativePath), normalized)
        if (updateMode.enabled) {
            write(resolve(goldenRoot, relativePath), normalized)
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
            paths.filter { Files.isRegularFile(it) }.map { it.fileName.toString() }.toList().toSet()
        }
    }

    fun removeGolden(relativePath: String) {
        updateMode.requireAllowed()
        require(updateMode.enabled) { "골든 삭제는 갱신 모드(-Pcontract.update=true)에서만 합니다: $relativePath" }
        Files.deleteIfExists(resolve(goldenRoot, relativePath))
    }

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
        return listOf(
            "[API 계약 위반] $relativePath 와 실제 출력이 다릅니다.",
            "- 리팩터링(규약 보존) PR이면 운영 코드를 고쳐 골든과 같게 만드세요. 골든을 갱신하지 마세요.",
            "- 규약 변경이 승인된 PR(결정 번호 필요)만 로컬에서 -Pcontract.update=true 로 갱신합니다. " +
                "CI에서는 갱신할 수 없습니다.",
            "- 골든: ${resolve(goldenRoot, relativePath)}",
            "- 실제 출력: ${resolve(actualRoot, relativePath)}",
            "--- 차이(앞 ${DIFF_LINE_LIMIT}줄) ---"
        ).plus(diff.take(DIFF_LINE_LIMIT)).joinToString("\n")
    }

    private fun newContractMessage(relativePath: String): String = listOf(
        "[새 계약 대상] $relativePath 골든이 없습니다.",
        "- 새 골든은 0단계 PR이나, 운영 코드를 바꾸기 전 커밋에서 develop 코드로 " +
            "-Pcontract.update=true 를 돌려 만듭니다.",
        "- 실제 출력: ${resolve(actualRoot, relativePath)}"
    ).joinToString("\n")

    private fun resolve(root: Path, relativePath: String): Path {
        val path = root.resolve(relativePath).normalize()
        require(!Path.of(relativePath).isAbsolute && path.startsWith(root.normalize())) {
            "골든 경로는 계약 루트 아래 상대 경로여야 합니다: $relativePath"
        }
        return path
    }

    private fun write(path: Path, text: String) {
        Files.createDirectories(path.parent)
        Files.write(path, text.toByteArray(UTF_8))
    }

    companion object {
        private const val DIFF_CONTEXT = 3
        private const val DIFF_LINE_LIMIT = 40

        val DEFAULT: ContractGolden = ContractGolden()
    }
}
