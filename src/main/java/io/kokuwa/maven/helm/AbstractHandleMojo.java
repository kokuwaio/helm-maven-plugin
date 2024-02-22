package io.kokuwa.maven.helm;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import lombok.Setter;
import lombok.Value;

/** Base class for dependency build and update mojos. */
@Setter
public abstract class AbstractHandleMojo extends AbstractHelmWithValueOverrideMojo {

	/**
	 * Name of the release to handle.
	 *
	 * @since 6.4.0
	 */
	@Parameter(property = "helm.releaseName")
	private String releaseName;

	List<Chart> getCharts() throws MojoExecutionException {

		List<Path> chartDirectories = getChartDirectories();
		if (releaseName != null && chartDirectories.size() > 1) {
			throw new MojoExecutionException("For multiple charts releaseName is not supported.");
		}

		return chartDirectories.stream()
				.map(p -> new Chart(releaseName == null ? p.getFileName().toString() : releaseName, p))
				.collect(Collectors.toList());
	}

	@Value
	class Chart {
		private final String releaseName;
		private final Path directory;
	}
}
