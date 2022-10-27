package io.kokuwa.maven.helm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.kokuwa.maven.helm.pojo.ValueOverride;

@DisplayName("helm:lint")
public class LintMojoTest extends AbstractMojoTest {

	@DisplayName("default values")
	@Test
	void lint(LintMojo mojo) {
		assertHelm(mojo, "lint src/test/resources/simple");
	}

	@DisplayName("with flag skip")
	@Test
	void skip(LintMojo mojo) {
		assertHelm(mojo.setSkipLint(false).setSkip(true));
		assertHelm(mojo.setSkipLint(true).setSkip(false));
		assertHelm(mojo.setSkipLint(true).setSkip(true));
	}

	@DisplayName("with flag strict")
	@Test
	void strict(LintMojo mojo) {
		mojo.setLintStrict(true);
		assertHelm(mojo, "lint src/test/resources/simple --strict");
	}

	@DisplayName("with values file")
	@Test
	void overrideFile(LintMojo mojo) {
		mojo.setValues(new ValueOverride().setYamlFile("values.yaml"));
		assertHelm(mojo, "lint src/test/resources/simple --values values.yaml");
	}

	@DisplayName("with dependencies")
	@Test
	void dependencies(LintMojo mojo) {
		mojo.setChartDirectory("src/test/resources/dependencies");
		assertHelm(mojo,
				"lint src/test/resources/dependencies/b",
				"lint src/test/resources/dependencies/a2",
				"lint src/test/resources/dependencies/a1",
				"lint src/test/resources/dependencies");
	}
}
