package io.kokuwa.maven.helm;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.apache.hc.core5.http.HttpHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import io.kokuwa.maven.helm.pojo.HelmRepository;
import io.kokuwa.maven.helm.pojo.RepoType;

@DisplayName("helm:upload")
public class UploadMojoTest extends AbstractMojoTest {

	@RegisterExtension
	static WireMockExtension mock = WireMockExtension.newInstance().failOnUnmatchedRequests(true).build();

	@DisplayName("no tar gz present")
	@Test
	void upgrade(UploadMojo mojo) {
		mojo.setUploadRepoStable(new HelmRepository()
				.setType(RepoType.CHARTMUSEUM)
				.setName("my-chartmuseum")
				.setUrl("http://127.0.0.1:" + mock.getPort() + "/chartmuseum"));
		assertHelm(mojo);
		assertTrue(mock.findAll(RequestPatternBuilder.allRequests()).isEmpty());
	}

	@DisplayName("with flag skip")
	@Test
	void skip(UploadMojo mojo) {
		assertHelm(mojo.setSkipUpload(false).setSkip(true));
		assertHelm(mojo.setSkipUpload(true).setSkip(false));
		assertHelm(mojo.setSkipUpload(true).setSkip(true));
	}

	@DisplayName("chartmuseum: without authentication")
	@Test
	void urlChartmusemWithoutAuthentication(UploadMojo mojo) {
		mojo.setUploadRepoStable(new HelmRepository()
				.setType(RepoType.CHARTMUSEUM)
				.setName("my-chartmuseum")
				.setUrl("http://127.0.0.1:" + mock.getPort() + "/chartmuseum"));
		copyPackagedHelmChartToOutputdirectory(mojo);
		assertUpload(mojo, RequestMethod.POST, "/chartmuseum", null);
	}

	@DisplayName("chartmuseum: with username/password")
	@Test
	void urlChartmuseumWithUsernameAndPassword(UploadMojo mojo) {
		mojo.setUploadRepoStable(new HelmRepository()
				.setType(RepoType.CHARTMUSEUM)
				.setName("my-chartmuseum")
				.setUrl("http://127.0.0.1:" + mock.getPort() + "/chartmuseum")
				.setUsername("foo")
				.setPassword("secret"));
		copyPackagedHelmChartToOutputdirectory(mojo);
		assertUpload(mojo, RequestMethod.POST, "/chartmuseum", BASIC_FOO_SECRET);
	}

	@DisplayName("chartmuseum: with serverId")
	@Test
	void urlChartmuseumWithServerId(UploadMojo mojo) {
		mojo.getSettings().addServer(getServer("my-chartmuseum", "foo", "secret"));
		mojo.setUploadRepoStable(new HelmRepository()
				.setType(RepoType.CHARTMUSEUM)
				.setName("my-chartmuseum")
				.setUrl("http://127.0.0.1:" + mock.getPort() + "/chartmuseum"));
		copyPackagedHelmChartToOutputdirectory(mojo);
		assertUpload(mojo, RequestMethod.POST, "/chartmuseum", BASIC_FOO_SECRET);
	}

	@DisplayName("chartmuseum: with serverId and encrypted password")
	@Test
	void urlChartmuseumWithServerIdEncrypted(UploadMojo mojo) {
		mojo.getSettings().addServer(getServer("my-chartmuseum", "foo", SECRET_ENCRYPTED));
		mojo.setHelmSecurity(SETTINGS_SECURITY_XML);
		mojo.setUploadRepoStable(new HelmRepository()
				.setType(RepoType.CHARTMUSEUM)
				.setName("my-chartmuseum")
				.setUrl("http://127.0.0.1:" + mock.getPort() + "/chartmuseum"));
		copyPackagedHelmChartToOutputdirectory(mojo);
		assertUpload(mojo, RequestMethod.POST, "/chartmuseum", BASIC_FOO_SECRET);
	}

	@DisplayName("nexus: without authentication")
	@Test
	void urlNexusWithoutAuthentication(UploadMojo mojo) {
		mojo.setUploadRepoStable(new HelmRepository()
				.setType(RepoType.NEXUS)
				.setName("my-nexus")
				.setUrl("http://127.0.0.1:" + mock.getPort() + "/nexus"));
		Path packaged = copyPackagedHelmChartToOutputdirectory(mojo);
		assertUpload(mojo, RequestMethod.PUT, "/nexus/" + packaged.getFileName(), null);
	}

	@DisplayName("nexus: with username/password")
	@Test
	void urlNexusWithUsernameAndPassword(UploadMojo mojo) {
		mojo.setUploadRepoStable(new HelmRepository()
				.setType(RepoType.NEXUS)
				.setName("my-nexus")
				.setUrl("http://127.0.0.1:" + mock.getPort() + "/nexus")
				.setUsername("foo")
				.setPassword("secret"));
		Path packaged = copyPackagedHelmChartToOutputdirectory(mojo);
		assertUpload(mojo, RequestMethod.PUT, "/nexus/" + packaged.getFileName(), BASIC_FOO_SECRET);
	}

	@DisplayName("nexus: with serverId")
	@Test
	void urlNexusWithServerId(UploadMojo mojo) {
		mojo.getSettings().addServer(getServer("my-nexus", "foo", "secret"));
		mojo.setUploadRepoStable(new HelmRepository()
				.setType(RepoType.NEXUS)
				.setName("my-nexus")
				.setUrl("http://127.0.0.1:" + mock.getPort() + "/nexus"));
		Path packaged = copyPackagedHelmChartToOutputdirectory(mojo);
		assertUpload(mojo, RequestMethod.PUT, "/nexus/" + packaged.getFileName(), BASIC_FOO_SECRET);
	}

	@DisplayName("nexus: with serverId and encrypted password")
	@Test
	void urlNexusWithServerIdEncrypted(UploadMojo mojo) {
		mojo.getSettings().addServer(getServer("my-chartmuseum", "foo", SECRET_ENCRYPTED));
		mojo.setHelmSecurity(SETTINGS_SECURITY_XML);
		mojo.setUploadRepoStable(new HelmRepository()
				.setType(RepoType.NEXUS)
				.setName("my-nexus")
				.setUrl("http://127.0.0.1:" + mock.getPort() + "/nexus"));
		Path packaged = copyPackagedHelmChartToOutputdirectory(mojo);
		assertUpload(mojo, RequestMethod.PUT, "/nexus/" + packaged.getFileName(), BASIC_FOO_SECRET);
	}

	@DisplayName("artifactory: without authentication")
	@Test
	void urlArtifactoryWithoutAuthentication(UploadMojo mojo) {
		copyPackagedHelmChartToOutputdirectory(mojo);
		mojo.setUploadRepoStable(new HelmRepository()
				.setType(RepoType.ARTIFACTORY)
				.setName("my-artifactory")
				.setUrl("http://example.org/repo"));
		assertThrows(IllegalArgumentException.class, mojo::execute, "Missing credentials must fail.");
	}

	@DisplayName("artifactory: with username/password")
	@Test
	void urlArtifactoryWithUsernameAndPassword(UploadMojo mojo) {
		mojo.setUploadRepoStable(new HelmRepository()
				.setType(RepoType.ARTIFACTORY)
				.setName("my-artifactory")
				.setUrl("http://127.0.0.1:" + mock.getPort() + "/artifactory")
				.setUsername("foo")
				.setPassword("secret"));
		Path packaged = copyPackagedHelmChartToOutputdirectory(mojo);
		assertUpload(mojo, RequestMethod.PUT, "/artifactory/" + packaged.getFileName(), BASIC_FOO_SECRET);
	}

	@DisplayName("artifactory: with username/password and groupId on repository level")
	@Test
	void urlArtifactoryWithServerIdEncryptedWithRepositoryGroupId(UploadMojo mojo) {
		mojo.setProjectGroupId("io.kokuwa.maven.helm");
		mojo.setProjectVersion("6.5.0");
		mojo.setUploadRepoStable(new HelmRepository()
				.setType(RepoType.ARTIFACTORY)
				.setName("my-artifactory")
				.setUrl("http://127.0.0.1:" + mock.getPort() + "/artifactory")
				.setUsername("foo")
				.setPassword("secret")
				.setUseGroupId(true));
		Path packaged = copyPackagedHelmChartToOutputdirectory(mojo);
		String expectedPath = "/artifactory/io/kokuwa/maven/helm/6.5.0/" + packaged.getFileName();
		assertUpload(mojo, RequestMethod.PUT, expectedPath, BASIC_FOO_SECRET);
	}

	@DisplayName("artifactory: with serverId")
	@Test
	void urlArtifactoryWithServerId(UploadMojo mojo) {
		mojo.getSettings().addServer(getServer("my-artifactory", "foo", "secret"));
		mojo.setUploadRepoStable(new HelmRepository()
				.setType(RepoType.ARTIFACTORY)
				.setName("my-artifactory")
				.setUrl("http://127.0.0.1:" + mock.getPort() + "/artifactory"));
		Path packaged = copyPackagedHelmChartToOutputdirectory(mojo);
		assertUpload(mojo, RequestMethod.PUT, "/artifactory/" + packaged.getFileName(), BASIC_FOO_SECRET);
	}

	@DisplayName("artifactory: with serverId and encrypted password")
	@Test
	void urlArtifactoryWithServerIdEncrypted(UploadMojo mojo) {
		mojo.getSettings().addServer(getServer("my-artifactory", "foo", SECRET_ENCRYPTED));
		mojo.setHelmSecurity(SETTINGS_SECURITY_XML);
		mojo.setUploadRepoStable(new HelmRepository()
				.setType(RepoType.ARTIFACTORY)
				.setName("my-artifactory")
				.setUrl("http://127.0.0.1:" + mock.getPort() + "/artifactory"));
		Path packaged = copyPackagedHelmChartToOutputdirectory(mojo);
		assertUpload(mojo, RequestMethod.PUT, "/artifactory/" + packaged.getFileName(), BASIC_FOO_SECRET);
	}

	@DisplayName("input: repository type missing")
	@Test
	void inputRepositoryTypeRequired(UploadMojo mojo) throws Exception {
		mojo.setUploadRepoStable(new HelmRepository()
				.setName("unknown-repo")
				.setUrl("http://example.org/repo"));
		copyPackagedHelmChartToOutputdirectory(mojo);
		assertThrows(IllegalArgumentException.class, mojo::execute, "Missing repo type must fail.");
	}

	private void assertUpload(UploadMojo mojo, RequestMethod method, String path, String authorization) {

		RequestPatternBuilder requestPattern;
		if (authorization == null) {
			mock.stubFor(WireMock.any(WireMock.anyUrl()).willReturn(WireMock.ok()));
			requestPattern = RequestPatternBuilder.allRequests();
		} else {
			mock.stubFor(WireMock.any(WireMock.anyUrl())
					.withHeader(HttpHeaders.AUTHORIZATION, WireMock.absent())
					.willReturn(WireMock.unauthorized().withHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic")));
			mock.stubFor(WireMock.any(WireMock.anyUrl())
					.withHeader(HttpHeaders.AUTHORIZATION, WireMock.matching(".*"))
					.willReturn(WireMock.ok()));
			requestPattern = RequestPatternBuilder.newRequestPattern()
					.withHeader(HttpHeaders.AUTHORIZATION, WireMock.matching(".*"));
		}

		assertTrue(path.startsWith("/"), "path should start with slash");
		assertDoesNotThrow(() -> mojo.execute(), "upload failed");
		List<LoggedRequest> requests = mock.findAll(requestPattern);
		assertEquals(1, requests.size(), "expected only one request");
		LoggedRequest request = requests.get(0);
		assertEquals(method, request.getMethod(), "method");
		assertEquals("http://127.0.0.1:" + mock.getPort() + path, request.getAbsoluteUrl(), "url");
		assertEquals("application/gzip", request.getHeader(HttpHeaders.CONTENT_TYPE), "content-type");
		assertEquals(authorization, request.getHeader(HttpHeaders.AUTHORIZATION), "authorization");
	}
}
