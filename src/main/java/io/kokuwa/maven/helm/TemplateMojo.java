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
@Mojo(name = "template", defaultPhase = LifecyclePhase.TEST)
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
			helm(String.format("%s %s %s %s",
					action,
					inputDirectory,
					StringUtils.isNotEmpty(additionalArguments) ? additionalArguments : "",
					getValuesOptions()),
					"There are test failures");
		}
	}
}
