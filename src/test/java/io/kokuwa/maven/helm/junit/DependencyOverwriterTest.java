package io.kokuwa.maven.helm.junit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;

import io.kokuwa.maven.helm.util.DependencyOverwriter;

/**
 * Test functionality of utility class {@link DependencyOverwriter} against
 * many different scenarios
 */
public class DependencyOverwriterTest {

	private String overwriteDependencyRepository = "https://fake.repo/";

	private String overwriteDependencyVersion = "1.0.0";

	private Log logger = new SystemStreamLog();

	private String testResourcesPath = "src/test/resources/dependency-overwrite/";

	/**
	 * Tests overwriting in a chart with one dependency to overwrite
	 */
	@Test
	public void testBasicOverwrite() throws MojoExecutionException, IOException {
		File overwrittenChart = this.overWriteChart("basic", this.overwriteDependencyRepository, 
			this.overwriteDependencyVersion);
		File validationChart = new File(testResourcesPath + "validation/BasicTest.yaml");

		//verify the charts are the same
		assertTrue(FileUtils.contentEquals(overwrittenChart, validationChart),
			"Failed to overwrite Chart.yaml containing a single dependency");
	}

	/**
	 * Tests overwriting in a chart with multiple dependencies to overwrite
	 */
	@Test
	public void testMultiOverwrite() throws MojoExecutionException, IOException {
		File overwrittenChart = this.overWriteChart("multi", this.overwriteDependencyRepository, 
			this.overwriteDependencyVersion);
		File validationChart = new File(testResourcesPath + "validation/MultiTest.yaml");

		//verify the charts are the same
		assertTrue(FileUtils.contentEquals(overwrittenChart, validationChart),
			"Failed to overwrite Chart.yaml containing multiple dependencies");
	}

	/**
	 * Tests overwriting in a chart with no dependencies
	 */
	@Test
	public void testNoOverwrite() throws MojoExecutionException, IOException {
		File overwrittenChart = this.overWriteChart("none", this.overwriteDependencyRepository, 
			this.overwriteDependencyVersion);

		//verify no chart is generated since there are no dependencies to override
		assertFalse(overwrittenChart.isFile(), "Chart.yaml should not have been overwritten");
	}

	/**
	 * Tests overwriting in a chart with a dependency with a null repository
	 */
	@Test
	public void testNullRepoOverwrite() throws MojoExecutionException, IOException {
		File overwrittenChart = this.overWriteChart("null/repo", this.overwriteDependencyRepository, 
			this.overwriteDependencyVersion);
		File validationChart = new File(testResourcesPath + "validation/NullRepoTest.yaml");

		//verify the charts are the same
		assertTrue(FileUtils.contentEquals(overwrittenChart, validationChart),
			"Failed to overwrite Chart.yaml containing a dependency without a repository");
	}

	/**
	 * Tests overwriting in a chart when a version to overwrite is not provided
	 */
	@Test
	public void testNullVersionOverwrite() throws MojoExecutionException, IOException {
		File overwrittenChart = this.overWriteChart("null/version", this.overwriteDependencyRepository, null);
		File validationChart = new File(testResourcesPath + "validation/NullVersionTest.yaml");

		//verify the charts are the same
		assertTrue(FileUtils.contentEquals(overwrittenChart, validationChart),
			"Dependency version should remain the same when overwrite version is null");
	}

	/**
	 * Tests overwriting in a chart with a out of order dependencies
	 */
	@Test
	public void testOutOfOrderOverwrite() throws MojoExecutionException, IOException {
		File overwrittenChart = this.overWriteChart("out-of-order", this.overwriteDependencyRepository, 
			this.overwriteDependencyVersion);
		File validationChart = new File(testResourcesPath + "validation/OutOfOrderTest.yaml");

		//verify the charts are the same
		assertTrue(FileUtils.contentEquals(overwrittenChart, validationChart),
			"Failed to overwrite Chart.yaml containing out of order dependencies");
	}

	/**
	 * Tests overwriting in a chart with dependencies with extra attributes
	 */
	@Test
	public void testExtraDependencyAttributeOverwrite() throws MojoExecutionException, IOException {
		File overwrittenChart = this.overWriteChart("extra/dependency-attribute", this.overwriteDependencyRepository, 
			this.overwriteDependencyVersion);
		File validationChart = new File(testResourcesPath + "validation/ExtraDependencyAttribute.yaml");

		//verify the charts are the same
		assertTrue(FileUtils.contentEquals(overwrittenChart, validationChart),
			"Failed to overwrite Chart.yaml containing dependencies with extra attributes");
	}

	/**
	 * Tests overwriting in a chart with extra chart attributes after the dependencies
	 */
	@Test
	public void testExtraChartAttributeOverwrite() throws MojoExecutionException, IOException {
		File overwrittenChart = this.overWriteChart("extra/chart-attribute", this.overwriteDependencyRepository, 
			this.overwriteDependencyVersion);
		File validationChart = new File(testResourcesPath + "validation/ExtraChartAttribute.yaml");

		//verify the charts are the same
		assertTrue(FileUtils.contentEquals(overwrittenChart, validationChart),
			"Failed to overwrite Chart.yaml containing extra chart attributes");
	}

	/**
	 * Tests overwriting in a chart with dependencies with extra spacing
	 */
	@Test
	public void testExtraSpaceOverwrite() throws MojoExecutionException, IOException {
		File overwrittenChart = this.overWriteChart("extra/space", this.overwriteDependencyRepository, 
			this.overwriteDependencyVersion);
		File validationChart = new File(testResourcesPath + "validation/ExtraSpace.yaml");

		//verify the charts are the same
		assertTrue(FileUtils.contentEquals(overwrittenChart, validationChart),
			"Failed to overwrite Chart.yaml containing dependencies with extra spacing");
	}

	/**
	 * Tests overwriting in a chart with dependencies with extra text/comments
	 */
	@Test
	public void testExtraTextOverwrite() throws MojoExecutionException, IOException {
		File overwrittenChart = this.overWriteChart("extra/text", this.overwriteDependencyRepository, 
			this.overwriteDependencyVersion);
		File validationChart = new File(testResourcesPath + "validation/ExtraText.yaml");

		//verify the charts are the same
		assertTrue(FileUtils.contentEquals(overwrittenChart, validationChart),
			"Failed to overwrite Chart.yaml containing dependencies with extra text/comments");
	}

	/**
	 * Tests overwriting in a chart with dependencies with extra new lines
	 */
	@Test
	public void testExtraLinesOverwrite() throws MojoExecutionException, IOException {
		File overwrittenChart = this.overWriteChart("extra/line", this.overwriteDependencyRepository, 
			this.overwriteDependencyVersion);
		File validationChart = new File(testResourcesPath + "validation/ExtraLine.yaml");

		//verify the charts are the same
		assertTrue(FileUtils.contentEquals(overwrittenChart, validationChart),
			"Failed to overwrite Chart.yaml containing dependencies with extra new lines");
	}

	/**
	 * Helper function for executing {@link DependencyOverwriter}
	 * @param testName Used to find the Chart.yaml to overwrite
	 * @param overwriteRepository repo used to overwrite
	 * @param overwriteVersion version to overwrite
	 * @return {@link File} that was overwritten
	 * @throws MojoExecutionException
	 */
	private File overWriteChart(String testName, String overwriteRepository, String overwriteVersion) 
		throws MojoExecutionException {
		//read in the chart to be overwritten
		File chartDirectory = new File(testResourcesPath + "to-overwrite/" + testName + "/");

		//set the output directory for the overwritten chart
		File writeDirectory = new File(testResourcesPath + "to-overwrite/" + testName + "/target/");
		writeDirectory.mkdirs();

		//execute the overwriter utility
		DependencyOverwriter dependencyOverwriter = new DependencyOverwriter(overwriteRepository,
			overwriteVersion, this.logger);

		dependencyOverwriter.setWriteDirectory(writeDirectory);
		dependencyOverwriter.execute(Paths.get(chartDirectory.getAbsolutePath()));

		//get the newly overwritten chart and the validation chart
		return new File(writeDirectory.toString() + "/Chart.yaml");
	}
}
