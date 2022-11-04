package io.kokuwa.maven.helm;

import java.net.PasswordAuthentication;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.kokuwa.maven.helm.pojo.HelmRepository;
import lombok.Setter;

/**
 * Mojo for executing "helm registry login" and "helm push".
 *
 * @see <a href="https://helm.sh/docs/helm/helm_registry_login">helm registry login</a>
 * @see <a href="https://helm.sh/docs/helm/helm_push">helm push</a>
 * @since 6.1.0
 */
@Mojo(name = "push", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
@Setter
public class PushMojo extends AbstractHelmMojo {

	private static final String LOGIN_COMMAND_TEMPLATE = "registry login %s --username %s --password-stdin";
	private static final String CHART_PUSH_TEMPLATE = "push %s oci://%s";

	/**
	 * Set this to <code>true</code> to skip invoking push goal.
	 *
	 * @since 6.1.0
	 */
	@Parameter(property = "helm.push.skip", defaultValue = "false")
	private boolean skipPush;

	@Override
	public void execute() throws MojoExecutionException {

		if (skip || skipPush) {
			getLog().info("Skip push");
			return;
		}

		HelmRepository registry = getHelmUploadRepo();
		if (Objects.isNull(registry)) {
			getLog().info("there is no helm repo. skipping the upload.");
			return;
		}

		if (isUseLocalHelmBinary()) {
			getLog().debug("helm version unknown");
		} else {
			ComparableVersion helmVersion = new ComparableVersion(getHelmVersion());
			ComparableVersion minimumHelmVersion = new ComparableVersion("3.8.0");
			if (helmVersion.compareTo(minimumHelmVersion) < 0) {
				getLog().error("your helm version is " + helmVersion.toString() + ", it's required to be >=3.8.0");
				throw new IllegalStateException();
			} else {
				getLog().debug("helm version minimum satisfied. the version is: " + helmVersion);
			}
		}

		PasswordAuthentication authentication = getAuthentication(registry);
		if (authentication != null) {
			String arguments = String.format(LOGIN_COMMAND_TEMPLATE, registry.getUrl(), authentication.getUserName());
			helm(arguments, "can't login to registry", new String(authentication.getPassword()));
		}

		// upload chart files

		for (Path chart : getChartArchives()) {
			getLog().info("Uploading " + chart + "...");
			helm(String.format(CHART_PUSH_TEMPLATE, chart, registry.getUrl()), "Upload failed", null);
		}
	}
}
