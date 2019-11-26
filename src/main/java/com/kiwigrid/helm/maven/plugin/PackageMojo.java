package com.kiwigrid.helm.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

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

			getLog().info("Packaging chart " + inputDirectory + "...");

			List<String> command = new ArrayList<>();
			command.add(getHelmExecuteablePath().toString());
			command.add("package");
			command.add(inputDirectory);
			command.add("--destination=" + getOutputDirectory());
			if (StringUtils.isNotEmpty(getHelmHomeDirectory())) command.add("--home=" + getHelmHomeDirectory());

			if (getChartVersion() != null) {
				getLog().info(String.format("Setting chart version to %s", getChartVersion()));
				command.add("--version=" + getChartVersion());
			}

			if (getAppVersion() != null) {
				getLog().info(String.format("Setting App version to %s", getAppVersion()));
				command.add("--app-version=" + getAppVersion());
			}

			callCli(command, "Unable to package chart at " + inputDirectory, true);
		}
	}

}
