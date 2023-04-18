package io.kokuwa.maven.helm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.kokuwa.maven.helm.pojo.HelmRepository;

@DisplayName("helm:registy-logout")
public class RepositoryLogoutMojoTest extends AbstractMojoTest {

	@DisplayName("default values")
	@Test
	void mojo(RegistryLogoutMojo mojo) {
		assertHelm(mojo);
	}

	@DisplayName("with flag skip")
	@Test
	void skip(RegistryLogoutMojo mojo) {
		assertHelm(mojo.setSkipRegistryLogout(false).setSkip(true));
		assertHelm(mojo.setSkipRegistryLogout(true).setSkip(false));
		assertHelm(mojo.setSkipRegistryLogout(true).setSkip(true));
	}

	@DisplayName("logout")
	@Test
	void withoutAuthentication(RegistryLogoutMojo mojo) {
		mojo.setUploadRepoStable(new HelmRepository().setUrl("docker.example.org"));
		assertHelm(mojo, "registry logout docker.example.org");
	}
}
