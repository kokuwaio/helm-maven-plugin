package io.kokuwa.maven.helm;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

import lombok.Setter;

/**
 * Mojo for simulate a dry run.
 *
 * @author Axel Koehler
 * @since 14.11.17
 */
@Mojo(name = "dry-run", defaultPhase = LifecyclePhase.TEST)
@Setter
public class DryRunMojo extends AbstractHelmWithValueOverrideMojo {

	@Parameter(property = "action", defaultValue = "install")
	private String action;

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
			callCli(getHelmExecuteablePath()
					+ " " + action
					+ " " + inputDirectory
					+ " --dry-run --generate-name"
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
