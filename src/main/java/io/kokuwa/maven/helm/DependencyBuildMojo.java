package io.kokuwa.maven.helm;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import lombok.Setter;

/**
 * Mojo for building chart dependencies
 *
 * @author Axel Köhler
 * @since 1.1
 */
@Mojo(name = "dependency-build", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
@Setter
public class DependencyBuildMojo extends AbstractHelmMojo {

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
