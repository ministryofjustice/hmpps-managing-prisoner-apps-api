package uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class ManageUsersApiMockServer : WireMockServer(8094) {
  fun stubHealthPing(status: Int) {
    stubFor(
      get(urlMatching("/health/.*")).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody("""{"status":"${if (status == 200) "UP" else "DOWN"}"}""")
          .withStatus(status),
      ),
    )
  }

  fun stubStaffDetailsFound(staffUserName: String) {
    stubFor(
      get(urlPathMatching("/users/$staffUserName"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              "{\n" +
                "  \"username\": \"${staffUserName}\",\n" +
                "  \"active\": true,\n" +
                "  \"name\": \"Joe Bloggs\",\n" +
                "  \"authSource\": \"nomis\",\n" +
                "  \"staffId\": 401228,\n" +
                "  \"activeCaseLoadId\": \"TEST_ESTABLISHMENT_FIRST\",\n" +
                "  \"userId\": \"401228\",\n" +
                "  \"uuid\": \"d6c60000-0000-0000-b664-0000ca06ddea\"\n" +
                "}",
            ),
        ),
    )
  }
}

class ManageUsersApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val manageUsersApi = ManageUsersApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext): Unit = manageUsersApi.start()
  override fun beforeEach(context: ExtensionContext): Unit = manageUsersApi.resetAll()
  override fun afterAll(context: ExtensionContext): Unit = manageUsersApi.stop()
}
