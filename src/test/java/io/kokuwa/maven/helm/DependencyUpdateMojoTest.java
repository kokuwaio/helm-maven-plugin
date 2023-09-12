package io.kokuwa.maven.helm;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("helm:dependency-update")
public class DependencyUpdateMojoTest extends AbstractMojoTest {

	@DisplayName("default values")
	@Test
	void DependencyUpdate(DependencyUpdateMojo mojo) {
		assertHelm(mojo, "dependency update src/test/resources/simple");
	}

	@DisplayName("with flag skip")
	@Test
	void skip(DependencyUpdateMojo mojo) {
		assertHelm(mojo.setSkipDependencyUpdate(false).setSkip(true));
		assertHelm(mojo.setSkipDependencyUpdate(true).setSkip(false));
		assertHelm(mojo.setSkipDependencyUpdate(true).setSkip(true));
	}

	@DisplayName("with dependencies")
	@Test
	void dependencies(DependencyUpdateMojo mojo) {
		mojo.setChartDirectory(new File("src/test/resources/dependencies"));
		assertHelm(mojo,
				"dependency update src/test/resources/dependencies/b",
				"dependency update src/test/resources/dependencies/a2",
				"dependency update src/test/resources/dependencies/a1",
				"dependency update src/test/resources/dependencies");
	}

	@DisplayName("with overwriteLocalDependencies (throws invalid config)")
	@Test
	void overwriteLocalDependenciesMisconfig(DependencyUpdateMojo mojo) {
		mojo.setChartDirectory(new File("src/test/resources/dependencies"));
		mojo.setOverwriteLocalDependencies(true);
		String message = assertThrows(MojoExecutionException.class, () -> mojo.execute()).getMessage();
		assertTrue(message.startsWith("Null value for 'overwriteDependencyRepository' "), message);
	}

	@DisplayName("with overwriteLocalDependencies and overwriteDependencyRepository")
	@Test
	void overwriteLocalDependencies(DependencyUpdateMojo mojo) {
		mojo.setChartDirectory(new File("src/test/resources/dependencies"));
		mojo.setOverwriteLocalDependencies(true);
		mojo.setOverwriteDependencyRepository("fake.example.org");
		assertHelm(mojo,
				"dependency update src/test/resources/dependencies/b",
				"dependency update src/test/resources/dependencies/a2",
				"dependency update src/test/resources/dependencies/a1",
				"dependency update src/test/resources/dependencies");
	}
}
