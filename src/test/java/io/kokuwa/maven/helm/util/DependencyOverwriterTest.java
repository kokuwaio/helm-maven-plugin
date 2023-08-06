package io.kokuwa.maven.helm.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test functionality of utility class {@link DependencyOverwriter} against many different scenarios.
 */
@DisplayName("util:dependency-overwriter")
public class DependencyOverwriterTest {

	private final String repository = "https://fake.example.org/";
	private final String version = "1.0.0";
	private final Log log = new SystemStreamLog();
	private final Path expectedPath = Paths.get("src/test/resources/dependency-overwrite/").toAbsolutePath();
	private final Path actualPath = Paths.get("target/dependency-overwrite/").toAbsolutePath();

	@DisplayName("with one dependency to overwrite")
	@Test
	void basicOverwrite() {
		Path actual = actual("basic", repository, version);
		Path expected = expectedPath.resolve("validation/BasicTest.yaml");
		// verify the charts are the same
		assertChart(expected, actual, "Failed to overwrite Chart.yaml containing a single dependency");
	}

	@DisplayName("with multiple dependencies to overwrite")
	@Test
	void multiOverwrite() {
		Path actual = actual("multi", repository, version);
		Path expected = expectedPath.resolve("validation/MultiTest.yaml");
		// verify the charts are the same
		assertChart(expected, actual, "Failed to overwrite Chart.yaml containing multiple dependencies");
	}

	@DisplayName("with no dependencies")
	@Test
	void noOverwrite() {
		Path actual = actual("none", repository, version);
		// verify no chart is generated since there are no dependencies to override
		assertFalse(Files.isRegularFile(actual), "Chart.yaml should not have been overwritten");
	}

	@DisplayName("with a dependency with a null repository")
	@Test
	void nullRepoOverwrite() {
		Path actual = actual("null/repo", repository, version);
		Path expected = expectedPath.resolve("validation/NullRepoTest.yaml");
		// verify the charts are the same
		assertChart(expected, actual, "Failed to overwrite Chart.yaml containing a dependency without a repository");
	}

	@DisplayName("when a version to overwrite is not provided")
	@Test
	void nullVersionOverwrite() {
		Path actual = actual("null/version", repository, null);
		Path expected = expectedPath.resolve("validation/NullVersionTest.yaml");
		// verify the charts are the same
		assertChart(expected, actual, "Dependency version should remain the same when overwrite version is null");
	}

	@DisplayName("with a out of order dependencies")
	@Test
	void outOfOrderOverwrite() {
		Path actual = actual("out-of-order", repository, version);
		Path expected = expectedPath.resolve("validation/OutOfOrderTest.yaml");
		// verify the charts are the same
		assertChart(expected, actual, "Failed to overwrite Chart.yaml containing out of order dependencies");
	}

	@DisplayName("with dependencies with extra attributes")
	@Test
	void extraDependencyAttributeOverwrite() {
		Path actual = actual("extra/dependency-attribute", repository, version);
		Path expected = expectedPath.resolve("validation/ExtraDependencyAttribute.yaml");
		// verify the charts are the same
		assertChart(expected, actual, "Failed to overwrite Chart.yaml containing dependencies with extra attributes");
	}

	@DisplayName("with extra chart attributes after the dependencies")
	@Test
	void extraChartAttributeOverwrite() {
		Path actual = actual("extra/chart-attribute", repository, version);
		Path expected = expectedPath.resolve("validation/ExtraChartAttribute.yaml");
		// verify the charts are the same
		assertChart(expected, actual, "Failed to overwrite Chart.yaml containing extra chart attributes");
	}

	@DisplayName("with dependencies with extra spacing")
	@Test
	void extraSpaceOverwrite() {
		Path actual = actual("extra/space", repository, version);
		Path expected = expectedPath.resolve("validation/ExtraSpace.yaml");
		// verify the charts are the same
		assertChart(expected, actual, "Failed to overwrite Chart.yaml containing dependencies with extra spacing");
	}

	@DisplayName("with dependencies with extra text/comments")
	@Test
	void extraTextOverwrite() {
		Path actual = actual("extra/text", repository, version);
		Path expected = expectedPath.resolve("validation/ExtraText.yaml");
		// verify the charts are the same
		assertChart(expected, actual,
				"Failed to overwrite Chart.yaml containing dependencies with extra text/comments");
	}

	@DisplayName("with dependencies with extra new lines")
	@Test
	void ExtraLinesOverwrite() {
		Path actual = actual("extra/line", repository, version);
		Path expected = expectedPath.resolve("validation/ExtraLine.yaml");
		// verify the charts are the same
		assertChart(expected, actual, "Failed to overwrite Chart.yaml containing dependencies with extra new lines");
	}

	/**
	 * Helper function for executing {@link DependencyOverwriter}
	 *
	 * @param test          Used to find the Chart.yaml to overwrite
	 * @param newRepository repo used to overwrite
	 * @param newVersion    version to overwrite
	 * @return {@link File} that was overwritten
	 * @throws MojoExecutionException
	 */
	private Path actual(String test, String newRepository, String newVersion) {

		Path sub = Paths.get("to-overwrite", test);
		// read in the chart to be overwritten
		Path chartDirectory = expectedPath.resolve(sub);
		// set the output directory for the overwritten chart
		Path writeDirectory = actualPath.resolve(sub);

		// execute the overwriter utility
		DependencyOverwriter dependencyOverwriter = new DependencyOverwriter(newRepository, newVersion, log);
		dependencyOverwriter.setWriteDirectory(writeDirectory);
		assertDoesNotThrow(() -> Files.createDirectories(writeDirectory));
		assertDoesNotThrow(() -> dependencyOverwriter.execute(chartDirectory));

		// get the newly overwritten chart and the validation chart
		return writeDirectory.resolve("Chart.yaml");
	}

	private void assertChart(Path expected, Path actual, String message) {
		try {
			String expectedChart = new String(Files.readAllBytes(expected), StandardCharsets.UTF_8);
			String actualChart = new String(Files.readAllBytes(actual), StandardCharsets.UTF_8);
			assertEquals(expectedChart, actualChart, message);
		} catch (IOException e) {
			fail(e);
		}
	}
}
