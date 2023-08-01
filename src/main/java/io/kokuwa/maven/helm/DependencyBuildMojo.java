package io.kokuwa.maven.helm;

import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.kokuwa.maven.helm.util.DependencyOverwriter;
import lombok.Setter;

/**
 * Mojo for executing "helm dependency-build".
 *
 * @author Axel Köhler
 * @see <a href="https://helm.sh/docs/helm/helm_dependency_build">helm dependency-build</a>
 * @since 1.1
 */
@Mojo(name = "dependency-build", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, threadSafe = true)
@Setter
public class DependencyBuildMojo extends AbstractHelmMojo {

	/**
	 * Set this to <code>true</code> to skip invoking dependency-build goal.
	 *
	 * @since 3.3
	 */
	@Parameter(property = "helm.dependency-build.skip", defaultValue = "false")
	private boolean skipDependencyBuild;

	/**
	 * Controls whether a local path chart should be used for a chart dependency. When set to <code>true</code>,
	 * chart dependencies on a local path chart will be overwritten with the respective properties set by
	 * "helm.overwriteDependencyVersion" and "helm.overwriteDependencyRepository". This is helpful for deploying charts 
	 * with intra repository dependencies, while still being able to use local path dependencies for development builds.
	 *
	 * Example usage:
	 * 	For development: mvn clean install
	 * 	For deployment: mvn clean deploy -Dhelm.overwriteLocalDependencies=true
	 * @since 6.9.1
	 */
	@Parameter(property = "helm.overwriteLocalDependencies", defaultValue = "false")
	private boolean overwriteLocalDependencies;

	/**
	 * Value used to overwrite a local path chart's version within a chart's dependencies. The property 
	 * "helm.overwriteLocalDependencies" must be set to <code>true</code> for this to apply.
	 *
	 * @since 6.9.1
	 */
	@Parameter(property = "helm.overwriteDependencyVersion")
	private String overwriteDependencyVersion;

	/**
	 * Value used to overwrite a local path chart's repository within a chart's dependencies. The property 
	 * "helm.overwriteLocalDependencies" must be set to <code>true</code> for this to apply.
	 *
	 * @since 6.9.1
	 */
	@Parameter(property = "helm.overwriteDependencyRepository")
	private String overwriteDependencyRepository;

	@Override
	public void execute() throws MojoExecutionException {

		if (skip || skipDependencyBuild) {
			getLog().info("Skip dependency build");
			return;
		}

		for (Path chartDirectory : getChartDirectories()) {
			if (overwriteLocalDependencies) {
				DependencyOverwriter dependencyOverwriter = 
					new DependencyOverwriter(overwriteDependencyRepository, overwriteDependencyVersion, getLog());
				dependencyOverwriter.execute(chartDirectory);
			}

			getLog().info("Build chart dependencies for " + chartDirectory + " ...");
			helm()
					.arguments("dependency", "build", chartDirectory)
					.execute("Failed to resolve dependencies");
		}
	}
}
