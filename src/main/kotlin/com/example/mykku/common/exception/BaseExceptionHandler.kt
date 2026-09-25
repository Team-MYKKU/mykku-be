package com.example.mykku.common.exception

import org.redisson.client.RedisException
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.dao.QueryTimeoutException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.MultipartException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
class BaseExceptionHandler {

    private val logger = LoggerFactory.getLogger(BaseExceptionHandler::class.java)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(
        exception: MethodArgumentNotValidException
    ): ResponseEntity<ErrorResponse> {
        ExceptionLoggingSupport.logException(logger, exception, CommonErrorCode.INVALID_INPUT.status)

        val errorMessage = exception.bindingResult.fieldErrors
            .joinToString(", ") { it.defaultMessage ?: CommonErrorCode.INVALID_INPUT.message }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ErrorResponse(CommonErrorCode.INVALID_INPUT.code, errorMessage))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        exception: HttpMessageNotReadableException
    ): ResponseEntity<ErrorResponse> {
        return handle(exception, CommonErrorCode.INVALID_INPUT)
    }

    @ExceptionHandler(BaseException::class)
    fun handleBaseException(exception: BaseException): ResponseEntity<ErrorResponse> {
        ExceptionLoggingSupport.logException(logger, exception)

        return ResponseEntity
            .status(exception.errorCode.status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ErrorResponse(exception.errorCode.code, exception.errorCode.message))
    }

    @ExceptionHandler(NoResourceFoundException::class, NoHandlerFoundException::class)
    fun handleEndpointNotFoundException(exception: Exception): ResponseEntity<ErrorResponse> {
        return handle(exception, CommonErrorCode.ENDPOINT_NOT_FOUND)
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotAllowedException(
        exception: HttpRequestMethodNotSupportedException
    ): ResponseEntity<ErrorResponse> {
        return handle(exception, CommonErrorCode.METHOD_NOT_ALLOWED)
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleUnsupportedMediaTypeException(
        exception: HttpMediaTypeNotSupportedException
    ): ResponseEntity<ErrorResponse> {
        return handle(exception, CommonErrorCode.UNSUPPORTED_MEDIA_TYPE)
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException::class)
    fun handleNotAcceptableException(
        exception: HttpMediaTypeNotAcceptableException
    ): ResponseEntity<ErrorResponse> {
        return handle(exception, CommonErrorCode.NOT_ACCEPTABLE)
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSizeExceededException(
        exception: MaxUploadSizeExceededException
    ): ResponseEntity<ErrorResponse> {
        return handle(exception, CommonErrorCode.PAYLOAD_TOO_LARGE)
    }

    @ExceptionHandler(MultipartException::class)
    fun handleMultipartException(exception: MultipartException): ResponseEntity<ErrorResponse> {
        return handle(exception, CommonErrorCode.INVALID_MULTIPART_REQUEST)
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(
        exception: DataIntegrityViolationException
    ): ResponseEntity<ErrorResponse> {
        return handle(exception, CommonErrorCode.DATA_INTEGRITY_VIOLATION)
    }

    @ExceptionHandler(
        MissingServletRequestParameterException::class,
        MissingServletRequestPartException::class
    )
    fun handleMissingRequestValueException(exception: Exception): ResponseEntity<ErrorResponse> {
        return handle(exception, CommonErrorCode.MISSING_REQUEST_PARAMETER)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleParameterTypeMismatchException(
        exception: MethodArgumentTypeMismatchException
    ): ResponseEntity<ErrorResponse> {
        return handle(exception, CommonErrorCode.INVALID_PARAMETER_TYPE)
    }

    @ExceptionHandler(PessimisticLockingFailureException::class, QueryTimeoutException::class)
    fun handleLockConflictException(exception: Exception): ResponseEntity<ErrorResponse> {
        return handle(exception, CommonErrorCode.RESOURCE_LOCK_CONFLICT)
    }

    @ExceptionHandler(RedisException::class)
    fun handleRedisException(exception: RedisException): ResponseEntity<ErrorResponse> {
        return handle(exception, CommonErrorCode.REDIS_CONNECTION_FAILURE)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ResponseEntity<ErrorResponse> {
        return handle(exception, CommonErrorCode.INTERNAL_SERVER_ERROR)
    }

    private fun handle(exception: Exception, errorCode: CommonErrorCode): ResponseEntity<ErrorResponse> {
        ExceptionLoggingSupport.logException(logger, exception, errorCode.status)

        return errorResponseOf(errorCode)
    }

    private fun errorResponseOf(errorCode: CommonErrorCode): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(errorCode.status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ErrorResponse(errorCode.code, errorCode.message))
    }
}
