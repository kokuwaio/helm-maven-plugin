package io.kokuwa.maven.helm;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

import lombok.Setter;

/**
 * Mojo for executing "helm package".
 *
 * @author Fabian Schlegel
 * @see <a href="https://helm.sh/docs/helm/helm_package">helm package</a>
 * @since 1.0
 */
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
@Setter
public class PackageMojo extends AbstractHelmMojo {

	/**
	 * Set this to <code>true</code> to skip invoking package goal.
	 *
	 * @since 3.3
	 */
	@Parameter(property = "helm.package.skip", defaultValue = "false")
	private boolean skipPackage;

	/**
	 * Path to gpg secret keyring for signing.
	 *
	 * @since 5.10
	 */
	@Parameter(property = "helm.package.keyring")
	private String keyring;

	/**
	 * Name of gpg key in keyring.
	 *
	 * @since 5.10
	 */
	@Parameter(property = "helm.package.key")
	private String key;

	/**
	 * Passphrase for gpg key (requires helm 3.4 or newer).
	 *
	 * @since 5.10
	 */
	@Parameter(property = "helm.package.passphrase")
	private String passphrase;

	/**
	 * The version of the app. This needn't be SemVer.
	 *
	 * @since 2.8
	 */
	@Parameter(property = "helm.appVersion")
	private String appVersion;

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

			if (appVersion != null) {
				getLog().info(String.format("Setting App version to %s", appVersion));
				arguments += " --app-version " + appVersion;
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
