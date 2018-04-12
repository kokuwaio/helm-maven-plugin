package com.kiwigrid.helm.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

/**
 * Mojo for simulate a dry run.
 *
 * @author Axel Koehler
 * @since 14.11.17
 */
@Mojo(name = "dry-run", defaultPhase = LifecyclePhase.TEST)
public class DryRunMojo extends AbstractHelmMojo {

	@Parameter(property = "action", defaultValue = "install")
	private String action;

	public void execute() throws MojoExecutionException, MojoFailureException {
		for (String inputDirectory : getChartDirectories(getChartDirectory())) {
			getLog().info("\n\nPerform dry-run for chart " + inputDirectory + "...");

			callCli(getHelmExecuteablePath()
					+ " " + action
					+ " " + inputDirectory
					+ (StringUtils.isNotEmpty(getHelmHomeDirectory()) ? " --home=" + getHelmHomeDirectory() : ""),
					"There are test failures", true);
		}
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}
}
