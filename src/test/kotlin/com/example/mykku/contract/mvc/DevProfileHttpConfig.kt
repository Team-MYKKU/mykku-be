package com.example.mykku.contract.mvc

import com.example.mykku.contract.support.ContractViolationError
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.EnumerablePropertySource
import org.springframework.core.env.Profiles
import org.springframework.core.io.FileSystemResource
import java.nio.file.Files
import java.nio.file.Path

class DevProfileHttpConfig(
    private val resourceRoot: Path = Path.of("src/main/resources"),
    private val tracked: (Path) -> Boolean = { GitFiles.isTracked(it) }
) {

    fun properties(): Map<String, String> {
        val merged = sortedMapOf<String, String>()
        activeDocuments().forEach { merge(merged, whitelisted(it)) }
        return merged
    }

    fun render(): String = properties().entries.joinToString("") { "${it.key}=${it.value}\n" }

    fun mergedSources(): List<Path> =
        SOURCES.map { resourceRoot.resolve(it) }.filter { Files.isRegularFile(it) && tracked(it) }

    fun unsupportedSources(): List<Path> =
        UNSUPPORTED_SOURCES.map { resourceRoot.resolve(it) }.filter { Files.exists(it) }

    private fun activeDocuments(): List<Map<String, String>> = mergedSources().flatMap { documents(it) }
        .filter { activeForDev(it) }
        .onEach { requireNoProfileKeys(it) }

    private fun documents(file: Path): List<Map<String, String>> =
        YamlPropertySourceLoader().load(file.fileName.toString(), FileSystemResource(file))
            .map { it as EnumerablePropertySource<*> }
            .map { source -> source.propertyNames.associate { canonicalEntry(source, it) } }

    private fun canonicalEntry(source: EnumerablePropertySource<*>, name: String): Pair<String, String> =
        ConfigKeys.canonical(name) to "${source.getProperty(name)}"

    private fun activeForDev(document: Map<String, String>): Boolean {
        val platform = document.entries.firstOrNull { ConfigKeys.isUnderRoot(it.key, CLOUD_PLATFORM_KEY) }?.value
        if (platform != null && !platform.equals(NO_CLOUD_PLATFORM, ignoreCase = true)) {
            return false
        }
        val expressions = document.filterKeys { ConfigKeys.isUnderRoot(it, PROFILE_KEY) }.values
        if (expressions.isEmpty()) {
            return true
        }
        val profiles = expressions.flatMap { it.split(',') }.map { it.trim() }
        return Profiles.of(*profiles.toTypedArray()).matches { it == DEV_PROFILE }
    }

    private fun requireNoProfileKeys(document: Map<String, String>) {
        val keys = document.keys.filter { key -> UNSUPPORTED_ROOTS.any { ConfigKeys.isAtOrBelow(key, it) } }
        if (keys.isNotEmpty()) {
            throw ContractViolationError("[API 계약] dev 프로필 덤프는 프로필·클라우드 플랫폼 지정 키를 해석하지 않습니다: $keys")
        }
    }

    private fun merge(target: MutableMap<String, String>, document: Map<String, String>) {
        val roots = document.keys.map { ConfigKeys.listRoot(it) }.toSet()
        target.keys.removeIf { key -> roots.any { ConfigKeys.isUnderRoot(key, it) } }
        target.putAll(document)
    }

    private fun whitelisted(document: Map<String, String>): Map<String, String> =
        document.filterKeys { key -> PREFIXES.any { ConfigKeys.hasPrefix(key, it) } && !ConfigKeys.isExcluded(key) }

    companion object {
        val SOURCES = listOf("application.yml", "application-dev.yml")
        val PREFIXES = listOf(
            "server.",
            "spring.jackson.",
            "spring.mvc.",
            "spring.web.",
            "spring.servlet.",
            "spring.lifecycle.",
            "spring.data.web."
        )
        private const val PROFILE_KEY = "spring.config.activate.on-profile"
        private const val CLOUD_PLATFORM_KEY = "spring.config.activate.on-cloud-platform"
        private const val NO_CLOUD_PLATFORM = "none"
        private val UNSUPPORTED_ROOTS = listOf("spring.profiles", "spring.main.cloud-platform")
        private const val DEV_PROFILE = "dev"
        private val UNSUPPORTED_SOURCES = listOf(
            "application.yaml",
            "application.properties",
            "application-dev.yaml",
            "application-dev.properties",
            "config"
        )
    }
}
