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
 * @since 14.11.17
 */
@Mojo(name = "dry-run", defaultPhase = LifecyclePhase.TEST, threadSafe = true)
@Setter
public class DryRunMojo extends AbstractHelmWithValueOverrideMojo {

	/**
	 * Helm command to execute.
	 *
	 * @deprecated Will be removed in 7.x and set to "template".
	 */
	@Deprecated // java8 (since = "6.5.0", forRemoval = true)
	@Parameter(property = "action", defaultValue = "install")
	private String action;

	/** Set this to `true` to skip invoking dry-run goal. */
	@Parameter(property = "helm.dry-run.skip", defaultValue = "false")
	private boolean skipDryRun;

	@Override
	public void execute() throws MojoExecutionException {

		if (skip || skipDryRun) {
			getLog().info("Skip dry run");
			return;
		}

		for (String inputDirectory : getChartDirectories(getChartDirectory())) {
			getLog().info("\n\nPerform dry-run for chart " + inputDirectory + "...");
			String arguments = action + " " + inputDirectory + " --dry-run --generate-name" + getValuesOptions();
			helm(arguments, "There are test failures");
		}
	}
}
