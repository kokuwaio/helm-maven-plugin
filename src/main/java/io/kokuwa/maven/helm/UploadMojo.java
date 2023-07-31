package io.kokuwa.maven.helm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

import io.kokuwa.maven.helm.pojo.HelmRepository;
import io.kokuwa.maven.helm.pojo.RepoType;
import lombok.Setter;

/**
 * Mojo for uploading to helm repo (see types {@link RepoType}).
 *
 * @author Fabian Schlegel
 * @since 1.4
 */
@Mojo(name = "upload", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
@Setter
public class UploadMojo extends AbstractHelmMojo {

	/**
	 * Set this to <code>true</code> to skip invoking upload goal.
	 *
	 * @since 3.3
	 */
	@Parameter(property = "helm.upload.skip", defaultValue = "false")
	private boolean skipUpload;

	/**
	 * Project groupId.
	 *
	 * @since 5.10
	 */
	@Parameter(defaultValue = "${project.groupId}", readonly = true)
	private String projectGroupId;

	/**
	 * Project artifactId.
	 *
	 * @since 6.7.0
	 */
	@Parameter(defaultValue = "${project.artifactId}", readonly = true)
	private String projectArtifactId;

	/**
	 * Project version.
	 *
	 * @since 5.10
	 */
	@Parameter(defaultValue = "${project.version}", readonly = true)
	private String projectVersion;

	/**
	 * Skip tls certificate checks for the chart upload.
	 *
	 * @since 6.9.0
	 */
	@Parameter(property = "helm.upload.insecure", defaultValue = "false")
	private boolean insecure;

	@Override
	public void execute() throws MojoExecutionException {

		if (skip || skipUpload) {
			getLog().info("Skip upload");
			return;
		}

		getLog().info("Uploading to " + getHelmUploadUrl() + "\n");
		for (Path chart : getChartArchives()) {
			getLog().info("Uploading " + chart + "...");
			try {
				uploadSingle(chart);
			} catch (IOException e) {
				throw new MojoExecutionException("Upload failed.", e);
			}
		}
	}

	/**
	 * Returns the proper upload URL based on the provided chart version. Charts w/ an SNAPSHOT suffix will be uploaded
	 * to SNAPSHOT repo.
	 *
	 * @return Upload URL based on chart version
	 */
	private String getHelmUploadUrl() {
		String chartVersion = getChartVersion();
		HelmRepository uploadRepoStable = getUploadRepoStable();
		HelmRepository uploadRepoSnapshot = getUploadRepoSnapshot();
		String uploadUrl = uploadRepoStable.getUrl();
		if (chartVersion != null && chartVersion.endsWith("-SNAPSHOT")
				&& uploadRepoSnapshot != null
				&& StringUtils.isNotEmpty(uploadRepoSnapshot.getUrl())) {
			uploadUrl = uploadRepoSnapshot.getUrl();
		}

		return uploadUrl;
	}

	private void uploadSingle(Path chart) throws MojoExecutionException, IOException {
		File fileToUpload = chart.toFile();
		HelmRepository uploadRepo = getHelmUploadRepo();

		HttpURLConnection connection;
		if (uploadRepo.getType() == null) {
			throw new IllegalArgumentException("Repository type missing. Check your plugin configuration.");
		}

		switch (uploadRepo.getType()) {
			case ARTIFACTORY:
				connection = getConnectionForUploadToArtifactory(fileToUpload, uploadRepo);
				break;
			case CHARTMUSEUM:
				connection = getConnectionForUploadToChartMuseum();
				break;
			case NEXUS:
				connection = getConnectionForUploadToNexus(fileToUpload);
				break;
			default:
				throw new IllegalArgumentException("Unsupported repository type for upload.");
		}

		if (insecure && connection instanceof HttpsURLConnection) {
			setupInsecureTLS((HttpsURLConnection) connection);
		}

		try (FileInputStream fileInputStream = new FileInputStream(fileToUpload)) {
			IOUtils.copy(fileInputStream, connection.getOutputStream());
		}
		if (connection.getResponseCode() >= 300) {
			String response;
			if (connection.getErrorStream() != null) {
				response = new String(IOUtils.toByteArray(connection.getErrorStream()), StandardCharsets.UTF_8);
			} else if (connection.getInputStream() != null) {
				response = new String(IOUtils.toByteArray(connection.getInputStream()), StandardCharsets.UTF_8);
			} else {
				response = "No details provided";
			}
			throw new MojoExecutionException("Failed to upload: " + response);
		} else {
			String message = "Returned: " + connection.getResponseCode();
			if (connection.getInputStream() != null) {
				String details = new String(IOUtils.toByteArray(connection.getInputStream()), StandardCharsets.UTF_8);
				if (!details.isEmpty()) {
					message += " - " + details;
				}
			}
			getLog().info(message);
		}
		connection.disconnect();
	}

	private HttpURLConnection getConnectionForUploadToChartMuseum() throws IOException, MojoExecutionException {
		HttpURLConnection connection = (HttpURLConnection) new URL(getHelmUploadUrl()).openConnection();
		connection.setDoOutput(true);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/gzip");

		setBasicAuthHeader(connection);

		return connection;
	}

	private void setBasicAuthHeader(HttpURLConnection connection) throws MojoExecutionException {
		PasswordAuthentication authentication = getAuthentication(getHelmUploadRepo());
		if (authentication != null) {
			String encoded = Base64.getEncoder()
					.encodeToString((authentication.getUserName() + ":" + new String(authentication.getPassword()))
							.getBytes(StandardCharsets.UTF_8));
			connection.setRequestProperty("Authorization", "Basic " + encoded);
		}
	}

	private HttpURLConnection getConnectionForUploadToArtifactory(File file, HelmRepository repo)
			throws IOException, MojoExecutionException {
		String uploadUrl = getHelmUploadUrl();
		// Append slash if not already in place
		if (!uploadUrl.endsWith("/")) {
			uploadUrl += "/";
		}
		if (repo.isUseGroupId()) {
			uploadUrl += projectGroupId.replace(".", "/") + "/";
		}
		if (repo.isUseArtifactId()) {
			uploadUrl += projectArtifactId.replace(".", "/") + "/";
		}
		if (repo.isUseGroupId() || repo.isUseArtifactId()) {
			uploadUrl += projectVersion + "/";
		}

		uploadUrl = uploadUrl + file.getName();

		HttpURLConnection connection = (HttpURLConnection) new URL(uploadUrl).openConnection();
		connection.setDoOutput(true);
		connection.setRequestMethod("PUT");
		connection.setRequestProperty("Content-Type", "application/gzip");

		verifyAndSetAuthentication(true);

		return connection;
	}

	private HttpURLConnection getConnectionForUploadToNexus(File file) throws IOException, MojoExecutionException {
		String uploadUrl = getHelmUploadUrl();
		// Append slash if not already in place
		if (!uploadUrl.endsWith("/")) {
			uploadUrl += "/";
		}
		uploadUrl = uploadUrl + file.getName();

		HttpURLConnection connection = (HttpURLConnection) new URL(uploadUrl).openConnection();
		connection.setDoOutput(true);
		connection.setRequestMethod("PUT");
		connection.setRequestProperty("Content-Type", "application/gzip");

		verifyAndSetAuthentication(false);

		return connection;
	}

	private void setupInsecureTLS(HttpsURLConnection connection) throws MojoExecutionException {

		TrustManager trustManager = new X509TrustManager() {

			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			@Override
			public void checkClientTrusted(X509Certificate[] certs, String authType) {}

			@Override
			public void checkServerTrusted(X509Certificate[] certs, String authType) {}
		};

		SSLContext sc;
		try {
			sc = SSLContext.getInstance("TLS");
			sc.init(null, new TrustManager[] { trustManager }, new java.security.SecureRandom());
		} catch (KeyManagementException e) {
			throw new MojoExecutionException("Cannot initialize TLS context instance", e);
		} catch (NoSuchAlgorithmException e) {
			throw new MojoExecutionException("Cannot create TLS context instance", e);
		}

		connection.setSSLSocketFactory(sc.getSocketFactory());
		connection.setHostnameVerifier((hostname, session) -> true);

		getLog().info("Use insecure TLS connection for [" + connection.getURL() + "]");
	}

	/**
	 *
	 * @param requireCredentials The need for credentials depends on how the repository is configured. For instance on
	 *                           nexus it is possible to configure a repository without authentication
	 * @throws MojoExecutionException
	 */
	private void verifyAndSetAuthentication(boolean requireCredentials) throws MojoExecutionException {

		PasswordAuthentication authentication = getAuthentication(getHelmUploadRepo());
		if (authentication != null) {
			Authenticator.setDefault(new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return authentication;
				}
			});
		} else if (requireCredentials) {
			throw new IllegalArgumentException("Credentials has to be configured for uploading to Artifactory.");
		}
	}
}
