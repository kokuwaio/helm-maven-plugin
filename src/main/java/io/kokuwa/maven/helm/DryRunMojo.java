package io.kokuwa.maven.helm;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import lombok.Setter;

/**
 * Mojo for executing "helm install --dry-run".
 *
 * @author Axel Koehler
 * @since 1.0
 */
@Mojo(name = "dry-run", defaultPhase = LifecyclePhase.TEST, threadSafe = true)
@Setter
public class DryRunMojo extends AbstractHelmWithValueOverrideMojo {

	/**
	 * Helm command to execute.
	 *
	 * @since 1.0
	 * @deprecated Will be removed in 7.x and set to "template".
	 */
	@Deprecated // java8 (since = "6.5.0", forRemoval = true)
	@Parameter(property = "action", defaultValue = "install")
	private String action;

	/**
	 * Set this to <code>true</code> to skip invoking dry-run goal.
	 *
	 * @since 3.3
	 */
	@Parameter(property = "helm.dry-run.skip", defaultValue = "false")
	private boolean skipDryRun;

	@Override
	public void execute() throws MojoExecutionException {

		if (skip || skipDryRun) {
			getLog().info("Skip dry run");
			return;
		}

		for (String chartDirectory : getChartDirectories()) {
			getLog().info("\n\nPerform dry-run for chart " + chartDirectory + "...");
			String arguments = action + " " + chartDirectory + " --dry-run --generate-name" + getValuesOptions();
			helm(arguments, "There are test failures", null);
		}
	}
}
