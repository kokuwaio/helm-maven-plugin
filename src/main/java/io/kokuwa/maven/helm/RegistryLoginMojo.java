package io.kokuwa.maven.helm;

import java.net.PasswordAuthentication;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.kokuwa.maven.helm.pojo.HelmRepository;
import lombok.Setter;

/**
 * Mojo for executing "helm registry login".
 *
 * @see <a href="https://helm.sh/docs/helm/helm_registry_login">helm registry login</a>
 * @since 6.7.0
 */
@Mojo(name = "registry-login", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
@Setter
public class RegistryLoginMojo extends AbstractHelmMojo {

	/**
	 * Allow connections to TLS registry without certs.
	 *
	 * @since 6.7.0
	 */
	@Parameter(property = "helm.registry-login.insecure", defaultValue = "false")
	private boolean insecure;

	/**
	 * Set this to <code>true</code> to skip invoking registry-login goal.
	 *
	 * @since 6.7.0
	 */
	@Parameter(property = "helm.registry-login.skip", defaultValue = "false")
	private boolean skipRegistryLogin;

	@Override
	public void execute() throws MojoExecutionException {

		if (skip || skipRegistryLogin) {
			getLog().info("Skip registry login");
			return;
		}

		HelmRepository repository = getHelmUploadRepo();
		if (repository == null) {
			getLog().warn("No upload repo found for login, skipped.");
			return;
		}

		PasswordAuthentication authentication = getAuthentication(repository);
		if (authentication == null) {
			getLog().warn("No credentials found for upload repo.");
			return;
		}

		helm()
				.arguments("registry", "login", repository.getUrl())
				.flag("insecure", insecure)
				.flag("username", authentication.getUserName())
				.flag("password-stdin")
				.setStdin(new String(authentication.getPassword()))
				.execute("Failed to login into repository " + repository.getName() + " at " + repository.getUrl());
	}
}
