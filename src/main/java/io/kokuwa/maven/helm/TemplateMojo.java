package io.kokuwa.maven.helm;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

import lombok.Setter;

/**
 * Mojo for executing "helm template".
 *
 * @author Tim IJntema, Kirill Nazarov
 * @see <a href="https://helm.sh/docs/helm/helm_template">helm template</a>
 * @since 5.10
 */
@Mojo(name = "template", defaultPhase = LifecyclePhase.TEST, threadSafe = true)
@Setter
public class TemplateMojo extends AbstractHelmWithValueOverrideMojo {

	/**
	 * Helm command to execute.
	 *
	 * @deprecated Will be removed in 7.x and set to "template".
	 */
	@Deprecated // java8 (since = "6.5.0", forRemoval = true)
	@Parameter(property = "action", defaultValue = "template")
	private String action;

	/**
	 * Additional arguments.
	 *
	 * @deprecated Will be removed in 7.x and use "helm.values".
	 */
	@Deprecated // java8 (since = "6.5.0", forRemoval = true)
	@Parameter(property = "helm.additional.arguments")
	private String additionalArguments;

	/** Set this to <code>true</code> to skip invoking template goal. */
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
