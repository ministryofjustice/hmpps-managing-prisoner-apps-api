package uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class PrisonerSearchApiMockServer : WireMockServer(8093) {
  fun stubHealthPing(status: Int) {
    stubFor(
      get("/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody("""{"status":"${if (status == 200) "UP" else "DOWN"}"}""")
          .withStatus(status),
      ),
    )
  }

  fun stubPrisonerSearchFound(prisonerId: String) {
    stubFor(
      get(urlPathMatching("/prisoner/$prisonerId"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              "{\n" +
                "  \"prisonerNumber\": \"${prisonerId}\",\n" +
                "  \"pncNumber\": \"77/119926A\",\n" +
                "  \"pncNumberCanonicalShort\": \"77/119926A\",\n" +
                "  \"pncNumberCanonicalLong\": \"1977/119926A\",\n" +
                "  \"croNumber\": \"119926/77F\",\n" +
                "  \"bookingId\": \"668286\",\n" +
                "  \"bookNumber\": \"G12345\",\n" +
                "  \"title\": \"Miss\",\n" +
                "  \"firstName\": \"YFHINNAIN\",\n" +
                "  \"lastName\": \"DAMELLA\",\n" +
                "  \"dateOfBirth\": \"1961-05-03\",\n" +
                "  \"gender\": \"Female\",\n" +
                "  \"ethnicity\": \"White: Eng./Welsh/Scot./N.Irish/British\",\n" +
                "  \"raceCode\": \"W1\",\n" +
                "  \"youthOffender\": false,\n" +
                "  \"religion\": \"No Religion\",\n" +
                "  \"nationality\": \"British\",\n" +
                "  \"status\": \"INACTIVE OUT\",\n" +
                "  \"lastMovementTypeCode\": \"REL\",\n" +
                "  \"lastMovementReasonCode\": \"NCS\",\n" +
                "  \"inOutStatus\": \"OUT\",\n" +
                "  \"prisonId\": \"OUT\",\n" +
                "  \"lastPrisonId\": \"BZI\",\n" +
                "  \"prisonName\": \"Outside\",\n" +
                "  \"aliases\": [\n" +
                "    {\n" +
                "      \"firstName\": \"YFHINNAIN\",\n" +
                "      \"middleNames\": \"ARTYA\",\n" +
                "      \"lastName\": \"BOBBIYA\",\n" +
                "      \"dateOfBirth\": \"1961-04-25\",\n" +
                "      \"gender\": \"Female\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"firstName\": \"YFHINNAIN\",\n" +
                "      \"middleNames\": \"HUNTINGTON\",\n" +
                "      \"lastName\": \"DRUR\",\n" +
                "      \"dateOfBirth\": \"1961-05-18\",\n" +
                "      \"gender\": \"Female\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"alerts\": [\n" +
                "    {\n" +
                "      \"alertType\": \"X\",\n" +
                "      \"alertCode\": \"XA\",\n" +
                "      \"active\": true,\n" +
                "      \"expired\": false\n" +
                "    },\n" +
                "    {\n" +
                "      \"alertType\": \"R\",\n" +
                "      \"alertCode\": \"RPB\",\n" +
                "      \"active\": true,\n" +
                "      \"expired\": false\n" +
                "    },\n" +
                "    {\n" +
                "      \"alertType\": \"R\",\n" +
                "      \"alertCode\": \"RSS\",\n" +
                "      \"active\": true,\n" +
                "      \"expired\": false\n" +
                "    },\n" +
                "    {\n" +
                "      \"alertType\": \"H\",\n" +
                "      \"alertCode\": \"HA\",\n" +
                "      \"active\": true,\n" +
                "      \"expired\": false\n" +
                "    }\n" +
                "  ],\n" +
                "  \"csra\": \"High\",\n" +
                "  \"category\": \"U\",\n" +
                "  \"legalStatus\": \"UNKNOWN\",\n" +
                "  \"imprisonmentStatus\": \"UNKNOWN\",\n" +
                "  \"imprisonmentStatusDescription\": \"Disposal Not Known\",\n" +
                "  \"convictedStatus\": \"Remand\",\n" +
                "  \"mostSeriousOffence\": \"Possess a controlled drug of Class B - Cannabis / Cannabis Resin\",\n" +
                "  \"recall\": false,\n" +
                "  \"indeterminateSentence\": false,\n" +
                "  \"receptionDate\": \"2012-09-17\",\n" +
                "  \"cellLocation\": \"7-1-007\",\n" +
                "  \"locationDescription\": \"Outside - released from Bronzefield (HMP)\",\n" +
                "  \"restrictedPatient\": false,\n" +
                "  \"currentIncentive\": {\n" +
                "    \"level\": {\n" +
                "      \"code\": \"STD\",\n" +
                "      \"description\": \"Standard\"\n" +
                "    },\n" +
                "    \"dateTime\": \"2012-09-17T18:44:55\",\n" +
                "    \"nextReviewDate\": \"2013-09-17\"\n" +
                "  },\n" +
                "  \"heightCentimetres\": 193,\n" +
                "  \"weightKilograms\": 51,\n" +
                "  \"hairColour\": \"Brown\",\n" +
                "  \"rightEyeColour\": \"Blue\",\n" +
                "  \"leftEyeColour\": \"Blue\",\n" +
                "  \"facialHair\": \"Not Asked\",\n" +
                "  \"shapeOfFace\": \"Oval\",\n" +
                "  \"build\": \"Thin\",\n" +
                "  \"tattoos\": [\n" +
                "    {\n" +
                "      \"bodyPart\": \"Arm\",\n" +
                "      \"comment\": \"ncdSHEhNkumrlncdSHEhNkumrl\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"addresses\": [\n" +
                "    {\n" +
                "      \"fullAddress\": \"SS, ydCSWHWydCSWHW, fJjWkfJjW, Milton Keynes, OU1 9BK, England\",\n" +
                "      \"postalCode\": \"OU1 9BK\",\n" +
                "      \"startDate\": \"2012-09-01\",\n" +
                "      \"primaryAddress\": true,\n" +
                "      \"noFixedAddress\": false,\n" +
                "      \"phoneNumbers\": []\n" +
                "    }\n" +
                "  ],\n" +
                "  \"emailAddresses\": [],\n" +
                "  \"phoneNumbers\": [],\n" +
                "  \"identifiers\": [\n" +
                "    {\n" +
                "      \"type\": \"CRO\",\n" +
                "      \"value\": \"119926/77F\",\n" +
                "      \"issuedDate\": \"2012-09-18\",\n" +
                "      \"createdDateTime\": \"2012-09-18T09:41:27\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"type\": \"PNC\",\n" +
                "      \"value\": \"77/119926A\",\n" +
                "      \"issuedDate\": \"2012-09-18\",\n" +
                "      \"createdDateTime\": \"2012-09-18T09:41:27\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"allConvictedOffences\": [\n" +
                "    {\n" +
                "      \"statuteCode\": \"CL77\",\n" +
                "      \"offenceCode\": \"CL77033\",\n" +
                "      \"offenceDescription\": \"Conspire to commit either way offence outside England and Wales in relation to damage and offences against property\",\n" +
                "      \"offenceDate\": \"2012-10-28\",\n" +
                "      \"latestBooking\": true\n" +
                "    },\n" +
                "    {\n" +
                "      \"statuteCode\": \"MD71\",\n" +
                "      \"offenceCode\": \"MD71530\",\n" +
                "      \"offenceDescription\": \"Possess a controlled drug of Class B - Cannabis / Cannabis Resin\",\n" +
                "      \"offenceDate\": \"2012-10-26\",\n" +
                "      \"latestBooking\": true\n" +
                "    },\n" +
                "    {\n" +
                "      \"statuteCode\": \"PC53\",\n" +
                "      \"offenceCode\": \"PC53001\",\n" +
                "      \"offenceDescription\": \"Possess an offensive weapon in a public place\",\n" +
                "      \"offenceDate\": \"2012-10-05\",\n" +
                "      \"latestBooking\": true\n" +
                "    },\n" +
                "    {\n" +
                "      \"statuteCode\": \"CJ88\",\n" +
                "      \"offenceCode\": \"CJ88144\",\n" +
                "      \"offenceDescription\": \"Possess knife blade / sharp pointed article in a public place - Criminal Justice Act 1988\",\n" +
                "      \"offenceDate\": \"2012-10-16\",\n" +
                "      \"latestBooking\": true\n" +
                "    }\n" +
                "  ]\n" +
                "}",
            ),
        ),
    )
  }
}

class PrisonerSearchApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val prisonerSearchApi = PrisonerSearchApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext): Unit = prisonerSearchApi.start()
  override fun beforeEach(context: ExtensionContext): Unit = prisonerSearchApi.resetAll()
  override fun afterAll(context: ExtensionContext): Unit = prisonerSearchApi.stop()
}
