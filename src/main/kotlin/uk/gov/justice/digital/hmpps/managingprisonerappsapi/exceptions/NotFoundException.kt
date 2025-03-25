package uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions

import jakarta.validation.ValidationException
import java.util.function.Supplier

class NotFoundException(message: String? = null, cause: Throwable? = null) :
  ValidationException(message, cause),
  Supplier<NotFoundException> {
  override fun get(): NotFoundException = NotFoundException(message, cause)
}
