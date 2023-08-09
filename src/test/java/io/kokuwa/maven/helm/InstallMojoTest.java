package io.kokuwa.maven.helm;

import java.io.File;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.kokuwa.maven.helm.pojo.ValueOverride;

@DisplayName("helm:install")
public class InstallMojoTest extends AbstractMojoTest {

	@DisplayName("default values")
	@Test
	void install(InstallMojo mojo) {
		mojo.setSkipInstall(false);
		assertHelm(mojo, "install simple src/test/resources/simple");
	}

	@DisplayName("with flag skip")
	@Test
	void skip(InstallMojo mojo) {
		assertHelm(mojo.setSkipInstall(false).setSkip(true));
		assertHelm(mojo.setSkipInstall(true).setSkip(false));
		assertHelm(mojo.setSkipInstall(true).setSkip(true));
	}

	@DisplayName("with flag atomic")
	@Test
	void atomic(InstallMojo mojo) {
		mojo.setSkipInstall(false);
		mojo.setInstallAtomic(true);
		assertHelm(mojo, "install simple src/test/resources/simple --atomic");
	}

	@DisplayName("with flags atomic and timeout")
	@Test
	void atomicAndTimeout(InstallMojo mojo) {
		mojo.setSkipInstall(false);
		mojo.setInstallAtomic(true);
		mojo.setInstallTimeout(30);
		assertHelm(mojo, "install simple src/test/resources/simple --atomic --timeout 30s");
	}

	@DisplayName("with values overrides")
	@Test
	void valuesFile(InstallMojo mojo) {
		mojo.setSkipInstall(false);
		mojo.setValues(new ValueOverride().setYamlFile("bar.yaml"));
		assertHelm(mojo, "install simple src/test/resources/simple --values bar.yaml");
	}

	@DisplayName("with dependencies")
	@Test
	void dependencies(InstallMojo mojo) {
		mojo.setSkipInstall(false);
		mojo.setChartDirectory(new File("src/test/resources/dependencies"));
		assertHelm(mojo,
				"install b src/test/resources/dependencies/b",
				"install a2 src/test/resources/dependencies/a2",
				"install a1 src/test/resources/dependencies/a1",
				"install dependencies src/test/resources/dependencies");
	}
}
