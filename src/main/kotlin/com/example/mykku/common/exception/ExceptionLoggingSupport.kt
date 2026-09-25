package com.example.mykku.common.exception

import org.slf4j.Logger
import org.slf4j.MDC
import org.springframework.http.HttpStatus

object ExceptionLoggingSupport {

    private const val REQUEST_ID_KEY = "req-id"

    fun logException(logger: Logger, exception: Exception) {
        logException(logger, exception, (exception as? BaseException)?.errorCode?.status)
    }

    fun logException(logger: Logger, exception: Exception, status: HttpStatus?) {
        val message = describe(exception)
        if (status?.is4xxClientError == true) {
            logger.warn(message)
            return
        }
        logger.error(message, exception)
    }

    private fun describe(exception: Exception): String {
        val requestId = MDC.get(REQUEST_ID_KEY) ?: "unknown"
        val code = (exception as? BaseException)?.errorCode?.code?.let { "[$it]" } ?: ""
        return "[$requestId] ${exception::class.simpleName}$code: ${exception.message}"
    }
}
