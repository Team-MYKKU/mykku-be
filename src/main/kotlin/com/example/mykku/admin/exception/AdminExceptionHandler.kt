package com.example.mykku.admin.exception

import com.example.mykku.common.exception.ErrorResponse
import com.example.mykku.common.exception.ExceptionLoggingSupport
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(basePackages = ["com.example.mykku.admin"])
@Order(Ordered.HIGHEST_PRECEDENCE)
class AdminExceptionHandler {

    private val logger = LoggerFactory.getLogger(AdminExceptionHandler::class.java)

    @ExceptionHandler(AdminException::class)
    fun handleAdminException(e: AdminException): ResponseEntity<ErrorResponse> {
        ExceptionLoggingSupport.logException(logger, e)

        return ResponseEntity
            .status(e.errorCode.status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ErrorResponse(e.errorCode.code, e.errorCode.message))
    }
}
