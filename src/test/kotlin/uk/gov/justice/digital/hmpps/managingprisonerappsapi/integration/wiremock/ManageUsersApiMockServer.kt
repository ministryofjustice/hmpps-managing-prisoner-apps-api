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
import java.io.File

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

  fun stubStaffDetailsFound() {
    stubFor(
      get(urlPathMatching("/users/[a-zA-Z0-9_@]*"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(File("src/test/resources/JsonStub/manage/users/staffDetails.json").readText(Charsets.UTF_8)),
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
