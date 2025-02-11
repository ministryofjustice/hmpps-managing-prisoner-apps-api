package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Forms
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Requests

interface AppTypeFormValidation {
  fun validateFormByAppType(appType: AppType, requests: Requests, forms: Forms)
}