package io.kokuwa.maven.helm;

import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

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
public class DependencyBuildMojo extends AbstractDependencyMojo {

	/**
	 * Set this to <code>true</code> to skip invoking dependency-build goal.
	 *
	 * @since 3.3
	 */
	@Parameter(property = "helm.dependency-build.skip", defaultValue = "false")
	private boolean skipDependencyBuild;

	@Override
	public void execute() throws MojoExecutionException {

		if (skip || skipDependencyBuild) {
			getLog().info("Skip dependency build");
			return;
		}

		for (Path chartDirectory : getChartDirectories()) {

			doOverwriteLocalDependencies(chartDirectory);

			getLog().info("Build chart dependencies for " + chartDirectory + " ...");
			helm()
					.arguments("dependency", "build", chartDirectory)
					.execute("Failed to resolve dependencies");
		}
	}
}
