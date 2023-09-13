package io.kokuwa.maven.helm;

import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import lombok.Setter;

/**
 * Mojo for executing "helm dependency-update".
 *
 * @see <a href="https://helm.sh/docs/helm/helm_dependency_update/">helm dependency-update</a>
 * @since 6.10.2
 */
@Mojo(name = "dependency-update", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, threadSafe = true)
@Setter
public class DependencyUpdateMojo extends AbstractDependencyMojo {

	/**
	 * Set this to <code>true</code> to skip invoking dependency-update goal.
	 *
	 * @since 6.10.2
	 */
	@Parameter(property = "helm.dependency-update.skip", defaultValue = "false")
	private boolean skipDependencyUpdate;

	@Override
	public void execute() throws MojoExecutionException {

		if (skip || skipDependencyUpdate) {
			getLog().info("Skip dependency update");
			return;
		}

		for (Path chartDirectory : getChartDirectories()) {

			doOverwriteLocalDependencies(chartDirectory);

			getLog().info("Updating chart dependencies for " + chartDirectory + " ...");
			helm()
					.arguments("dependency", "update", chartDirectory)
					.execute("Failed to resolve dependencies");
		}
	}
}
