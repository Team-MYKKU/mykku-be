package com.example.mykku.contract.error

import com.example.mykku.auth.exception.AuthException
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import com.fasterxml.jackson.databind.type.TypeFactory
import jakarta.validation.ConstraintViolationException
import org.redisson.client.RedisException
import org.springframework.core.MethodParameter
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.dao.QueryTimeoutException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.MultipartException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException

object FrameworkExceptions {

    private const val PROBE = "contract-probe"
    private const val PROBE_PATH = "/api/v1/contract-probe"
    private const val OBJECT_NAME = "request"
    private const val MAX_UPLOAD_SIZE = 10_485_760L

    fun cases(): List<Pair<String, Exception>> =
        bindingErrors() + routingErrors() + multipartErrors() + dataErrors() + unexpectedErrors()

    private fun bindingErrors(): List<Pair<String, Exception>> = listOf(
        "http-message-not-readable" to HttpMessageNotReadableException(PROBE, emptyInput()),
        "http-message-not-readable<-value-instantiation<-auth-exception" to authCauseChain(),
        "method-argument-not-valid(field)" to argumentNotValid(FieldError(OBJECT_NAME, "memberId", "필드 메시지")),
        "method-argument-not-valid(field-without-message)" to argumentNotValid(fieldErrorWithoutMessage()),
        "method-argument-not-valid(global-only)" to argumentNotValid(ObjectError(OBJECT_NAME, "전역 메시지")),
        "missing-servlet-request-parameter" to MissingServletRequestParameterException("date", "LocalDate"),
        "missing-servlet-request-part" to MissingServletRequestPartException(OBJECT_NAME),
        "method-argument-type-mismatch" to typeMismatch()
    )

    private fun routingErrors(): List<Pair<String, Exception>> = listOf(
        "no-resource-found" to NoResourceFoundException(HttpMethod.GET, PROBE_PATH),
        "no-handler-found" to NoHandlerFoundException("GET", PROBE_PATH, HttpHeaders()),
        "http-request-method-not-supported" to HttpRequestMethodNotSupportedException("PUT", listOf("PATCH")),
        "http-media-type-not-supported" to
            HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, listOf(MediaType.APPLICATION_JSON)),
        "http-media-type-not-acceptable" to HttpMediaTypeNotAcceptableException(listOf(MediaType.APPLICATION_JSON))
    )

    private fun multipartErrors(): List<Pair<String, Exception>> = listOf(
        "max-upload-size-exceeded" to MaxUploadSizeExceededException(MAX_UPLOAD_SIZE),
        "multipart" to MultipartException(PROBE)
    )

    private fun dataErrors(): List<Pair<String, Exception>> = listOf(
        "data-integrity-violation" to DataIntegrityViolationException(PROBE),
        "pessimistic-locking-failure" to PessimisticLockingFailureException(PROBE),
        "query-timeout" to QueryTimeoutException(PROBE),
        "object-optimistic-locking-failure" to ObjectOptimisticLockingFailureException(Any::class.java, 1L),
        "redis" to RedisException(PROBE)
    )

    private fun unexpectedErrors(): List<Pair<String, Exception>> = listOf(
        "runtime" to RuntimeException(PROBE),
        "illegal-argument" to IllegalArgumentException(PROBE),
        "illegal-state" to IllegalStateException(PROBE),
        "null-pointer" to NullPointerException(PROBE),
        "constraint-violation" to ConstraintViolationException(PROBE, emptySet())
    )

    private fun emptyInput(): MockHttpInputMessage = MockHttpInputMessage(ByteArray(0))

    private fun authCauseChain(): HttpMessageNotReadableException {
        val type = TypeFactory.defaultInstance().constructType(Any::class.java)
        val instantiation = ValueInstantiationException.from(null, PROBE, type, AuthException.mobileLoginNotSupported())
        return HttpMessageNotReadableException(PROBE, instantiation, emptyInput())
    }

    private fun argumentNotValid(error: ObjectError): MethodArgumentNotValidException {
        val bindingResult = BeanPropertyBindingResult(Any(), OBJECT_NAME).apply { addError(error) }
        return MethodArgumentNotValidException(probeParameter(0), bindingResult)
    }

    private fun fieldErrorWithoutMessage(): FieldError =
        FieldError(OBJECT_NAME, "memberId", null, false, null, null, null)

    private fun typeMismatch(): MethodArgumentTypeMismatchException {
        val cause = NumberFormatException(PROBE)
        return MethodArgumentTypeMismatchException("abc", Long::class.java, "feedId", probeParameter(1), cause)
    }

    private fun probeParameter(index: Int): MethodParameter =
        MethodParameter(ExceptionProbeTarget::class.java.methods.single { it.name == "probe" }, index)
}

class ExceptionProbeTarget {
    @Suppress("UNUSED_PARAMETER")
    fun probe(request: String, feedId: Long) = Unit
}
