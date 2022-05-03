package io.kokuwa.maven.helm;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

import lombok.Setter;

/**
 * Mojo for simulate a template.
 *
 * @author Tim IJntema, Kirill Nazarov
 * @since 07.02.2021
 */
@Mojo(name = "template", defaultPhase = LifecyclePhase.TEST, threadSafe = true)
@Setter
public class TemplateMojo extends AbstractHelmWithValueOverrideMojo {

	@Parameter(property = "action", defaultValue = "template")
	private String action;

	@Parameter(property = "helm.additional.arguments")
	private String additionalArguments;

	@Parameter(property = "helm.template.skip", defaultValue = "true")
	private boolean skipTemplate;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if (skip || skipTemplate) {
			getLog().info("Skip template");
			return;
		}

		for (String inputDirectory : getChartDirectories(getChartDirectory())) {
			getLog().info(String.format("\n\nPerform template for chart %s...", inputDirectory));
			callCli(String.format("%s %s %s %s %s %s %s %s",
					getHelmExecuteablePath(),
					action,
					inputDirectory,
					StringUtils.isNotEmpty(additionalArguments) ? additionalArguments : "",
					formatIfValueIsNotEmpty("--registry-config=%s", getRegistryConfig()),
					formatIfValueIsNotEmpty("--repository-cache=%s", getRepositoryCache()),
					formatIfValueIsNotEmpty("--repository-config=%s", getRepositoryConfig()),
					getValuesOptions()),
					"There are test failures");
		}
	}
}
