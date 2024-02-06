package io.kokuwa.maven.helm;

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
public class UpgradeMojo extends AbstractHandleMojo {

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
	private Integer upgradeTimeout;

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
	 * Force resource updates through a replacement strategy.
	 *
	 * @since 6.10.1
	 */
	@Parameter(property = "helm.upgrade.force")
	private boolean upgradeForce;

	/**
	 * Use insecure HTTP connections for the chart download.
	 *
	 * @since 6.12.0
	 */
	@Parameter(property = "helm.upgrade.plain-http")
	private Boolean upgradePlainHttp;

	@Override
	public void execute() throws MojoExecutionException {

		if (skip || skipUpgrade) {
			getLog().info("Skip upgrade");
			return;
		}

		for (Chart chart : getCharts()) {
			getLog().info("Upgrading the chart " + chart.getReleaseName() + " " +
					(upgradeWithInstall ? "with install " : "") +
					(upgradeAtomic ? "with atomic " : "") +
					(upgradeTimeout != null ? upgradeTimeout + "s" : "") +
					(upgradeDryRun ? "as dry run " : "") +
					chart.getDirectory());
			helm()
					.arguments("upgrade", chart.getReleaseName(), chart.getDirectory())
					.flag("install", upgradeWithInstall)
					.flag("dry-run", upgradeDryRun)
					.flag("atomic", upgradeAtomic)
					.flag("force", upgradeForce)
					.flag("plain-http", isPlainHttp(upgradePlainHttp))
					.flag("timeout", upgradeTimeout != null ? upgradeTimeout + "s" : null)
					.execute("Error occurred while upgrading the chart");
		}
	}
}
