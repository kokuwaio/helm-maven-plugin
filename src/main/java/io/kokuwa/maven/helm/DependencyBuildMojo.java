package io.kokuwa.maven.helm;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import lombok.Setter;

/**
 * Mojo for executing "helm dependency-build".
 *
 * @author Axel Köhler
 * @see "https://helm.sh/docs/helm/helm_dependency_build"
 * @since 1.1
 */
@Mojo(name = "dependency-build", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, threadSafe = true)
@Setter
public class DependencyBuildMojo extends AbstractHelmMojo {

	/** Set this to `true` to skip invoking dependency-build goal. */
	@Parameter(property = "helm.dependency-build.skip", defaultValue = "false")
	private boolean skipDependencyBuild;

	@Override
	public void execute() throws MojoExecutionException {

		if (skip || skipDependencyBuild) {
			getLog().info("Skip dependency build");
			return;
		}

		for (String inputDirectory : getChartDirectories(getChartDirectory())) {
			getLog().info("Build chart dependencies for " + inputDirectory + "...");
			helm("dependency build " + inputDirectory, "Failed to resolve dependencies");
		}
	}
}
