package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder


class main
fun main1(args: Array<String>) {
  val mapper = Jackson2ObjectMapperBuilder().build<ObjectMapper>()
  val map1 = HashMap<String, Any>()
  map1.put("amount", 5)
  map1.put("number", 50)

  val map2 = HashMap<String, Any>()
  map2.put("amount", 5)
  map2.put("number", 50)

  val map3 = HashMap<String, Any>()
  map3.put("amount", 5)
  map3.put("number", 50)

  val requests = Requests(listOf<Map<String, Any>>(map1, map2, map3))

  val requestsString =  mapper.writeValueAsString(requests)
  println(requestsString)
   val req = mapper.readValue(requestsString, Requests::class.java)
  println(req)

  val field1 = DataField(
    1,
    "amountToTopUp",
    "Amount to top up",
    "integer",
    "20",
    null,
    true,
    null
  )

  val field2 = DataField(
    2,
    "contactPostcode",
    "Contact postcode",
    "string",
    "aby 23x",
    null,
    false,
    null
  )

  val field3 = DataField(
    3,
    "totalBalance",
    "Total Balance",
    "integer",
    "45",
    null,
    true,
    1
  )

  val map = HashMap<String, Form>()
  map.put(AppType.PIN_PHONE_ADD_NEW_CONTACT.toString(), Form(listOf(field1, field2,field3)))
  val forms = Forms(map)
  val formsString = mapper.writeValueAsString(forms)
  println(formsString)
  val forms1 = mapper.readValue(formsString, Forms::class.java)
  println(forms1)

  println(AppType.PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS)

}