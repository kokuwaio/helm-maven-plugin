package io.kokuwa.maven.helm;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Proxy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import io.kokuwa.maven.helm.junit.MojoExtension;
import io.kokuwa.maven.helm.pojo.HelmRepository;
import io.kokuwa.maven.helm.pojo.RepoType;

@DisplayName("helm:init")
public class InitMojoTest extends AbstractMojoTest {

	@RegisterExtension
	static WireMockExtension mock = WireMockExtension.newInstance()
			.failOnUnmatchedRequests(true)
			.options(WireMockConfiguration.wireMockConfig().enableBrowserProxying(true))
			.build();

	@DisplayName("default values")
	@Test
	void init(InitMojo mojo) {
		assertHelm(mojo, "repo add stable https://charts.helm.sh/stable");
	}

	@DisplayName("with flag skip")
	@Test
	void skip(InitMojo mojo) {
		assertHelm(mojo.setSkipInit(false).setSkip(true));
		assertHelm(mojo.setSkipInit(true).setSkip(false));
		assertHelm(mojo.setSkipInit(true).setSkip(true));
	}

	@DisplayName("with flag --force-update")
	@Test
	void withForceUpdateForAll(InitMojo mojo) {
		mojo.setAddDefaultRepo(true);
		mojo.setAddUploadRepos(true);
		mojo.setRepositoryAddForceUpdate(true);
		mojo.setHelmExtraRepos(new HelmRepository[] { new HelmRepository()
				.setName("example")
				.setUrl("https://example.org/repo/example")
				.setForceUpdate(true) });
		mojo.setUploadRepoStable(new HelmRepository()
				.setType(RepoType.ARTIFACTORY)
				.setName("example-stable")
				.setUrl("https://example.org/repo/stable"));
		mojo.setUploadRepoSnapshot(new HelmRepository()
				.setType(RepoType.ARTIFACTORY)
				.setName("example-snapshot")
				.setUrl("https://example.org/repo/snapshot"));
		assertHelm(mojo,
				"repo add stable " + InitMojo.STABLE_HELM_REPO + " --force-update",
				"repo add example-stable https://example.org/repo/stable --force-update",
				"repo add example-snapshot https://example.org/repo/snapshot --force-update",
				"repo add example https://example.org/repo/example --force-update");
	}

	@DisplayName("with flag --force-update for single repository")
	@Test
	void withForceUpdateForSingleRepository(InitMojo mojo) {
		mojo.setAddDefaultRepo(true);
		mojo.setHelmExtraRepos(new HelmRepository[] { new HelmRepository()
				.setName("example")
				.setUrl("https://example.org/repo/example")
				.setForceUpdate(true) });
		assertHelm(mojo,
				"repo add stable " + InitMojo.STABLE_HELM_REPO,
				"repo add example https://example.org/repo/example --force-update");
	}

	@DisplayName("without flag addDefaultRepo")
	@Test
	void addDefaultRepo(InitMojo mojo) {
		assertHelm(mojo.setAddDefaultRepo(false));
	}

	@DisplayName("executable: local")
	@Test
	void localHelm(InitMojo mojo) {
		mojo.setAutoDetectLocalHelmBinary(false);
		mojo.setUseLocalHelmBinary(true);
		mojo.setFallbackBinaryDownload(false);
		mojo.setHelmVersion(null);
		mojo.setHelmExecutableDirectory(MojoExtension.determineHelmExecutableDirectory().toFile());
		assertHelm(mojo, "version", "repo add stable " + InitMojo.STABLE_HELM_REPO);
	}

	@DisplayName("executable: local missing and fallback enabled")
	@Test
	void localHelmMissingWithFallbackEnabled(InitMojo mojo) {
		mojo.setAutoDetectLocalHelmBinary(false);
		mojo.setUseLocalHelmBinary(true);
		mojo.setFallbackBinaryDownload(true);
		mojo.setHelmVersion("3.12.0");
		mojo.setHelmExecutableDirectory(createTempDirectory());
		assertHelm(mojo, "repo add stable " + InitMojo.STABLE_HELM_REPO);
		assertHelmExecuteable(mojo);
	}

	@DisplayName("executable: local missing and fallback disabled")
	@Test
	void localHelmMissingWithFallbackDisabled(InitMojo mojo) {
		mojo.setAutoDetectLocalHelmBinary(false);
		mojo.setUseLocalHelmBinary(true);
		mojo.setFallbackBinaryDownload(false);
		mojo.setHelmVersion("3.12.0");
		mojo.setHelmExecutableDirectory(createTempDirectory());
		assertThrows(MojoExecutionException.class, mojo::execute);
	}

	@DisplayName("executable: download with version")
	@Test
	void downloadHelmWithVersion(InitMojo mojo) {
		mojo.setHelmExecutableDirectory(createTempDirectory());
		mojo.setHelmVersion("3.12.0");
		mojo.setUseLocalHelmBinary(false);
		assertHelm(mojo, "repo add stable " + InitMojo.STABLE_HELM_REPO);
		assertHelmExecuteable(mojo);
	}

	@DisplayName("executable: download with proxy")
	@Test
	void downloadHelmWithProxyUnauthenticated(InitMojo mojo) {

		Proxy proxy1 = new Proxy();
		proxy1.setId("test1");
		proxy1.setActive(false);
		proxy1.setProtocol("http");
		proxy1.setHost("proxy1.example.org");
		proxy1.setPort(8000);
		proxy1.setNonProxyHosts(null);
		Proxy proxy2 = new Proxy();
		proxy2.setId("test2");
		proxy2.setActive(true);
		proxy2.setProtocol("http");
		proxy2.setHost("proxy2.example.org");
		proxy2.setPort(8000);
		proxy2.setNonProxyHosts("nope|get.helm.sh");
		Proxy proxy3 = new Proxy();
		proxy3.setId("test3");
		proxy3.setActive(true);
		proxy3.setProtocol("http");
		proxy3.setHost("proxy3.127.0.0.1.nip.io");
		proxy3.setPort(mock.getPort());
		proxy3.setNonProxyHosts(null);

		mojo.setHelmExecutableDirectory(createTempDirectory());
		mojo.setHelmVersion("3.12.0");
		mojo.setUseLocalHelmBinary(false);
		mojo.getSettings().addProxy(proxy1);
		mojo.getSettings().addProxy(proxy2);
		mojo.getSettings().addProxy(proxy3);
		assertHelm(mojo, "repo add stable " + InitMojo.STABLE_HELM_REPO);

		List<LoggedRequest> requests = mock.findAll(RequestPatternBuilder.allRequests());
		assertEquals(1, requests.size(), "expected only 1 response, got: " + requests);
		LoggedRequest request = requests.get(0);
		assertTrue(request.isBrowserProxyRequest(), "isBrowserProxyRequest(): " + request);
		assertEquals("get.helm.sh", request.getHost(), "getHost(): " + request);
		assertEquals(RequestMethod.GET, request.getMethod(), "getMethod(): " + request);
		assertTrue(request.getUrl().startsWith("/helm-v3.12.0-"), "getUrl(): " + request);
		assertHelmExecuteable(mojo);
	}

	@DisplayName("executable: download with url")
	@DisabledOnOs(OS.WINDOWS)
	@Test
	void downloadHelmWithUrl(InitMojo mojo) throws IOException {
		mojo.setHelmExecutableDirectory(createTempDirectory());
		mojo.setHelmVersion(null);
		mojo.setHelmDownloadUrl(new URL("https://get.helm.sh/helm-v3.12.0-linux-amd64.tar.gz"));
		assertHelm(mojo, "repo add stable " + InitMojo.STABLE_HELM_REPO);
	}

	@DisplayName("repository: extra repo without authentication")
	@Test
	void extraRepositoryWithoutAuthentication(InitMojo mojo) {
		mojo.setAddDefaultRepo(false);
		mojo.setHelmExtraRepos(new HelmRepository[] { new HelmRepository()
				.setName("extra")
				.setUrl("https://example.org/extra") });
		assertHelm(mojo, "repo add extra https://example.org/extra");
	}

	@DisplayName("repository: extra repo with username/password")
	@Test
	void extraRepositoryWithUsernameAndPassword(InitMojo mojo) {
		mojo.setAddDefaultRepo(false);
		mojo.setHelmExtraRepos(new HelmRepository[] { new HelmRepository()
				.setName("extra")
				.setUrl("https://example.org/extra")
				.setUsername("foo")
				.setPassword("secret") });
		assertHelm(mojo, "repo add extra https://example.org/extra --username foo --password secret");
	}

	@DisplayName("repository: extra repo with serverId")
	@Test
	void extraRepositoryServerId(InitMojo mojo) {
		mojo.setAddDefaultRepo(false);
		mojo.getSettings().getServers().add(getServer("extra", "foo", "secret"));
		mojo.setHelmExtraRepos(new HelmRepository[] { new HelmRepository()
				.setName("extra")
				.setUrl("https://example.org/extra") });
		assertHelm(mojo, "repo add extra https://example.org/extra --username foo --password secret");
	}

	@DisplayName("repository: stable repo without authentication")
	@Test
	void stableRepositoryWithoutAuthentication(InitMojo mojo) {
		mojo.setAddDefaultRepo(false);
		mojo.setAddUploadRepos(true);
		mojo.setUploadRepoStable(new HelmRepository().setName("stable").setUrl("https://example.org/stable"));
		assertHelm(mojo, "repo add stable https://example.org/stable");
	}

	@DisplayName("repository: stable repo with username/password")
	@Test
	void stableRepositoryWithUsernameAndPassword(InitMojo mojo) {
		mojo.setAddDefaultRepo(false);
		mojo.setAddUploadRepos(true);
		mojo.setUploadRepoStable(new HelmRepository()
				.setName("stable")
				.setUrl("https://example.org/stable")
				.setUsername("foo")
				.setPassword("secret"));
		assertHelm(mojo, "repo add stable https://example.org/stable --username foo --password secret");
	}

	@DisplayName("repository: stable repo with serverId")
	@Test
	void stableRepositoryServerId(InitMojo mojo) {
		mojo.setAddDefaultRepo(false);
		mojo.setAddUploadRepos(true);
		mojo.getSettings().getServers().add(getServer("stable", "foo", "secret"));
		mojo.setUploadRepoStable(new HelmRepository().setName("stable").setUrl("https://example.org/stable"));
		assertHelm(mojo, "repo add stable https://example.org/stable --username foo --password secret");
	}

	@DisplayName("repository: snapshot repo without authentication")
	@Test
	void snapshotRepositoryWithoutAuthentication(InitMojo mojo) {
		mojo.setAddDefaultRepo(false);
		mojo.setAddUploadRepos(true);
		mojo.setUploadRepoSnapshot(new HelmRepository()
				.setName("snapshot")
				.setUrl("https://example.org/stable"));
		assertHelm(mojo, "repo add snapshot https://example.org/stable");
	}

	@DisplayName("repository: snapshot repo with username/password")
	@Test
	void snapshotRepositoryWithUsernameAndPassword(InitMojo mojo) {
		mojo.setAddDefaultRepo(false);
		mojo.setAddUploadRepos(true);
		mojo.setUploadRepoSnapshot(new HelmRepository()
				.setName("snapshot")
				.setUrl("https://example.org/stable")
				.setUsername("foo")
				.setPassword("secret"));
		assertHelm(mojo, "repo add snapshot https://example.org/stable --username foo --password secret");
	}

	@DisplayName("repository: snapshot repo with serverId")
	@Test
	void snapshotRepositoryServerId(InitMojo mojo) {
		mojo.setAddDefaultRepo(false);
		mojo.setAddUploadRepos(true);
		mojo.getSettings().getServers().add(getServer("snapshot", "foo", "secret"));
		mojo.setUploadRepoSnapshot(new HelmRepository().setName("snapshot").setUrl("https://example.org/stable"));
		assertHelm(mojo, "repo add snapshot https://example.org/stable --username foo --password secret");
	}

	@DisplayName("repository: same stable & snapshot repo")
	@Test
	void stableAndsnapshotRepository(InitMojo mojo) {
		mojo.setAddDefaultRepo(false);
		mojo.setAddUploadRepos(true);
		mojo.setUploadRepoStable(new HelmRepository().setName("upload").setUrl("https://example.org/upload"));
		mojo.setUploadRepoSnapshot(new HelmRepository().setName("upload").setUrl("https://example.org/upload"));
		assertHelm(mojo, "repo add upload https://example.org/upload");
	}

	@DisplayName("repository: different stable & snapshot repo")
	@Test
	void stableAndsnapshotRepositoryDiffer(InitMojo mojo) {
		mojo.setAddDefaultRepo(false);
		mojo.setAddUploadRepos(true);
		mojo.setUploadRepoStable(new HelmRepository().setName("stable").setUrl("https://example.org/stable"));
		mojo.setUploadRepoSnapshot(new HelmRepository().setName("snapshot").setUrl("https://example.org/snapshot"));
		assertHelm(mojo,
				"repo add stable https://example.org/stable",
				"repo add snapshot https://example.org/snapshot");
	}

	@DisplayName("repository: all different kinds of configurations")
	@Test
	void allKindsOfRepositories(InitMojo mojo) {
		mojo.setAddUploadRepos(true);
		mojo.getSettings().getServers().add(getServer("extra1", "user-extra1", "secret-extra1"));
		mojo.setHelmExtraRepos(new HelmRepository[] {
				new HelmRepository()
						.setName("extra1")
						.setUrl("https://example.org/extra1"),
				new HelmRepository()
						.setName("extra2")
						.setUrl("https://example.org/extra2")
						.setUsername("user-extra2")
						.setPassword("secret-extra2"),
				new HelmRepository()
						.setName("extra3")
						.setUrl("https://example.org/extra3") });
		mojo.setUploadRepoStable(new HelmRepository().setName("my-stable").setUrl("https://example.org/stable"));
		mojo.setUploadRepoSnapshot(new HelmRepository().setName("my-snapshot").setUrl("https://example.org/snapshot"));
		assertHelm(mojo,
				"repo add stable " + InitMojo.STABLE_HELM_REPO,
				"repo add my-stable https://example.org/stable",
				"repo add my-snapshot https://example.org/snapshot",
				"repo add extra1 https://example.org/extra1 --username user-extra1 --password secret-extra1",
				"repo add extra2 https://example.org/extra2 --username user-extra2 --password secret-extra2",
				"repo add extra3 https://example.org/extra3");
	}

	private File createTempDirectory() {
		return assertDoesNotThrow(() -> Files.createTempDirectory("helm-maven-plugin-test")).toFile();
	}

	private void assertHelmExecuteable(InitMojo mojo) {
		Path helmExecutable = mojo.getHelmExecutableDirectory().resolve(HELM);
		assertTrue(Files.isRegularFile(helmExecutable), "executable not found");
		assertTrue(Files.isExecutable(helmExecutable), "executable not executable");
	}
}
