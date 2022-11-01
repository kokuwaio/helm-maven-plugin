package io.kokuwa.maven.helm;

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
		mojo.setChartDirectory("src/test/resources/dependencies");
		assertHelm(mojo,
				"install b src/test/resources/dependencies/b",
				"install a2 src/test/resources/dependencies/a2",
				"install a1 src/test/resources/dependencies/a1",
				"install dependencies src/test/resources/dependencies");
	}
}
