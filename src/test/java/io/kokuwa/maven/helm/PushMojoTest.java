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

	@DisplayName("push simple")
	@Test
	void simple(PushMojo mojo) {
		Path packaged = copyPackagedHelmChartToOutputdirectory(mojo);
		mojo.setUploadRepoStable(new HelmRepository().setUrl("reg.example.org"));
		assertHelm(mojo, "push " + packaged + " oci://reg.example.org");
	}
}
