package uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions

import org.springframework.http.HttpStatus

class ApiException(override val message: String, val status: HttpStatus) : RuntimeException(message)
