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

  fun stubPrisonerSearchFound() {
    stubFor(
      get(urlPathMatching("/prisoner/[a-zA-Z0-9]*"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("{\"prisonerNumber\":\"A7795DY\",\"croNumber\":\"03/123113113\",\"bookingId\":\"1204755\",\"bookNumber\":\"41758A\",\"title\":\"Dr\",\"firstName\":\"JOHN\",\"middleNames\":\"BLOB\",\"lastName\":\"WILLIS\",\"dateOfBirth\":\"1991-02-28\",\"gender\":\"Male\",\"ethnicity\":\"White : Irish\",\"raceCode\":\"W2\",\"youthOffender\":false,\"status\":\"ACTIVE IN\",\"lastMovementTypeCode\":\"ADM\",\"lastMovementReasonCode\":\"N\",\"inOutStatus\":\"IN\",\"prisonId\":\"MDI\",\"lastPrisonId\":\"MDI\",\"prisonName\":\"Moorland (HMP & YOI)\",\"cellLocation\":\"RECV\",\"aliases\":[],\"alerts\":[],\"legalStatus\":\"SENTENCED\",\"imprisonmentStatus\":\"SENT03\",\"imprisonmentStatusDescription\":\"Adult Imprisonment Without Option CJA03\",\"convictedStatus\":\"Convicted\",\"recall\":false,\"indeterminateSentence\":false,\"receptionDate\":\"2023-02-07\",\"locationDescription\":\"Moorland (HMP & YOI)\",\"restrictedPatient\":false,\"currentIncentive\":{\"level\":{\"code\":\"ENH\",\"description\":\"Enhanced\"},\"dateTime\":\"2023-02-07T10:43:44\",\"nextReviewDate\":\"2023-05-07\"},\"leftEyeColour\":\"Clouded\",\"addresses\":[],\"emailAddresses\":[],\"phoneNumbers\":[],\"identifiers\":[{\"type\":\"CRO\",\"value\":\"03/123113113\",\"issuedDate\":\"2021-01-18\",\"createdDateTime\":\"2021-01-18T12:15:14\"}],\"allConvictedOffences\":[]}"),
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
