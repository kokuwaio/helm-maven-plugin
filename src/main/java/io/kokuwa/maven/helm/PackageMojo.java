package io.kokuwa.maven.helm;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

import lombok.Setter;

/**
 * Mojo for packaging charts
 *
 * @author Fabian Schlegel
 * @since 06.11.17
 */
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
@Setter
public class PackageMojo extends AbstractHelmMojo {

	@Parameter(property = "helm.package.skip", defaultValue = "false")
	private boolean skipPackage;

	@Parameter(property = "helm.package.keyring")
	private String keyring;

	@Parameter(property = "helm.package.key")
	private String key;

	@Parameter(property = "helm.package.passphrase")
	private String passphrase;

	@Override
	public void execute() throws MojoExecutionException {

		if (skip || skipPackage) {
			getLog().info("Skip package");
			return;
		}

		for (String inputDirectory : getChartDirectories(getChartDirectory())) {
			getLog().info("Packaging chart " + inputDirectory + "...");

			String arguments = "package " + inputDirectory + " -d " + getOutputDirectory();

			if (getChartVersion() != null) {
				getLog().info(String.format("Setting chart version to %s", getChartVersionWithProcessing()));
				arguments += " --version " + getChartVersionWithProcessing();
			}

			if (getAppVersion() != null) {
				getLog().info(String.format("Setting App version to %s", getAppVersion()));
				arguments += " --app-version " + getAppVersion();
			}

			String stdin = null;
			if (isSignEnabled()) {
				getLog().info("Enable signing");
				arguments += " --sign --keyring " + keyring + " --key " + key;
				if (StringUtils.isNotEmpty(passphrase)) {
					arguments += " --passphrase-file -";
					stdin = passphrase;
				}
			}

			helm(arguments, "Unable to package chart at " + inputDirectory, stdin);
		}
	}

	private boolean isSignEnabled() {
		return StringUtils.isNotEmpty(keyring) && StringUtils.isNotEmpty(key);
	}
}
