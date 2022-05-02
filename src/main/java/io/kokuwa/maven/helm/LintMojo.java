package io.kokuwa.maven.helm;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import lombok.Setter;

/**
 * Mojo for testing charts
 *
 * @author Fabian Schlegel
 * @since 06.11.17
 */
@Mojo(name = "lint", defaultPhase = LifecyclePhase.TEST)
@Setter
public class LintMojo extends AbstractHelmWithValueOverrideMojo {

	@Parameter(property = "helm.lint.skip", defaultValue = "false")
	private boolean skipLint;

	@Parameter(property = "helm.lint.strict", defaultValue = "false")
	private boolean lintStrict;

	@Override
	public void execute() throws MojoExecutionException {

		if (skip || skipLint) {
			getLog().info("Skip lint");
			return;
		}

		for (String inputDirectory : getChartDirectories(getChartDirectory())) {
			getLog().info("\n\nTesting chart " + inputDirectory + "...");
			String arguments = "lint " + inputDirectory + (lintStrict ? " --strict" : "") + getValuesOptions();
			helm(arguments, "There are test failures");
		}
	}
}
