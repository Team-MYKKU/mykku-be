package com.example.mykku.contract.mvc

import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.EnumerablePropertySource
import org.springframework.core.env.Profiles
import org.springframework.core.io.FileSystemResource
import java.nio.file.Files
import java.nio.file.Path

class DevProfileHttpConfig(private val resourceRoot: Path = Path.of("src/main/resources")) {

    fun properties(): Map<String, String> {
        val merged = sortedMapOf<String, String>()
        val documents = existingSources().flatMap { documents(it) }.filter { activeForDev(it) }
        documents.forEach { merged.putAll(whitelisted(it)) }
        return merged
    }

    fun render(): String = properties().entries.joinToString("") { "${it.key}=${it.value}\n" }

    fun existingSources(): List<Path> = SOURCES.map { resourceRoot.resolve(it) }.filter { Files.isRegularFile(it) }

    fun unsupportedSources(): List<Path> =
        UNSUPPORTED_SOURCES.map { resourceRoot.resolve(it) }.filter { Files.exists(it) }

    private fun documents(file: Path): List<EnumerablePropertySource<*>> =
        YamlPropertySourceLoader().load(file.fileName.toString(), FileSystemResource(file))
            .map { it as EnumerablePropertySource<*> }

    private fun activeForDev(document: EnumerablePropertySource<*>): Boolean {
        val expression = document.getProperty(ACTIVATION_KEY)?.toString() ?: return true
        val profiles = Profiles.of(*expression.split(',').map { it.trim() }.toTypedArray())
        return profiles.matches { it == DEV_PROFILE }
    }

    private fun whitelisted(document: EnumerablePropertySource<*>): Map<String, String> = document.propertyNames
        .filter { name -> PREFIXES.any { name.startsWith(it) } }
        .associateWith { document.getProperty(it).toString() }

    companion object {
        val SOURCES = listOf("application.yml", "application-dev.yml")
        val PREFIXES = listOf(
            "server.",
            "spring.jackson.",
            "spring.mvc.",
            "spring.web.",
            "spring.servlet.",
            "spring.lifecycle."
        )
        private const val ACTIVATION_KEY = "spring.config.activate.on-profile"
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
