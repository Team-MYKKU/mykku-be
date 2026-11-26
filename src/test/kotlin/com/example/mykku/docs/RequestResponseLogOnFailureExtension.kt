package com.example.mykku.docs

import io.restassured.filter.Filter
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestWatcher
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.jvm.optionals.getOrNull

class RequestResponseLogBuffer {

    private val buffer = ByteArrayOutputStream()
    private val stream = PrintStream(buffer, true, Charsets.UTF_8)

    fun filters(): List<Filter> = listOf(RequestLoggingFilter(stream), ResponseLoggingFilter(stream))

    fun content(): String = buffer.toString(Charsets.UTF_8)

    fun reset() = buffer.reset()
}

interface RequestResponseLogSource {
    fun requestResponseLog(): String
}

class RequestResponseLogOnFailureExtension(
    private val out: () -> PrintStream = { System.out }
) : TestWatcher {

    override fun testFailed(context: ExtensionContext, cause: Throwable?) {
        context.testInstances.getOrNull()?.allInstances.orEmpty()
            .filterIsInstance<RequestResponseLogSource>()
            .forEach { out().print(it.requestResponseLog()) }
    }
}
