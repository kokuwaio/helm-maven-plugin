package com.kiwigrid.helm.maven.plugin;

import java.util.Arrays;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

/**
 * Mojo for packaging charts
 *
 * @author Fabian Schlegel
 * @since 06.11.17
 */
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE)
public class PackageMojo extends AbstractHelmMojo {

	@Parameter(property = "helm.package.skip", defaultValue = "false")
	private boolean skipPackage;

	public void execute()
			throws MojoExecutionException
	{
		if (skip || skipPackage) {
			getLog().info("Skip package");
			return;
		}

		for (String inputDirectory : getChartDirectories(getChartDirectory())) {
			if (getExcludes() != null && Arrays.asList(getExcludes()).contains(inputDirectory)) {
				getLog().debug("Skip excluded directory " + inputDirectory);
				continue;
			}
			getLog().info("Packaging chart " + inputDirectory + "...");

			String helmCommand = getHelmExecuteablePath()
					+ " package "
					+ inputDirectory
					+ " -d "
					+ getOutputDirectory()
					+ (StringUtils.isNotEmpty(getHelmHomeDirectory()) ? " --home=" + getHelmHomeDirectory() : "");

			if (getChartVersion() != null) {
				getLog().info(String.format("Setting chart version to %s", getChartVersion()));
				helmCommand = helmCommand + " --version " + getChartVersion();
			}

			if (getAppVersion() != null) {
				getLog().info(String.format("Setting App version to %s", getAppVersion()));
				helmCommand = helmCommand + " --app-version " + getAppVersion();
			}
			callCli(helmCommand, "Unable to package chart at " + inputDirectory, true);
		}
	}

}
