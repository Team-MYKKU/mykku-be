package com.example.mykku.contract.mvc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

@DisplayName("dev 프로필 설정 덤프")
class DevProfileHttpConfigTest {

    @TempDir
    private lateinit var root: Path

    @Test
    fun `공통 파일 위에 dev 파일을 덮고 다른 프로필 문서·import·화이트리스트 밖 키는 버린다`() {
        write("application.yml", COMMON)
        write("application-dev.yml", DEV)
        write("dev-secret.yml", "server:\n  server-header: secret-value\n")

        assertThat(DevProfileHttpConfig(root).properties()).containsExactlyEntriesOf(
            sortedMapOf(
                "server.servlet.session.cookie.secure" to "\${secret.cookie.secure}",
                "server.shutdown" to "graceful",
                "spring.mvc.servlet.path" to "/",
                "spring.servlet.multipart.max-file-size" to "10MB"
            )
        )
    }

    @Test
    fun `지원하지 않는 설정 파일이 생기면 알린다`() {
        write("application.properties", "server.shutdown=immediate\n")
        val unsupported = DevProfileHttpConfig(root).unsupportedSources()

        assertThat(unsupported).containsExactly(root.resolve("application.properties"))
    }

    private fun write(name: String, text: String) {
        Files.writeString(root.resolve(name), text)
    }

    private companion object {
        val COMMON = """
            server:
              shutdown: immediate
            spring:
              mvc:
                servlet:
                  path: /
            ---
            spring:
              config:
                activate:
                  on-profile: test
            server:
              shutdown: test-only
        """.trimIndent()

        val DEV = """
            spring:
              config:
                import: dev-secret.yml
              servlet:
                multipart:
                  max-file-size: 10MB
            server:
              shutdown: graceful
              servlet:
                session:
                  cookie:
                    secure: ${'$'}{secret.cookie.secure}
            jwt:
              secret: ${'$'}{secret.jwt}
        """.trimIndent()
    }
}
