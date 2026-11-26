package com.example.mykku.docs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestInstances
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.opentest4j.TestAbortedException
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.Optional

@DisplayName("요청·응답 로그 실패 시 출력 확장")
class RequestResponseLogOnFailureExtensionTest {

    private val output = ByteArrayOutputStream()

    private val out = PrintStream(output, true, Charsets.UTF_8)

    private val extension = RequestResponseLogOnFailureExtension { out }

    @Test
    fun `테스트가 실패하면 바깥 인스턴스의 요청·응답 로그를 한 번 출력한다`() {
        extension.testFailed(context(FixedLogSource("요청·응답 로그")), AssertionError("상태 코드 불일치"))

        assertThat(output.toString(Charsets.UTF_8)).isEqualTo("요청·응답 로그")
    }

    @Test
    fun `테스트가 성공하거나 중단되면 출력하지 않는다`() {
        val context = context(FixedLogSource("요청·응답 로그"))

        extension.testSuccessful(context)
        extension.testAborted(context, TestAbortedException("가정 불충족"))

        assertThat(output.toString(Charsets.UTF_8)).isEmpty()
    }

    @Test
    fun `테스트 인스턴스가 없으면 출력하지 않는다`() {
        val context = mock<ExtensionContext> { on { testInstances } doReturn Optional.empty() }

        extension.testFailed(context, IllegalStateException("테스트 클래스 생성 실패"))

        assertThat(output.toString(Charsets.UTF_8)).isEmpty()
    }

    private fun context(source: RequestResponseLogSource): ExtensionContext {
        val instances = mock<TestInstances> { on { allInstances } doReturn listOf(source, Any()) }
        return mock { on { testInstances } doReturn Optional.of(instances) }
    }

    private class FixedLogSource(private val log: String) : RequestResponseLogSource {
        override fun requestResponseLog(): String = log
    }
}
