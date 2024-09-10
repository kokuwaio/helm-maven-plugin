package io.kokuwa.maven.helm;

import java.io.File;
import java.nio.file.Path;

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
	 * @since 5.10
	 * @deprecated Will be removed in 7.x and set to "template".
	 */
	@Deprecated // java8 (since = "6.5.0", forRemoval = true)
	@Parameter(property = "action", defaultValue = "template")
	private String action;

	/**
	 * Specify template used to name the release.
	 *
	 * @since 6.15
	 */
	@Parameter(property = "helm.template.name-template")
	private String templateNameTemplate;

	/**
	 * Writes the executed templates to files in output-dir instead of stdout.
	 *
	 * @since 6.6.1
	 */
	@Parameter(property = "helm.template.output-dir")
	private File templateOutputDir;

	/**
	 * Generate the name (and omit the NAME parameter).
	 *
	 * @since 6.6.1
	 */
	@Parameter(property = "helm.template.generate-name", defaultValue = "false")
	private boolean templateGenerateName;

	/**
	 * Additional arguments.
	 *
	 * @since 5.10
	 * @deprecated Will be removed in 7.x and use "helm.values".
	 */
	@Deprecated // java8 (since = "6.5.0", forRemoval = true)
	@Parameter(property = "helm.additional.arguments")
	private String additionalArguments;

	/**
	 * Use insecure HTTP connections for the chart download.
	 *
	 * @since 6.12.0
	 */
	@Parameter(property = "helm.template.plain-http")
	private Boolean templatePlainHttp;

	/**
	 * Set this to <code>true</code> to skip invoking template goal.
	 *
	 * @since 5.10
	 */
	@Parameter(property = "helm.template.skip", defaultValue = "true")
	private boolean skipTemplate;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if (skip || skipTemplate) {
			getLog().info("Skip template");
			return;
		}

		for (Path chartDirectory : getChartDirectories()) {
			getLog().info(String.format("\n\nPerform template for chart %s...", chartDirectory));
			helm()
					.arguments(action, chartDirectory)
					.arguments(getArguments())
					.flag("output-dir", templateOutputDir)
					.flag("name-template", templateNameTemplate)
					.flag("generate-name", templateGenerateName)
					.flag("plain-http", isPlainHttp(templatePlainHttp))
					.execute("There are test failures");
		}
	}

	private Object[] getArguments() {
		if (StringUtils.isEmpty(additionalArguments)) {
			return new Object[0];
		}
		getLog().warn("NOTE: <additionalArguments> option will be removed in future major release.");
		return additionalArguments.split(" ");
	}
}
