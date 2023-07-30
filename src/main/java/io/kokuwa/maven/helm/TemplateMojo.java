package io.kokuwa.maven.helm;

import java.io.File;
import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

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
					.arguments("template", chartDirectory)
					.flag("output-dir", templateOutputDir)
					.flag("generate-name", templateGenerateName)
					.execute("There are test failures");
		}
	}
}
