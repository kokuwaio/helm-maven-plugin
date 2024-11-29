import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.BasicCredentials

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo

WireMockServer wireMockServer = context.get("wireMockServer")
try {
    // verify that registry login was tried
    wireMockServer.verify(getRequestedFor(urlPathEqualTo('/token'))
            .withBasicAuth(new BasicCredentials('auth-from-pom', 'bar')))
} finally {
    wireMockServer.stop()
}
