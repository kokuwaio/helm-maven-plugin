package io.kokuwa.maven.helm;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

/**
 * Mojo for testing charts
 *
 * @author Fabian Schlegel
 * @since 06.11.17
 */
@Mojo(name = "lint", defaultPhase = LifecyclePhase.TEST)
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
			callCli(getHelmExecuteablePath()
					+ " lint "
					+ inputDirectory
					+ (lintStrict ? " --strict" : "")
					+ (StringUtils.isNotEmpty(getRegistryConfig()) ? " --registry-config=" + getRegistryConfig() : "")
					+ (StringUtils.isNotEmpty(getRepositoryCache()) ? " --repository-cache=" + getRepositoryCache()
							: "")
					+ (StringUtils.isNotEmpty(getRepositoryConfig()) ? " --repository-config=" + getRepositoryConfig()
							: "")
					+ getValuesOptions(),
					"There are test failures", true);
		}
	}
}
