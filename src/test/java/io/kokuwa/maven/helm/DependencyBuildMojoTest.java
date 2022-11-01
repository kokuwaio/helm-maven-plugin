package io.kokuwa.maven.helm;

import java.io.File;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("helm:dependency-build")
public class DependencyBuildMojoTest extends AbstractMojoTest {

	@DisplayName("default values")
	@Test
	void DependencyBuild(DependencyBuildMojo mojo) {
		assertHelm(mojo, "dependency build src/test/resources/simple");
	}

	@DisplayName("with flag skip")
	@Test
	void skip(DependencyBuildMojo mojo) {
		assertHelm(mojo.setSkipDependencyBuild(false).setSkip(true));
		assertHelm(mojo.setSkipDependencyBuild(true).setSkip(false));
		assertHelm(mojo.setSkipDependencyBuild(true).setSkip(true));
	}

	@DisplayName("with dependencies")
	@Test
	void dependencies(DependencyBuildMojo mojo) {
		mojo.setChartDirectory(new File("src/test/resources/dependencies"));
		assertHelm(mojo,
				"dependency build src/test/resources/dependencies/b",
				"dependency build src/test/resources/dependencies/a2",
				"dependency build src/test/resources/dependencies/a1",
				"dependency build src/test/resources/dependencies");
	}
}
