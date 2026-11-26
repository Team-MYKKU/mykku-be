package com.example.mykku.contract.mvc

import com.example.mykku.contract.support.ContractViolationError
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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

        assertThat(config().properties()).containsExactlyEntriesOf(
            sortedMapOf(
                "server.servlet.session.cookie.secure" to "\${secret.cookie.secure}",
                "server.shutdown" to "graceful",
                "spring.mvc.servlet.path" to "/",
                "spring.servlet.multipart.max-file-size" to "10MB"
            )
        )
    }

    @Test
    fun `목록형 on-profile과 on-cloud-platform 문서는 dev에서 빼고 relaxed 키와 목록은 Boot처럼 합친다`() {
        write("application.yml", RELAXED_COMMON)
        write("application-dev.yml", RELAXED_DEV)

        assertThat(config().properties()).containsExactlyEntriesOf(
            sortedMapOf(
                "server.tomcat.relaxed-query-chars[0]" to "|",
                "spring.servlet.multipart.max-file-size" to "10MB"
            )
        )
    }

    @Test
    fun `git이 추적하지 않는 공통 파일은 병합하지 않는다`() {
        write("application.yml", "server:\n  shutdown: immediate\n")
        write("application-dev.yml", "spring:\n  mvc:\n    servlet:\n      path: /\n")
        val untrackedCommon = DevProfileHttpConfig(root) { it.fileName.toString() != "application.yml" }

        assertThat(untrackedCommon.properties()).containsExactlyEntriesOf(sortedMapOf("spring.mvc.servlet.path" to "/"))
    }

    @Test
    fun `포트·경로·비밀성 키는 화이트리스트 접두 안이라도 남기지 않는다`() {
        write("application-dev.yml", SENSITIVE)

        assertThat(config().properties()).containsExactlyEntriesOf(sortedMapOf("server.shutdown" to "graceful"))
    }

    @Test
    fun `spring profiles 키가 있으면 해석하지 않고 실패한다`() {
        write("application-dev.yml", "spring:\n  profiles:\n    group:\n      dev: extra\n")

        assertThatThrownBy { config().properties() }.isInstanceOf(ContractViolationError::class.java)
    }

    @Test
    fun `지원하지 않는 설정 파일이 생기면 알린다`() {
        write("application.properties", "server.shutdown=immediate\n")
        val unsupported = config().unsupportedSources()

        assertThat(unsupported).containsExactly(root.resolve("application.properties"))
    }

    private fun config(): DevProfileHttpConfig = DevProfileHttpConfig(root) { true }

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

        val RELAXED_COMMON = """
            spring:
              servlet:
                multipart:
                  maxFileSize: 5MB
            server:
              tomcat:
                relaxed-query-chars: ["[", "]"]
            ---
            spring:
              config:
                activate:
                  on-profile: [local, test]
            server:
              shutdown: local-only
            ---
            spring:
              config:
                activate:
                  on-cloud-platform: kubernetes
            server:
              shutdown: cloud-only
        """.trimIndent()

        val RELAXED_DEV = """
            spring:
              servlet:
                multipart:
                  max_file_size: 10MB
            server:
              tomcat:
                relaxedQueryChars: ["|"]
        """.trimIndent()

        val SENSITIVE = """
            server:
              port: 8080
              shutdown: graceful
              ssl:
                key-store: /etc/key.p12
              tomcat:
                basedir: /tmp/tomcat
              servlet:
                session:
                  cookie:
                    secret-token: abc
        """.trimIndent()
    }
}
