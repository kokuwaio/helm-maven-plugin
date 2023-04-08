package io.kokuwa.maven.helm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.kokuwa.maven.helm.pojo.HelmRepository;

@DisplayName("helm:registy-login")
public class RepositoryLoginMojoTest extends AbstractMojoTest {

	@DisplayName("default values")
	@Test
	void mojo(RegistryLoginMojo mojo) {
		assertHelm(mojo);
	}

	@DisplayName("with flag skip")
	@Test
	void skip(RegistryLoginMojo mojo) {
		assertHelm(mojo.setSkipRegistryLogin(false).setSkip(true));
		assertHelm(mojo.setSkipRegistryLogin(true).setSkip(false));
		assertHelm(mojo.setSkipRegistryLogin(true).setSkip(true));
	}

	@DisplayName("with flag insecure")
	@Test
	void insecure(RegistryLoginMojo mojo) {
		mojo.setUploadRepoStable(new HelmRepository().setUrl("reg.example.org").setUsername("foo").setPassword("bar"));
		mojo.setInsecure(true);
		assertHelm(mojo, "registry login reg.example.org --insecure --username foo --password-stdin");
	}

	@DisplayName("registry login without authentication")
	@Test
	void withoutAuthentication(RegistryLoginMojo mojo) {
		mojo.setUploadRepoStable(new HelmRepository().setUrl("docker.example.org"));
		assertHelm(mojo);
	}

	@DisplayName("registry login with username/password")
	@Test
	void withUsernameAndPassword(RegistryLoginMojo mojo) {
		mojo.setUploadRepoStable(new HelmRepository().setUrl("reg.example.org").setUsername("foo").setPassword("bar"));
		assertHelm(mojo, "registry login reg.example.org --username foo --password-stdin");
	}

	@DisplayName("registry login with serverId")
	@Test
	void withServerId(RegistryLoginMojo mojo) {
		mojo.getSettings().getServers().add(getServer("oci", "foo", "secret"));
		mojo.setUploadRepoStable(new HelmRepository().setName("oci").setUrl("reg.example.org"));
		assertHelm(mojo, "registry login reg.example.org --username foo --password-stdin");
	}

	@DisplayName("registry login with serverId")
	@Test
	void withServerIdEncrypted(RegistryLoginMojo mojo) {
		mojo.getSettings().addServer(getServer("oci", "foo", SECRET_ENCRYPTED));
		mojo.setHelmSecurity(SETTINGS_SECURITY_XML);
		mojo.setUploadRepoStable(new HelmRepository().setName("oci").setUrl("reg.example.org"));
		assertHelm(mojo, "registry login reg.example.org --username foo --password-stdin");
	}
}
