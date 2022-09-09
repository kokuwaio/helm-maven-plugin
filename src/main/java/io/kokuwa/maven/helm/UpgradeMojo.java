package io.kokuwa.maven.helm;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import lombok.Setter;

/**
 * Mojo for executing an Upgrade.
 */
@Mojo(name = "upgrade", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
@Setter
public class UpgradeMojo extends AbstractHelmWithValueOverrideMojo {

	@Parameter(property = "helm.upgrade.skip", defaultValue = "true")
	private boolean skipUpgrade;

	@Parameter(property = "helm.upgrade.upgradeWithInstall", defaultValue = "true")
	private boolean upgradeWithInstall;

	@Parameter(property = "helm.releaseName")
	private String releaseName;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip || skipUpgrade) {
			getLog().info("Skip upgrade");
			return;
		}

		for (String inputDirectory : getChartDirectories(getChartDirectory())) {
			getLog().info(new StringBuilder()
					.append("installing the chart ")
					.append(upgradeWithInstall ? "with install " : "")
					.append(inputDirectory)
					.toString());
			String arguments = "upgrade " + releaseName + " " + inputDirectory + " "
					+ (upgradeWithInstall ? "--install" : "") + getValuesOptions();
			helm(arguments, "Error happened during upgrading the chart");
		}
	}
}
