package io.kokuwa.maven.helm;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.MatchPatterns;

import lombok.Getter;
import lombok.Setter;

/**
 * Base Mojo for operating on Chart directory.
 */
@Getter
@Setter
public abstract class AbstractChartDirectoryMojo extends AbstractMojo {

	/**
	 * Set this to <code>true</code> to skip all goals.
	 *
	 * @since 3.2
	 */
	@Parameter(property = "helm.skip", defaultValue = "false")
	protected boolean skip;

	/**
	 * List of chart directories to exclude.
	 *
	 * @since 1.0
	 */
	@Parameter(property = "helm.excludes")
	private String[] excludes;

	/**
	 * Root directory of your charts.
	 *
	 * @since 1.0
	 */
	@Parameter(property = "helm.chartDirectory", required = true)
	private File chartDirectory;

	List<Path> getChartDirectories() throws MojoExecutionException {

		List<String> exclusions = new ArrayList<>();
		if (excludes != null) {
			exclusions.addAll(Arrays.asList(excludes));
		}
		exclusions.addAll(FileUtils.getDefaultExcludesAsList());
		MatchPatterns exclusionPatterns = MatchPatterns.from(exclusions);

		try (Stream<Path> files = Files.walk(chartDirectory.toPath(), FileVisitOption.FOLLOW_LINKS)) {
			List<Path> chartDirectories = files
					.filter(p -> p.getFileName().toString().equalsIgnoreCase("chart.yaml"))
					.map(Path::getParent)
					.filter(p -> !exclusionPatterns.matches(p.toString(), false))
					.sorted(Comparator.reverseOrder())
					.collect(Collectors.toList());
			if (chartDirectories.isEmpty()) {
				getLog().warn("No Charts detected - no Chart.yaml files found below " + chartDirectory);
			}
			return chartDirectories;
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to scan chart directory at " + chartDirectory, e);
		}
	}
}
