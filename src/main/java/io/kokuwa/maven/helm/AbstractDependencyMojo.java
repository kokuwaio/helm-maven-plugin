package io.kokuwa.maven.helm;

import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import io.kokuwa.maven.helm.util.DependencyOverwriter;
import lombok.Setter;

/** Base class for dependency build and update mojos. */
@Setter
public abstract class AbstractDependencyMojo extends AbstractChartDirectoryMojo {

	/**
	 * Controls whether a local path chart should be used for a chart dependency. When set to <code>true</code>, chart
	 * dependencies on a local path chart will be overwritten with the respective properties set by
	 * "helm.overwriteDependencyVersion" and "helm.overwriteDependencyRepository". This is helpful for deploying charts
	 * with intra repository dependencies, while still being able to use local path dependencies for development builds.
	 *
	 * Example usage:
	 * <ul>
	 * <li>For development: mvn clean install</li>
	 * <li>For deployment: mvn clean deploy -Dhelm.overwriteLocalDependencies=true</li>
	 * </ul>
	 *
	 * @since 6.10.0
	 */
	@Parameter(property = "helm.overwriteLocalDependencies", defaultValue = "false")
	protected boolean overwriteLocalDependencies;

	/**
	 * Value used to overwrite a local path chart's version within a chart's dependencies. The property
	 * "helm.overwriteLocalDependencies" must be set to <code>true</code> for this to apply.
	 *
	 * @since 6.10.0
	 */
	@Parameter(property = "helm.overwriteDependencyVersion")
	protected String overwriteDependencyVersion;

	/**
	 * Value used to overwrite a local path chart's repository within a chart's dependencies. The property
	 * "helm.overwriteLocalDependencies" must be set to <code>true</code> for this to apply.
	 *
	 * @since 6.10.0
	 */
	@Parameter(property = "helm.overwriteDependencyRepository")
	protected String overwriteDependencyRepository;

	/**
	 * Overwrites the local path of a chart dependency with the desired repository and version when
	 * helm.overwriteLocalDependencies is set to <code>true</code>.
	 *
	 * @param chartDirectory directory containing a Helm chart
	 * @throws MojoExecutionException Null value for 'overwriteDependencyRepository'
	 *
	 * @since 6.11.0
	 */
	protected void doOverwriteLocalDependencies(Path chartDirectory) throws MojoExecutionException {
		if (overwriteLocalDependencies) {
			if (overwriteDependencyRepository == null) {
				throw new MojoExecutionException("Null value for 'overwriteDependencyRepository' is " +
						"not allowed when using 'overwriteLocalDependencies'. See the README for more details.");
			}
			getLog().info("Overwriting dependencies that contain local path charts with "
					+ overwriteDependencyRepository);
			new DependencyOverwriter(overwriteDependencyRepository, overwriteDependencyVersion, getLog())
					.execute(chartDirectory);
		}
	}
}
