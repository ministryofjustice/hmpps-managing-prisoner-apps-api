package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException

enum class AppType {
  PIN_PHONE_EMERGENCY_CREDIT_TOP_UP,
  PIN_PHONE_ADD_NEW_SOCIAL_CONTACT,
  PIN_PHONE_ADD_NEW_LEGAL_CONTACT,
  PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS,
  PIN_PHONE_SUPPLY_LIST_OF_CONTACTS,
  ;

  companion object {
    fun getAppType(type: String): AppType {
      if (type == PIN_PHONE_EMERGENCY_CREDIT_TOP_UP.toString()) {
        return PIN_PHONE_EMERGENCY_CREDIT_TOP_UP
      }
      if (type == PIN_PHONE_ADD_NEW_SOCIAL_CONTACT.toString()) {
        return PIN_PHONE_ADD_NEW_SOCIAL_CONTACT
      }
      if (type == PIN_PHONE_ADD_NEW_LEGAL_CONTACT.toString()) {
        return PIN_PHONE_ADD_NEW_LEGAL_CONTACT
      }
      if (type == PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS.toString()) {
        return PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS
      }
      if (type == PIN_PHONE_SUPPLY_LIST_OF_CONTACTS.toString()) {
        return PIN_PHONE_SUPPLY_LIST_OF_CONTACTS
      }
      throw ApiException("$type do not match with any app type", HttpStatus.BAD_REQUEST)
    }
  }
}
