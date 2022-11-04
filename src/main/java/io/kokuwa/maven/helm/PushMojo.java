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
				getLog().error("your helm version is " + helmVersion + ", it's required to be >=3.8.0");
				throw new IllegalStateException();
			} else {
				getLog().debug("helm version minimum satisfied. the version is: " + helmVersion);
			}
		}

		PasswordAuthentication authentication = getAuthentication(registry);
		if (authentication != null) {
			helm()
					.arguments("registry", "login", registry.getUrl())
					.flag("username", authentication.getUserName())
					.flag("password-stdin")
					.setStdin(new String(authentication.getPassword()))
					.execute("can't login to registry");
		}

		// upload chart files

		for (Path chartArchive : getChartArchives()) {
			getLog().info("Uploading " + chartArchive + "...");
			helm()
					.arguments("push", chartArchive, "oci://" + registry.getUrl())
					.execute("Upload failed");
		}
	}
}
