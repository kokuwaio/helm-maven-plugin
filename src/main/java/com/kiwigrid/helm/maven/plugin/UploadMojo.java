package com.kiwigrid.helm.maven.plugin;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.charset.Charset;

import com.kiwigrid.helm.maven.plugin.exception.BadUploadException;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.StringUtils;

/**
 * Mojo for uploading to helm repo (e.g. chartmuseum)
 *
 * @author Fabian Schlegel
 * @since 02.01.18
 */
@Mojo(name = "upload", defaultPhase = LifecyclePhase.DEPLOY)
public class UploadMojo extends AbstractHelmMojo {

	public void execute()
			throws MojoExecutionException
	{
		getLog().info("Uploading to " + getHelmUploadUrl() + "\n");
		for (String chartPackageFile : getChartTgzs(getChartDirectory())) {
			getLog().info("Uploading " + chartPackageFile + "...");
			try {
				uploadSingle(chartPackageFile);
			} catch (BadUploadException | IOException e) {
				getLog().error(e.getMessage());
				throw new MojoExecutionException("Error uploading " + chartPackageFile + " to " + getHelmUploadUrl(),
						e);
			}
		}
	}

	private void uploadSingle(String file) throws IOException, BadUploadException {
		HttpURLConnection connection = (HttpURLConnection) new URL(getHelmUploadUrl()).openConnection();
		connection.setDoOutput(true);
		connection.setRequestMethod("PUT");
		connection.setRequestProperty("Content-Type", "application/gzip");
		configureAuthenticationIfSet();

		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			IOUtils.copy(fileInputStream, connection.getOutputStream());
		}
		if (connection.getResponseCode() >= 400) {
			String response = IOUtils.toString(connection.getErrorStream(), Charset.defaultCharset());
			throw new BadUploadException(response);
		} else {
			String response = IOUtils.toString(connection.getInputStream(), Charset.defaultCharset());
			getLog().info(Integer.toString(connection.getResponseCode()) + " - " + response);
		}
		connection.disconnect();
	}

	private void configureAuthenticationIfSet() {
		if (getChartVersion().endsWith("-SNAPSHOT")
				&& getUploadRepoSnapshot() != null
				&& StringUtils.isNotEmpty(getUploadRepoSnapshot().getUsername()))
		{
			setAuthenticatorForRepo(getUploadRepoSnapshot());

		} else if (!getChartVersion().endsWith("-SNAPSHOT")
				&& getUploadRepoStable() != null
				&& getUploadRepoStable().getUsername() != null)
		{
			setAuthenticatorForRepo(getUploadRepoStable());
		}
	}

	private void setAuthenticatorForRepo(HelmRepository uploadRepo) {
		Authenticator.setDefault(new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(uploadRepo.getUsername(), uploadRepo.getPassword().toCharArray());
			}
		});
	}
}
