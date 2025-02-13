package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Forms
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Requests

class AppTypeFormValidationImpl : AppTypeFormValidation {

  override fun validateFormByAppType(appType: AppType, requests: Requests, forms: Forms) {
    TODO("Not yet implemented")
    val fields = forms.formMap[appType.toString()]
    requests.request.forEach { x ->
      x.keys.forEach {
          y ->
          fields!!.dataFields.forEach {
            f ->
            if (y === f.name) {
              // check for type
              if(f.type === "string") {
                if(x[y] is String) {

                } else {
                  throw Exception("Type i snot string")
                }
              }
              if(f.type === "integer") {
                if(x[y] is Integer) {

                } else {
                  throw Exception("Type is not integer")
                }
              }
              // check for value
              if (f.required) {
                if (x[y] === null) {
                  throw Exception("$y value is required")
                }
              }
            }
          }
      }
    }
  }
}