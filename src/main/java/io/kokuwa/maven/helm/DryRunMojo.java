package io.kokuwa.maven.helm;

import java.nio.file.Path;

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

		for (Path chartDirectory : getChartDirectories()) {
			getLog().info("\n\nPerform dry-run for chart " + chartDirectory + "...");
			helm()
					.arguments("install", chartDirectory)
					.flag("dry-run")
					.flag("generate-name")
					.execute("There are test failures");
		}
	}
}
