package io.kokuwa.maven.helm;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import lombok.Setter;

/**
 * Mojo for executing "helm uninstall".
 *
 * @author stephan.schnabel@posteo.de
 * @see <a href="https://helm.sh/docs/helm/helm_uninstall">helm uninstall</a>
 * @since 6.10.0
 */
@Mojo(name = "uninstall", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
@Setter
public class UninstallMojo extends AbstractHandleMojo {

	/**
	 * Must be "background", "orphan", or "foreground". Selects the deletion cascading strategy for the dependents.
	 * Defaults to background. (default "background" from helm)
	 *
	 * @since 6.10.0
	 */
	@Parameter(property = "helm.uninstall.cascade")
	private String uninstallCascade;

	/**
	 * Prevent hooks from running during uninstallation.
	 *
	 * @since 6.10.0
	 */
	@Parameter(property = "helm.uninstall.no-hooks", defaultValue = "false")
	private boolean uninstallNoHooks;

	/**
	 * Treat "release not found" as a successful uninstall.
	 *
	 * @since 6.14.0
	 */
	@Parameter(property = "helm.uninstall.ignore-not-found ", defaultValue = "false")
	private boolean uninstallIgnoreNotFound;

	/**
	 * Remove all associated resources and mark the release as deleted, but retain the release history.
	 *
	 * @since 6.10.0
	 */
	@Parameter(property = "helm.uninstall.keep-history ", defaultValue = "false")
	private boolean uninstallKeepHistory;

	/**
	 * If set, will wait until all the resources are deleted before returning. It will wait for as long
	 * as"uninstallTimeout".
	 *
	 * @since 6.10.0
	 */
	@Parameter(property = "helm.uninstall.wait ", defaultValue = "false")
	private boolean uninstallWait;

	/**
	 * Time to wait for any individual Kubernetes operation (like Jobs for hooks) (default 5m0s).
	 *
	 * @since 6.10.0
	 */
	@Parameter(property = "helm.uninstall.timeout")
	private Integer uninstallTimeout;

	/**
	 * Set this to <code>true</code> to skip invoking uninstall goal.
	 *
	 * @since 6.10
	 */
	@Parameter(property = "helm.uninstall.skip", defaultValue = "true")
	private boolean skipUninstall;

	@Override
	public void execute() throws MojoExecutionException {

		if (skip || skipUninstall) {
			getLog().info("Skip uninstall");
			return;
		}

		for (Chart charts : getCharts()) {
			getLog().info("Perform uninstall for chart with name " + charts.getReleaseName());
			helm().arguments("uninstall", charts.getReleaseName())
					.flag("wait", uninstallWait)
					.flag("timeout", uninstallTimeout != null ? uninstallTimeout + "s" : null)
					.flag("cascade", uninstallCascade)
					.flag("no-hooks", uninstallNoHooks)
					.flag("keep-history", uninstallKeepHistory)
					.flag("ignore-not-found", uninstallIgnoreNotFound)
					.execute("Failed to deploy helm chart");
		}
	}
}
