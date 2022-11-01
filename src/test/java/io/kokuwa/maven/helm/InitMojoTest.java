package io.kokuwa.maven.helm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.kokuwa.maven.helm.pojo.HelmRepository;
import io.kokuwa.maven.helm.pojo.RepoType;

@DisplayName("helm:init")
public class InitMojoTest extends AbstractMojoTest {

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

	@DisplayName("executeable: local")
	@Test
	@DisabledOnOs(OS.WINDOWS)
	void localHelm(InitMojo mojo) {
		mojo.setUseLocalHelmBinary(true);
		mojo.setHelmExecutableDirectory("src/it");
		assertHelm(mojo, "version", "repo add stable " + InitMojo.STABLE_HELM_REPO);
	}

	@DisplayName("executable: download with version")
	@Test
	void downloadHelmWithVersion(InitMojo mojo) throws IOException {
		Path helmExecutableDirectory = Files.createTempDirectory("helm-maven-plugin-test");
		Path helmExecutable = helmExecutableDirectory.resolve(HELM);
		mojo.setHelmExecutableDirectory(helmExecutableDirectory.toString());
		mojo.setHelmVersion("3.10.1");
		mojo.setUseLocalHelmBinary(false);
		assertHelm(mojo, "repo add stable " + InitMojo.STABLE_HELM_REPO);
		assertTrue(Files.isRegularFile(helmExecutable), "executable not found");
		assertTrue(Files.isExecutable(helmExecutable), "executable not executable");
	}

	@DisplayName("executable: download with url")
	@Test
	void downloadHelmWithUrl(InitMojo mojo) throws IOException {
		Path helmExecutableDirectory = Files.createTempDirectory("helm-maven-plugin-test");
		Path helmExecutable = helmExecutableDirectory.resolve("helm");
		mojo.setHelmExecutableDirectory(helmExecutableDirectory.toString());
		mojo.setHelmDownloadUrl(new URL("https://get.helm.sh/helm-v3.10.1-linux-amd64.tar.gz"));
		mojo.setUseLocalHelmBinary(false);
		assertHelm(mojo, "repo add stable " + InitMojo.STABLE_HELM_REPO);
		assertTrue(Files.isRegularFile(helmExecutable), "executable not found");
		assertTrue(Files.isExecutable(helmExecutable), "executable not executable");
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
		assertHelm(mojo, "repo add extra https://example.org/extra --username=foo --password=secret");
	}

	@DisplayName("repository: extra repo with serverId")
	@Test
	void extraRepositoryServerId(InitMojo mojo) {
		mojo.setAddDefaultRepo(false);
		mojo.getSettings().getServers().add(getServer("extra", "foo", "secret"));
		mojo.setHelmExtraRepos(new HelmRepository[] { new HelmRepository()
				.setName("extra")
				.setUrl("https://example.org/extra") });
		assertHelm(mojo, "repo add extra https://example.org/extra --username=foo --password=secret");
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
		assertHelm(mojo, "repo add stable https://example.org/stable --username=foo --password=secret");
	}

	@DisplayName("repository: stable repo with serverId")
	@Test
	void stableRepositoryServerId(InitMojo mojo) {
		mojo.setAddDefaultRepo(false);
		mojo.setAddUploadRepos(true);
		mojo.getSettings().getServers().add(getServer("stable", "foo", "secret"));
		mojo.setUploadRepoStable(new HelmRepository().setName("stable").setUrl("https://example.org/stable"));
		assertHelm(mojo, "repo add stable https://example.org/stable --username=foo --password=secret");
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
		assertHelm(mojo, "repo add snapshot https://example.org/stable --username=foo --password=secret");
	}

	@DisplayName("repository: snapshot repo with serverId")
	@Test
	void snapshotRepositoryServerId(InitMojo mojo) {
		mojo.setAddDefaultRepo(false);
		mojo.setAddUploadRepos(true);
		mojo.getSettings().getServers().add(getServer("snapshot", "foo", "secret"));
		mojo.setUploadRepoSnapshot(new HelmRepository().setName("snapshot").setUrl("https://example.org/stable"));
		assertHelm(mojo, "repo add snapshot https://example.org/stable --username=foo --password=secret");
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
				"repo add extra1 https://example.org/extra1 --username=user-extra1 --password=secret-extra1",
				"repo add extra2 https://example.org/extra2 --username=user-extra2 --password=secret-extra2",
				"repo add extra3 https://example.org/extra3");
	}
}
