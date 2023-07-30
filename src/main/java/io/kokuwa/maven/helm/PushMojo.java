package io.kokuwa.maven.helm;

import java.io.File;
import java.nio.file.Path;

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

	/**
	 * Verify certificates of HTTPS-enabled servers using this CA bundle.
	 *
	 * @since 6.8.0
	 */
	@Parameter(property = "helm.push.caFile")
	private File caFile;

	/**
	 * Skip tls certificate checks for the chart upload. Also known as `helm push --insecure-skip-tls-verify`.
	 *
	 * @since 6.8.0
	 */
	@Parameter(property = "helm.push.insecure", defaultValue = "false")
	private boolean insecure;

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

		HelmRepository repository = getHelmUploadRepo();
		if (repository == null) {
			getLog().info("there is no helm repo. skipping the upload.");
			return;
		}

		// upload chart files

		for (Path chartArchive : getChartArchives()) {
			getLog().info("Uploading " + chartArchive + "...");
			helm()
					.arguments("push", chartArchive, "oci://" + repository.getUrl())
					.flag("ca-file", caFile)
					.flag("insecure-skip-tls-verify", insecure)
					.execute("Upload failed");
		}
	}
}
