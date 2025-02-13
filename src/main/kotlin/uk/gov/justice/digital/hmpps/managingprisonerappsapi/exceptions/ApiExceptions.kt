package uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions

import org.springframework.http.HttpStatus

class ApiExceptions(override val message: String, status: HttpStatus): RuntimeException(message) {
}