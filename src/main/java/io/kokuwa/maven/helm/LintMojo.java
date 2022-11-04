package io.kokuwa.maven.helm;

import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import lombok.Setter;

/**
 * Mojo for executing "helm lint".
 *
 * @author Fabian Schlegel
 * @see <a href="https://helm.sh/docs/helm/helm_lint">helm lint</a>
 * @since 1.0
 */
@Mojo(name = "lint", defaultPhase = LifecyclePhase.TEST, threadSafe = true)
@Setter
public class LintMojo extends AbstractHelmWithValueOverrideMojo {

	/**
	 * Set this to <code>true</code> to skip invoking lint goal.
	 *
	 * @since 3.3
	 */
	@Parameter(property = "helm.lint.skip", defaultValue = "false")
	private boolean skipLint;

	/**
	 * Set this to <code>true</code> to fail on lint warnings.
	 *
	 * @since 5.0
	 */
	@Parameter(property = "helm.lint.strict", defaultValue = "false")
	private boolean lintStrict;

	/**
	 * Set this to <code>true</code> to print only warnings and errors.
	 *
	 * @since 6.6
	 */
	@Parameter(property = "helm.lint.quiet", defaultValue = "false")
	private boolean lintQuiet;

	@Override
	public void execute() throws MojoExecutionException {

		if (skip || skipLint) {
			getLog().info("Skip lint");
			return;
		}

		for (Path chartDirectory : getChartDirectories()) {
			getLog().info("\n\nTesting chart " + chartDirectory + "...");
			helm()
					.arguments("lint", chartDirectory)
					.flag("strict", lintStrict)
					.flag("quiet", lintQuiet)
					.execute("There are linting issues");
		}
	}
}
