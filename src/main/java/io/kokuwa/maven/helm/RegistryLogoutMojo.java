package io.kokuwa.maven.helm;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.kokuwa.maven.helm.pojo.HelmRepository;
import lombok.Setter;

/**
 * Mojo for executing "helm registry logout".
 *
 * @see <a href="https://helm.sh/docs/helm/helm_registry_logout">helm registry logout</a>
 * @since 6.7.0
 */
@Mojo(name = "registry-logout", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
@Setter
public class RegistryLogoutMojo extends AbstractHelmMojo {

	/**
	 * Set this to <code>true</code> to skip invoking registry-logout goal.
	 *
	 * @since 6.7.0
	 */
	@Parameter(property = "helm.registry-logout.skip", defaultValue = "false")
	private boolean skipRegistryLogout;

	@Override
	public void execute() throws MojoExecutionException {

		if (skip || skipRegistryLogout) {
			getLog().info("Skip registry logout");
			return;
		}

		HelmRepository repository = getHelmUploadRepo();
		if (repository == null) {
			getLog().warn("No upload repo found for logout, skipped.");
			return;
		}

		helm()
				.arguments("registry", "logout", repository.getUrl())
				.execute("Failed to logout from repository " + repository.getName() + " at " + repository.getUrl());
	}
}
