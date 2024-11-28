import com.github.tomakehurst.wiremock.WireMockServer

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching

WireMockServer wireMockServer = context.get("wireMockServer")
try {
    // verify that proxy was used
    wireMockServer.verify(getRequestedFor(urlMatching('/helm-v3.12.0-.*(tar.gz|zip)')))
} finally {
    wireMockServer.stop()
}

def logs = new File(basedir, 'build.log').text

assert logs.contains('Use proxy [my-proxy] for [https://get.helm.sh/helm-v3.12.0-')
