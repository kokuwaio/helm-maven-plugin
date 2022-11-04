package io.kokuwa.maven.helm;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

	/**
	 * If <code>true</code> add timestamps to snapshots.
	 *
	 * @since 5.11
	 */
	@Parameter(property = "helm.chartVersion.timestampOnSnapshot", defaultValue = "false")
	private boolean timestampOnSnapshot;

	/**
	 * If "helm.chartVersion.timestampOnSnapshot" is <code>true</code> then use this format for timestamps.
	 *
	 * @since 5.11
	 */
	@Parameter(property = "helm.chartVersion.timestampFormat", defaultValue = "yyyyMMddHHmmss")
	private String timestampFormat;

	@Override
	public void execute() throws MojoExecutionException {

		if (skip || skipPackage) {
			getLog().info("Skip package");
			return;
		}

		for (Path chartDirectory : getChartDirectories()) {
			getLog().info("Packaging chart " + chartDirectory + "...");

			String arguments = "package " + chartDirectory + " --destination " + getOutputDirectory();

			String chartVersion = getChartVersion();
			if (chartVersion != null) {
				if (timestampOnSnapshot && chartVersion.endsWith("-SNAPSHOT")) {
					String suffix = DateTimeFormatter.ofPattern(timestampFormat).format(getTimestamp());
					chartVersion = chartVersion.replace("SNAPSHOT", suffix);
				}
				getLog().info("Setting chart version to " + chartVersion);
				arguments += " --version " + chartVersion;
			}

			if (appVersion != null) {
				getLog().info(String.format("Setting App version to %s", appVersion));
				arguments += " --app-version " + appVersion;
			}

			String stdin = null;
			if (StringUtils.isNotEmpty(keyring) && StringUtils.isNotEmpty(key)) {
				getLog().info("Enable signing");
				arguments += " --sign --keyring " + keyring + " --key " + key;
				if (StringUtils.isNotEmpty(passphrase)) {
					arguments += " --passphrase-file -";
					stdin = passphrase;
				}
			}

			helm(arguments, "Unable to package chart at " + chartDirectory, stdin);
		}
	}

	LocalDateTime getTimestamp() {
		return LocalDateTime.now(Clock.systemDefaultZone());
	}
}
