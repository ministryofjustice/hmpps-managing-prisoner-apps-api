package uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File

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
            .withBody(File("src/test/resources/JsonStub/prisoner/search/prisonerSearch.json").readText(Charsets.UTF_8)),
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
