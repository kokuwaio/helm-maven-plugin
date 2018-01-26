package com.kiwigrid.core.k8deployment.helmplugin;

import java.util.Arrays;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Mojo for building chart dependencies
 *
 * @author Axel Köhler
 * @since 1.1
 */
@Mojo(name = "dependency-build", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class DependencyBuildMojo extends AbstractHelmMojo {

	public void execute()
			throws MojoExecutionException
	{
		for (String inputDirectory : getChartDirectories(getChartDirectory())) {
			if(getExcludes() != null && Arrays.asList(getExcludes()).contains(inputDirectory)) {
				getLog().debug("Skip excluded directory " + inputDirectory);
				continue;
			}
			getLog().info("Build chart dependencies for " + inputDirectory + "...");
			callCli(getHelmExecuteablePath()
					+ " dependency build "
					+ inputDirectory, "Failed to resolve dependencies", true);
		}
	}
}
