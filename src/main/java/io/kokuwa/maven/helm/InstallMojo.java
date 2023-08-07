package io.kokuwa.maven.helm;

import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import lombok.Setter;

/**
 * Mojo for executing "helm install".
 *
 * @author Tim IJntema
 * @see <a href="https://helm.sh/docs/helm/helm_install">helm install</a>
 * @since 5.10
 */
@Mojo(name = "install", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
@Setter
public class InstallMojo extends AbstractHelmWithValueOverrideMojo {

	/**
	 * Helm command to execute.
	 *
	 * @since 5.10
	 * @deprecated Will be removed in 7.x and set to "install".
	 */
	@Deprecated // java8 (since = "6.5.0", forRemoval = true)
	@Parameter(property = "action", defaultValue = "install")
	private String action;

	/**
	 * Set this to <code>true</code> to delete the installation on failure.
	 *
	 * @since 6.10.0
	 */
	@Parameter(property = "helm.install.atomic")
	private boolean installAtomic;

	/**
	 * Time to wait for any individual Kubernetes operation.
	 *
	 * @since 6.10.0
	 */
	@Parameter(property = "helm.install.timeout")
	private String installTimeout;

	/**
	 * Set this to <code>true</code> to skip invoking install goal.
	 *
	 * @since 5.10
	 */
	@Parameter(property = "helm.install.skip", defaultValue = "true")
	private boolean skipInstall;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if (skip || skipInstall) {
			getLog().info("Skip install");
			return;
		}

		for (Path chartDirectory : getChartDirectories()) {
			getLog().info(String.format("\n\nPerform install for chart %s...", chartDirectory) +
					(installAtomic ? " with atomic" : "") +
					(installTimeout != null ? String.format(" with timeout %s", installTimeout) : ""));
			helm()
					.arguments(action, chartDirectory.getFileName().toString(), chartDirectory)
					.flag("atomic", installAtomic)
					.flag("timeout", installTimeout)
					.execute("Failed to deploy helm chart");
		}
	}
}
