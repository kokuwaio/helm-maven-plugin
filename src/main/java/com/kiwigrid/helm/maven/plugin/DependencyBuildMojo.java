package com.kiwigrid.helm.maven.plugin;

import java.util.Arrays;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.StringUtils;

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
					+ inputDirectory
					+ (StringUtils.isNotEmpty(getHelmHomeDirectory()) ? " --home=" + getHelmHomeDirectory() : ""),
					"Failed to resolve dependencies", true);
		}
	}
}
