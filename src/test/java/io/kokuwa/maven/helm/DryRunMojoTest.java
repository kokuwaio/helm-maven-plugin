package io.kokuwa.maven.helm;

import java.io.File;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.kokuwa.maven.helm.pojo.ValueOverride;

@DisplayName("helm:dry-run")
public class DryRunMojoTest extends AbstractMojoTest {

	@DisplayName("default values")
	@Test
	void DryRun(DryRunMojo mojo) {
		assertHelm(mojo, "install src/test/resources/simple --dry-run --generate-name");
	}

	@DisplayName("with flag skip")
	@Test
	void skip(DryRunMojo mojo) {
		assertHelm(mojo.setSkipDryRun(false).setSkip(true));
		assertHelm(mojo.setSkipDryRun(true).setSkip(false));
		assertHelm(mojo.setSkipDryRun(true).setSkip(true));
	}

	@DisplayName("with values overrides")
	@Test
	void valuesFile(DryRunMojo mojo) {
		mojo.setValues(new ValueOverride().setYamlFile("values.yaml"));
		assertHelm(mojo, "install src/test/resources/simple --dry-run --generate-name --values values.yaml");
	}

	@DisplayName("with dependencies")
	@Test
	void dependencies(DryRunMojo mojo) {
		mojo.setChartDirectory(new File("src/test/resources/dependencies"));
		assertHelm(mojo,
				"install src/test/resources/dependencies/b --dry-run --generate-name",
				"install src/test/resources/dependencies/a2 --dry-run --generate-name",
				"install src/test/resources/dependencies/a1 --dry-run --generate-name",
				"install src/test/resources/dependencies --dry-run --generate-name");
	}
}
