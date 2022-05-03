package io.kokuwa.maven.helm;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import lombok.Setter;

/**
 * Mojo for simulate a template.
 *
 * @author Tim IJntema
 * @since 07.02.2021
 */
@Mojo(name = "install", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
@Setter
public class InstallMojo extends AbstractHelmWithValueOverrideMojo {

	@Parameter(property = "action", defaultValue = "install")
	private String action;

	@Parameter(property = "helm.install.skip", defaultValue = "true")
	private boolean skipInstall;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if (skip || skipInstall) {
			getLog().info("Skip install");
			return;
		}

		for (String inputDirectory : getChartDirectories(getChartDirectory())) {
			getLog().info(String.format("\n\nPerform install for chart %s...", inputDirectory));
			String clusterName = new File(inputDirectory).getName();
			callCli(String.format("%s %s %s %s %s %s %s %s",
					getHelmExecuteablePath(),
					action,
					clusterName,
					inputDirectory,
					formatIfValueIsNotEmpty("--registry-config=%s", getRegistryConfig()),
					formatIfValueIsNotEmpty("--repository-cache=%s", getRepositoryCache()),
					formatIfValueIsNotEmpty("--repository-config=%s", getRepositoryConfig()),
					getValuesOptions()),
					"Failed to deploy helm chart");
		}
	}
}
