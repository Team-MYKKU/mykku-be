package com.example.mykku.common.exception

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.example.mykku.auth.exception.AuthErrorCode
import com.example.mykku.auth.exception.AuthException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.servlet.resource.NoResourceFoundException

@DisplayName("ExceptionLoggingSupport 단위 테스트")
class ExceptionLoggingSupportTest {

    private val logger = LoggerFactory.getLogger(ExceptionLoggingSupportTest::class.java) as Logger
    private val appender = ListAppender<ILoggingEvent>()

    @BeforeEach
    fun setUp() {
        appender.start()
        logger.addAppender(appender)
        MDC.put("req-id", "abcd1234")
    }

    @AfterEach
    fun tearDown() {
        logger.detachAppender(appender)
        MDC.clear()
    }

    @Test
    fun `4xx 도메인 예외는 WARN 레벨로 스택트레이스 없이 에러 코드와 함께 기록한다`() {
        val errorCode = AuthErrorCode.INVALID_TOKEN

        ExceptionLoggingSupport.logException(logger, AuthException.invalidToken())

        val event = appender.list.single()
        assertThat(event.level).isEqualTo(Level.WARN)
        assertThat(event.throwableProxy).isNull()
        assertThat(event.formattedMessage)
            .isEqualTo("[abcd1234] AuthException[${errorCode.code}]: ${errorCode.message}")
    }

    @Test
    fun `5xx 도메인 예외는 ERROR 레벨로 스택트레이스와 에러 코드와 함께 기록한다`() {
        val errorCode = AuthErrorCode.OAUTH_SERVER_ERROR

        ExceptionLoggingSupport.logException(logger, AuthException.oauthServerError())

        val event = appender.list.single()
        assertThat(event.level).isEqualTo(Level.ERROR)
        assertThat(event.throwableProxy).isNotNull()
        assertThat(event.formattedMessage)
            .isEqualTo("[abcd1234] AuthException[${errorCode.code}]: ${errorCode.message}")
    }

    @Test
    fun `도메인 예외가 아닌 예외는 ERROR 레벨로 스택트레이스와 함께 기록한다`() {
        ExceptionLoggingSupport.logException(logger, IllegalStateException("boom"))

        val event = appender.list.single()
        assertThat(event.level).isEqualTo(Level.ERROR)
        assertThat(event.throwableProxy).isNotNull()
        assertThat(event.formattedMessage).isEqualTo("[abcd1234] IllegalStateException: boom")
    }

    @Test
    fun `4xx 상태를 지정한 프레임워크 예외는 WARN 레벨로 스택트레이스 없이 기록한다`() {
        val exception = NoResourceFoundException(HttpMethod.GET, ".env")

        ExceptionLoggingSupport.logException(logger, exception, HttpStatus.NOT_FOUND)

        val event = appender.list.single()
        assertThat(event.level).isEqualTo(Level.WARN)
        assertThat(event.throwableProxy).isNull()
        assertThat(event.formattedMessage).isEqualTo("[abcd1234] NoResourceFoundException: No static resource .env.")
    }

    @Test
    fun `5xx 상태를 지정한 예외는 ERROR 레벨로 스택트레이스와 함께 기록한다`() {
        ExceptionLoggingSupport.logException(logger, IllegalStateException("boom"), HttpStatus.INTERNAL_SERVER_ERROR)

        val event = appender.list.single()
        assertThat(event.level).isEqualTo(Level.ERROR)
        assertThat(event.throwableProxy).isNotNull()
    }
}
