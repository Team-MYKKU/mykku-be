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
    fun `공통 파일 위에 dev 파일을 덮고 다른 프로필 문서·import·다른 설정 파일·화이트리스트 밖 키는 버린다`() {
        write("application.yml", COMMON)
        write("application-dev.yml", DEV)
        write("dev-secret.yml", "server:\n  server-header: secret-value\n")
        write("application-local.yml", "server:\n  server-header: local-value\n")

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
    fun `목록형 on-profile과 다른 클라우드 플랫폼 문서는 빼고 none 문서는 넣으며 relaxed 키와 목록은 Boot처럼 합친다`() {
        write("application.yml", RELAXED_COMMON)
        write("application-dev.yml", RELAXED_DEV)

        assertThat(config().properties()).containsExactlyEntriesOf(
            sortedMapOf(
                "server.servlet.session.timeout" to "10m",
                "server.tomcat.relaxed-path-chars" to "|",
                "server.tomcat.relaxed-query-chars[0]" to "|",
                "spring.servlet.multipart.maxfilesize" to "10MB"
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
    fun `실행 인자가 덮는 포트·운영 경로·비밀성 키는 relaxed 철자여도 빼고 다른 키는 남긴다`() {
        write("application-dev.yml", SENSITIVE)

        assertThat(config().properties()).containsExactlyEntriesOf(
            sortedMapOf(
                "server.address" to "0.0.0.0",
                "server.shutdown" to "graceful",
                "server.ssl.client-auth" to "none",
                "server.ssl.enabled" to "false",
                "spring.jackson.deserialization.fail-on-trailing-tokens" to "true"
            )
        )
    }

    @Test
    fun `대괄호 맵 키도 대괄호 밖 요소처럼 맞춰 안전 키는 남기고 대문자·스네이크·카멜 비밀 낱말은 뺀다`() {
        write("application-dev.yml", BRACKETED)

        assertThat(config().properties()).containsExactlyEntriesOf(
            sortedMapOf(
                "spring.jackson.deserialization[FAIL_ON_TRAILING_TOKENS]" to "false",
                "spring.jackson.deserialization[fail-on-trailing-tokens]" to "true"
            )
        )
    }

    @Test
    fun `dev 파일에 spring profiles 키가 있으면 해석하지 않고 그 키를 알리며 실패한다`() {
        write("application-dev.yml", "spring:\n  profiles:\n    group:\n      dev: extra\n")

        assertThatThrownBy { config().properties() }
            .isInstanceOf(ContractViolationError::class.java)
            .hasMessageContaining("spring.profiles.group.dev")
    }

    @Test
    fun `대시 없는 relaxed 철자의 cloud-platform 지정 키도 해석하지 않고 실패한다`() {
        write("application.yml", "spring:\n  main:\n    cloudplatform: kubernetes\n")

        assertThatThrownBy { config().properties() }
            .isInstanceOf(ContractViolationError::class.java)
            .hasMessageContaining("spring.main.cloudplatform")
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
                  max-file-size: 5MB
            server:
              tomcat:
                relaxed-query-chars: ["[", "]"]
                relaxed-path-chars: ["{", "}"]
            ---
            spring:
              config:
                activate:
                  on-cloud-platform: none
            server:
              servlet:
                session:
                  timeout: 10m
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
                  oncloudplatform: kubernetes
            server:
              shutdown: cloud-only
        """.trimIndent()

        val RELAXED_DEV = """
            spring:
              servlet:
                multipart:
                  maxfilesize: 10MB
            server:
              tomcat:
                relaxedQueryChars: ["|"]
                relaxed_path_chars: "|"
        """.trimIndent()

        val BRACKETED = """
            spring:
              jackson:
                deserialization:
                  "[fail-on-trailing-tokens]": true
                  "[FAIL_ON_TRAILING_TOKENS]": false
            server:
              servlet:
                context-parameters:
                  "[API_TOKEN]": abc
                  "[PRIVATE_KEY]": abc
                  "[keyStore]": abc
        """.trimIndent()

        val SENSITIVE = """
            server:
              port: 8080
              address: 0.0.0.0
              shutdown: graceful
              ssl:
                enabled: false
                client-auth: none
                key-store: /etc/key.p12
                key-store-password: abc
                keystorepassword: abc
                truststore: /etc/trust.p12
                certificate: /etc/cert.pem
                trustCertificate: /etc/ca.pem
              tomcat:
                baseDir: /tmp/tomcat
              servlet:
                session:
                  cookie:
                    secret-token: abc
            spring:
              jackson:
                deserialization:
                  fail-on-trailing-tokens: true
        """.trimIndent()
    }
}
