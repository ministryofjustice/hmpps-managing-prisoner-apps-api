package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType

interface AppTypeFormValidation {
  fun validateFormByAppType(appType: AppType, form: Any)
}