package io.kokuwa.maven.helm;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
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

	@DisplayName("with overwriteLocalDependencies (throws invalid config)")
	@Test
	void overwriteLocalDependenciesMisconfig(DependencyBuildMojo mojo) {
		mojo.setChartDirectory(new File("src/test/resources/dependencies"));
		mojo.setOverwriteLocalDependencies(true);
		String message = assertThrows(MojoExecutionException.class, () -> mojo.execute()).getMessage();
		assertTrue(message.startsWith("Null value for 'overwriteDependencyRepository' "), message);
	}

	@DisplayName("with overwriteLocalDependencies and overwriteDependencyRepository")
	@Test
	void overwriteLocalDependencies(DependencyBuildMojo mojo) {
		mojo.setChartDirectory(new File("src/test/resources/dependencies"));
		mojo.setOverwriteLocalDependencies(true);
		mojo.setOverwriteDependencyRepository("fake.example.org");
		assertHelm(mojo,
				"dependency build src/test/resources/dependencies/b",
				"dependency build src/test/resources/dependencies/a2",
				"dependency build src/test/resources/dependencies/a1",
				"dependency build src/test/resources/dependencies");
	}

	@DisplayName("with flag skip repo refresh")
	@Test
	void skipRepoRefresh(DependencyBuildMojo mojo) {
		assertHelm(mojo.setSkipDependencyBuildRepoRefresh(true),
				"dependency build src/test/resources/simple --skip-refresh");
	}
}
