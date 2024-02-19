package io.kokuwa.maven.helm;

import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.kokuwa.maven.helm.pojo.HelmExecutable;
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

	/**
	 * Set this to <code>true</code> to skip refreshing the local repository cache when invoking dependency-build goal.
	 *
	 * @since 6.14.0
	 */
	@Parameter(property = "helm.dependency-build.skip-repo-refresh", defaultValue = "false")
	private boolean skipDependencyBuildRepoRefresh;

	@Override
	public void execute() throws MojoExecutionException {

		if (skip || skipDependencyBuild) {
			getLog().info("Skip dependency build");
			return;
		}

		for (Path chartDirectory : getChartDirectories()) {

			doOverwriteLocalDependencies(chartDirectory);

			getLog().info("Build chart dependencies for " + chartDirectory + " ...");
			HelmExecutable helm = helm()
					.arguments("dependency", "build", chartDirectory);
			if (skipDependencyBuildRepoRefresh) {
				helm.flag("skip-refresh");
			}
			helm.execute("Failed to resolve dependencies");
		}
	}
}
