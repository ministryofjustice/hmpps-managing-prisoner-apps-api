package uk.gov.justice.digital.hmpps.managingprisonerappsapi.config

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestControllerAdvice
class ManagingPrisonerAppsApiExceptionHandler {
  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: ValidationException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Validation exception: {}", e.message) }

  @ExceptionHandler(NoResourceFoundException::class)
  fun handleNoResourceFoundException(e: NoResourceFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND,
        userMessage = "No resource found failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("No resource found exception: {}", e.message) }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(FORBIDDEN)
    .body(
      ErrorResponse(
        status = FORBIDDEN,
        userMessage = "Forbidden: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.debug("Forbidden (403) returned: {}", e.message) }

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(INTERNAL_SERVER_ERROR)
    .body(
      ErrorResponse(
        status = INTERNAL_SERVER_ERROR,
        userMessage = "Unexpected error: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.error("Unexpected exception: {}", e.message) }

  @ExceptionHandler(ApiException::class)
  fun handleApiException(e: ApiException): ResponseEntity<ErrorResponse> {
    log.error("Api Exception: {}", e.message)
    return ResponseEntity
      .status(e.status)
      .body(
        ErrorResponse(
          status = e.status.value(),
          userMessage = "Unexpected error: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(MissingRequestHeaderException::class)
  fun handleApiMissingRequestHeader(e: MissingRequestHeaderException): ResponseEntity<ErrorResponse> {
    log.error("Exception due to missing required header in api request: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST.value())
      .body(
        ErrorResponse(
          status = HttpStatus.BAD_REQUEST.value(),
          userMessage = "Unexpected error: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
  fun handleApiMediaTypeNotSupported(e: HttpMediaTypeNotSupportedException): ResponseEntity<ErrorResponse> {
    log.error("Exception due to invalid or missing media type header in api request: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST.value())
      .body(
        ErrorResponse(
          status = HttpStatus.BAD_REQUEST.value(),
          userMessage = "Unexpected error: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(MissingServletRequestParameterException::class)
  fun handleQueryParamValidationException(e: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> {
    log.error("MissingServletRequestParameterException due to invalid request param {}", e.message)
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST.value())
      .body(
        ErrorResponse(
          status = HttpStatus.BAD_REQUEST.value(),
          userMessage = "Unexpected error: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
  fun handleHttpMethodNotSupported(e: HttpRequestMethodNotSupportedException): ResponseEntity<ErrorResponse> {
    log.error("HttpRequestMethodNotSupportedException due to invalid HTTP method {}", e.message)
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST.value())
      .body(
        ErrorResponse(
          status = HttpStatus.BAD_REQUEST.value(),
          userMessage = "Unexpected error: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
