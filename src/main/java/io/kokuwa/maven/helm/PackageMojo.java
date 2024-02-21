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

import io.kokuwa.maven.helm.pojo.HelmExecutable;
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
public class PackageMojo extends AbstractChartDirectoryMojo {

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

		String chartVersion = getChartVersion();
		if (chartVersion != null) {
			if (timestampOnSnapshot && chartVersion.endsWith("-SNAPSHOT")) {
				String suffix = DateTimeFormatter.ofPattern(timestampFormat).format(getTimestamp());
				chartVersion = chartVersion.replace("SNAPSHOT", suffix);
			}
			getLog().info("Setting chart version to " + chartVersion);
		}

		for (Path chartDirectory : getChartDirectories()) {
			getLog().info("Packaging chart " + chartDirectory + "...");

			HelmExecutable helm = helm()
					.arguments("package", chartDirectory)
					.flag("destination", getOutputDirectory())
					.flag("version", chartVersion)
					.flag("app-version", appVersion);

			if (StringUtils.isNotEmpty(keyring) && StringUtils.isNotEmpty(key)) {
				getLog().info("Enable signing");
				helm.flag("sign").flag("keyring", keyring).flag("key", key);
				if (StringUtils.isNotEmpty(passphrase)) {
					helm.flag("passphrase-file", "-").setStdin(passphrase);
				}
			}

			helm.execute("Unable to package chart at " + chartDirectory);
		}
	}

	LocalDateTime getTimestamp() {
		return LocalDateTime.now(Clock.systemDefaultZone());
	}
}
