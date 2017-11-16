package com.kiwigrid.core.k8deployment.helmplugin;

import java.util.Arrays;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Mojo for testing charts
 *
 * @author Fabian Schlegel
 * @since 06.11.17
 */
@Mojo(name = "lint", defaultPhase = LifecyclePhase.TEST)
public class LintMojo extends AbstractHelmMojo {

	public void execute()
			throws MojoExecutionException
	{
		for (String inputDirectory : getChartDirectories(getChartDirectory())) {
			if (getExcludes() != null && Arrays.asList(getExcludes()).contains(inputDirectory)) {
				getLog().debug("Skip excluded directory " + inputDirectory);
				continue;
			}
			getLog().info("\n\nTesting chart " + inputDirectory + "...");
			callCli(getHelmExecuteable()
					+ " lint "
					+ inputDirectory, "There are test failures", true);
		}
	}
}
