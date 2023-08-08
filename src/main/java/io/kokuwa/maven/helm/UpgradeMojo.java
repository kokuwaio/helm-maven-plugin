package io.kokuwa.maven.helm;

import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import lombok.Setter;

/**
 * Mojo for executing "helm upgrade".
 *
 * @see <a href="https://helm.sh/docs/helm/helm_upgrade">helm upgrade</a>
 * @since 6.4.0
 */
@Mojo(name = "upgrade", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
@Setter
public class UpgradeMojo extends AbstractHelmWithValueOverrideMojo {

	/**
	 * Set this to <code>true</code> to skip invoking upgrade goal.
	 *
	 * @since 6.4.0
	 */
	@Parameter(property = "helm.upgrade.skip", defaultValue = "true")
	private boolean skipUpgrade;

	/**
	 * Set this to <code>true</code> to rollback changes made in case of failed upgrade.
	 *
	 * @since 6.10.0
	 */
	@Parameter(property = "helm.upgrade.atomic")
	private boolean upgradeAtomic;

	/**
	 * Time in seconds to wait for any individual Kubernetes operation.
	 *
	 * @since 6.10.0
	 */
	@Parameter(property = "helm.upgrade.timeout")
	private int upgradeTimeout;

	/**
	 * Upgrade with install parameter.
	 *
	 * @since 6.4.0
	 */
	@Parameter(property = "helm.upgrade.upgradeWithInstall", defaultValue = "true")
	private boolean upgradeWithInstall;

	/**
	 * Run upgrade goal only in dry run mode.
	 *
	 * @since 6.4.0
	 */
	@Parameter(property = "helm.upgrade.dryRun", defaultValue = "false")
	private boolean upgradeDryRun;

	/**
	 * Name of the release for upgrade goal.
	 *
	 * @since 6.4.0
	 */
	@Parameter(property = "helm.releaseName")
	private String releaseName;

	@Override
	public void execute() throws MojoExecutionException {

		if (skip || skipUpgrade) {
			getLog().info("Skip upgrade");
			return;
		}

		for (Path chartDirectory : getChartDirectories()) {
			getLog().info("Upgrading the chart " +
					(upgradeWithInstall ? "with install " : "") +
					(upgradeAtomic ? "with atomic " : "") +
					(upgradeTimeout > 0 ? String.format("timeout %s ", upgradeTimeout) : "") +
					(upgradeDryRun ? "as dry run " : "") +
					chartDirectory);
			helm()
					.arguments("upgrade", releaseName, chartDirectory)
					.flag("install", upgradeWithInstall)
					.flag("dry-run", upgradeDryRun)
					.flag("atomic", upgradeAtomic)
					.flag("timeout", upgradeTimeout > 0 ? upgradeTimeout + "s" : null)
					.execute("Error occurred while upgrading the chart");
		}
	}
}
