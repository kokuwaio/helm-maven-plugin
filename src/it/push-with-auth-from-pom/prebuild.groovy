import com.github.tomakehurst.wiremock.WireMockServer

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options

// prepare wiremock
WireMockServer wireMockServer = new WireMockServer(options()
        .dynamicPort()
        .enableBrowserProxying(true))

wireMockServer.start();

// mock OCI login protocol
wireMockServer.givenThat(get('/v2/').willReturn(aResponse()
        .withStatus(401)
        .withHeader("Www-Authenticate", "Bearer realm=\"http://localhost:" + wireMockServer.port() + "/token\",service=\"test.service\"")))

wireMockServer.givenThat(get(urlPathEqualTo('/token'))
        .willReturn(aResponse().withStatus(401)))

// save for using in post build
context.put("wireMockServer", wireMockServer)

// pass port to project
def userProperties = context.get('userProperties')
userProperties.put('wiremockPort', String.valueOf(wireMockServer.port()))
