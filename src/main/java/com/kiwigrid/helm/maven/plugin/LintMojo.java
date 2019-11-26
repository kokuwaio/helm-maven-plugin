package com.kiwigrid.helm.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Mojo for testing charts
 *
 * @author Fabian Schlegel
 * @since 06.11.17
 */
@Mojo(name = "lint", defaultPhase = LifecyclePhase.TEST)
public class LintMojo extends AbstractHelmMojo {

	@Parameter(property = "helm.lint.skip", defaultValue = "false")
	private boolean skipLint;

	public void execute()
			throws MojoExecutionException
	{
		if (skip || skipLint) {
			getLog().info("Skip lint");
			return;
		}
		for (String inputDirectory : getChartDirectories(getChartDirectory())) {
			List<String> command = new ArrayList<>();
			command.add(getHelmExecuteablePath().toString());
			command.add("lint");
			command.add(inputDirectory);
			if (StringUtils.isNotEmpty(getHelmHomeDirectory())) command.add("--home=" + getHelmHomeDirectory());

			getLog().info("\n\nTesting chart " + inputDirectory + "...");
			callCli(command, "There are test failures", true);
		}
	}
}
