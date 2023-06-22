package io.kokuwa.maven.helm;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.kokuwa.maven.helm.pojo.HelmRepository;

@DisplayName("helm:push")
public class PushMojoTest extends AbstractMojoTest {

	@DisplayName("default values")
	@Test
	void push(PushMojo mojo) {
		assertHelm(mojo);
	}

	@DisplayName("with flag skip")
	@Test
	void skip(PushMojo mojo) {
		assertHelm(mojo.setSkipPush(false).setSkip(true));
		assertHelm(mojo.setSkipPush(true).setSkip(false));
		assertHelm(mojo.setSkipPush(true).setSkip(true));
	}

	@DisplayName("with flag insecure")
	@Test
	void insecure(PushMojo mojo) {
		Path packaged = copyPackagedHelmChartToOutputdirectory(mojo);
		mojo.setUploadRepoStable(new HelmRepository().setUrl("reg.example.org"));
		mojo.setInsecure(true);
		assertHelm(mojo, "push " + packaged + " oci://reg.example.org --insecure-skip-tls-verify");
	}

	@DisplayName("with flag ca-file")
	@Test
	void caFile(PushMojo mojo) {
		Path packaged = copyPackagedHelmChartToOutputdirectory(mojo);
		mojo.setUploadRepoStable(new HelmRepository().setUrl("reg.example.org"));
		mojo.setCaFile(new File("registry-ca.pem"));
		assertHelm(mojo, "push " + packaged + " oci://reg.example.org --ca-file registry-ca.pem");
	}

	@DisplayName("push without authentication")
	@Test
	void withoutAuthentication(PushMojo mojo) {
		Path packaged = copyPackagedHelmChartToOutputdirectory(mojo);
		mojo.setUploadRepoStable(new HelmRepository().setName("oci").setUrl("reg.example.org"));
		assertHelm(mojo, "push " + packaged + " oci://reg.example.org");
	}

	@DisplayName("push without authentication and skipped Login")
	@Test
	void withoutAuthenticationAndSkippedLogin(PushMojo mojo) {
		Path packaged = copyPackagedHelmChartToOutputdirectory(mojo);
		mojo.setSkipPushLogin(true);
		mojo.setUploadRepoStable(new HelmRepository().setName("oci").setUrl("reg.example.org"));
		assertHelm(mojo, "push " + packaged + " oci://reg.example.org");
	}

	@DisplayName("push with username/password and skipped Login")
	@Test
	void withUsernameAndPasswordAndSkippedLogin(PushMojo mojo) {
		Path packaged = copyPackagedHelmChartToOutputdirectory(mojo);
		mojo.setSkipPushLogin(true);
		mojo.setUploadRepoStable(new HelmRepository().setUrl("reg.example.org").setUsername("foo").setPassword("bar"));
		assertHelm(mojo, "push " + packaged + " oci://reg.example.org");
	}

	@DisplayName("push with username/password")
	@Test
	void withUsernameAndPassword(PushMojo mojo) {
		Path packaged = copyPackagedHelmChartToOutputdirectory(mojo);
		mojo.setUploadRepoStable(new HelmRepository().setUrl("reg.example.org").setUsername("foo").setPassword("bar"));
		assertHelm(mojo,
				"registry login reg.example.org --username foo --password-stdin",
				"push " + packaged + " oci://reg.example.org");
	}

	@DisplayName("push with serverId")
	@Test
	void withServerId(PushMojo mojo) {
		Path packaged = copyPackagedHelmChartToOutputdirectory(mojo);
		mojo.getSettings().getServers().add(getServer("oci", "foo", "secret"));
		mojo.setUploadRepoStable(new HelmRepository().setName("oci").setUrl("reg.example.org"));
		assertHelm(mojo,
				"registry login reg.example.org --username foo --password-stdin",
				"push " + packaged + " oci://reg.example.org");
	}

	@DisplayName("push with serverId")
	@Test
	void withServerIdEncrypted(PushMojo mojo) {
		Path packaged = copyPackagedHelmChartToOutputdirectory(mojo);
		mojo.getSettings().addServer(getServer("oci", "foo", SECRET_ENCRYPTED));
		mojo.setHelmSecurity(SETTINGS_SECURITY_XML);
		mojo.setUploadRepoStable(new HelmRepository().setName("oci").setUrl("reg.example.org"));
		assertHelm(mojo,
				"registry login reg.example.org --username foo --password-stdin",
				"push " + packaged + " oci://reg.example.org");
	}
}
