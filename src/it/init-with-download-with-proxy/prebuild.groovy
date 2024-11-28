import com.github.tomakehurst.wiremock.WireMockServer

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options

// prepare wiremock for proxy
WireMockServer wireMockServer = new WireMockServer(options()
        .dynamicPort()
        .enableBrowserProxying(true))
wireMockServer.start();

def wiremockProxyPort = wireMockServer.port()

// save for using in post build
context.put("wireMockServer", wireMockServer)

// generate settings - we need a dynamic port
def settingsFile = new File(basedir, "settings.xml")
settingsFile.delete()
settingsFile << """
<?xml version="1.0" encoding="UTF-8"?>
<settings>
  <proxies>
    <proxy>
      <id>my-proxy</id>
      <host>localhost</host>
      <port>${wiremockProxyPort}</port>
    </proxy>
  </proxies>
</settings>"""

true