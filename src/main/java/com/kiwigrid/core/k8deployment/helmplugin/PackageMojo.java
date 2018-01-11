package com.kiwigrid.core.k8deployment.helmplugin;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Mojo for packaging charts
 *
 * @author Fabian Schlegel
 * @since 06.11.17
 */
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE)
public class PackageMojo extends AbstractHelmMojo {

	private static final String CHART_YAML = "Chart.yaml";
	/**
	 * Regular expression to check if a given string is in the {@see https://semver.org/} format.
	 */
	private static final String SEMVER_REGEX = "(\\d+)\\.(\\d+)(?:\\.)?(\\d*)(\\.|-|\\+)?([0-9A-Za-z-.]*)?";

	public void execute()
			throws MojoExecutionException
	{

		for (String inputDirectory : getChartDirectories(getChartDirectory())) {
			if (getExcludes() != null && Arrays.asList(getExcludes()).contains(inputDirectory)) {
				getLog().debug("Skip excluded directory " + inputDirectory);
				continue;
			}
			getLog().info("Packaging chart " + inputDirectory + "...");

			String helmCommand = getHelmExecuteable()
					+ " package "
					+ inputDirectory
					+ " -d "
					+ getOutputDirectory();

			if (getChartVersion() != null) {
				getLog().info(String.format("Setting chart version to %s", getChartVersion()));
				helmCommand = helmCommand + " --version " + getChartVersion();
			}

			callCli(helmCommand, "Unable to package chart at " + inputDirectory, true);
		}
	}

}
