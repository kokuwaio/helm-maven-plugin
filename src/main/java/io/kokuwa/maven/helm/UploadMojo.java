package io.kokuwa.maven.helm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.kokuwa.maven.helm.pojo.Catalog;
import io.kokuwa.maven.helm.pojo.HelmChart;
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

	private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
	private static final ObjectMapper YAML_MAPPER = new YAMLMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	private static final String CATALOG_ARTIFACT_NAME = "helm-catalog";
	private static final String CATALOG_ARTIFACT_TYPE = "json";

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

	/**
	 * Skips creation of a catalog file with a list of helm chart upload details.
	 *
	 * @since 6.12.0
	 */
	@Parameter(property = "helm.upload.skip.catalog", defaultValue = "true")
	private boolean skipCatalog;

	/**
	 * Verify charts are accessible in repository.
	 *
	 * @since 6.13.0
	 */
	@Parameter(property = "helm.upload.verification", defaultValue = "false")
	private boolean uploadVerification;

	/**
	 * Set timeout period to try verifying charts are accessible in repository.
	 *
	 * @since 6.13.0
	 */
	@Parameter(property = "helm.upload.timeout", defaultValue = "30")
	private Integer uploadVerificationTimeout;

	/**
	 * File where to write the helm chart upload URL.
	 *
	 * @since 6.14.0
	 */
	@Parameter(property = "helm.upload.urlWriteFile")
	private File helmChartUploadUrlFile;

	@Override
	public void execute() throws MojoExecutionException {

		if (skip || skipUpload) {
			getLog().info("Skip upload");
			return;
		}

		if (uploadVerificationTimeout != null && uploadVerificationTimeout <= 0) {
			throw new MojoExecutionException("Timeout must be a positive value.");
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

		if (!skipCatalog) {
			Path catalogPath = getCatalogFilePath();
			getLog().info("Attaching catalog artifact: " + catalogPath);
			mavenProjectHelper.attachArtifact(mavenProject, CATALOG_ARTIFACT_TYPE, CATALOG_ARTIFACT_NAME,
					catalogPath.toFile());
		}

		if (uploadVerification) {
			for (Path chartDirectory : getChartDirectories()) {
				Path chartPath = chartDirectory.resolve("Chart.yaml");
				getLog().info("Verifying upload of " + chartPath);
				if (!verifyUpload(chartPath)) {
					getLog().info("Upload verification timed out.");
					throw new MojoExecutionException("Chart verification failed");
				}
			}
		}

		if (helmChartUploadUrlFile != null) {
			try {
				for (Path chart : getChartArchives()) {
					getLog().info("Writing upload URL of chart " + chart + " to file: " + helmChartUploadUrlFile);
					Files.write(helmChartUploadUrlFile.toPath(),
							getHelmFileUploadUrl(chart).getBytes(StandardCharsets.UTF_8));
				}
			} catch (IOException e) {
				throw new MojoExecutionException("Failed to write upload URL of charts to file", e);
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

	/**
	 * Returns the proper upload URL of the file based on the provided chart version.
	 *
	 * @return Upload URL of the file based on chart version
	 */
	private String getHelmFileUploadUrl(Path chart) {
		File fileToUpload = chart.toFile();
		HelmRepository uploadRepo = getHelmUploadRepo();

		if (uploadRepo.getType() == null) {
			throw new IllegalArgumentException("Repository type missing. Check your plugin configuration.");
		}

		switch (uploadRepo.getType()) {
			case ARTIFACTORY:
				return getFileUrlForUploadToArtifactory(fileToUpload, uploadRepo);
			case CHARTMUSEUM:
				return getHelmUploadUrl();
			case NEXUS:
				return getFileUrlForUploadToNexus(fileToUpload);
			default:
				throw new IllegalArgumentException("Unsupported repository type for upload.");
		}
	}

	/**
	 * Reads the catalog file and deserializes its content as a POJO.
	 *
	 * @param catalogFile instance of catalog file
	 * @return list of Catalog
	 * @throws MojoExecutionException when IOException is encountered
	 */
	List<Catalog> readCatalog(File catalogFile) throws MojoExecutionException {
		List<Catalog> catalogList = new ArrayList<>();
		if (catalogFile == null || !catalogFile.exists()) {
			return catalogList;
		}
		try {
			return Arrays.asList(MAPPER.readValue(catalogFile, Catalog[].class));
		} catch (DatabindException e) {
			getLog().warn("Unable to parse the existing catalog file content. Overwriting data.");
		} catch (IOException e) {
			throw new MojoExecutionException("Failure occurred while reading the catalog file.", e);
		}
		return catalogList;
	}

	/**
	 * Reads the existing helm catalog content file and merges the new catalog data with it.
	 *
	 * @param data helm chart upload info represented as the Catalog object
	 * @return pretty string json representation of the updated helm catalog contents
	 */
	private String createCatalogContent(Catalog data) throws MojoExecutionException {
		File file = getCatalogFilePath().toFile();
		List<Catalog> catalog = readCatalog(file);
		catalog.add(data);
		try {
			return MAPPER.writeValueAsString(catalog);
		} catch (JsonProcessingException e) {
			throw new MojoExecutionException("Failure occurred while writing the catalog content as a JSON string.", e);
		}
	}

	/**
	 * Returns the path where the catalog file is to be created. The catalog file is created in the maven project build
	 * directory.
	 *
	 * @return Path of the catalog file
	 */
	protected Path getCatalogFilePath() {
		String catalogFileName = String.format("%s.%s", CATALOG_ARTIFACT_NAME, CATALOG_ARTIFACT_TYPE);
		return Paths.get(mavenProject.getBuild().getDirectory(), catalogFileName);
	}

	/**
	 * Writes the catalog data to the catalog file.
	 *
	 * @param content Helm upload catalog data in string form
	 * @throws MojoExecutionException when writing catalog data to the file
	 */
	private void catalogHelmChart(String content) throws MojoExecutionException {
		Path catalogPath = getCatalogFilePath();
		File catalogFile = catalogPath.toFile();
		getLog().debug("Writing content to catalog file: " + content);

		try (OutputStream out = Files.newOutputStream(catalogFile.toPath())) {
			// FIXME: It is better to use the encoding either from
			// project.build.sourceEncoding or project.reporting.outputEncoding
			OutputStreamWriter fileWriter = new OutputStreamWriter(out, StandardCharsets.UTF_8);
			fileWriter.write(content);
			fileWriter.close();
		} catch (IOException e) {
			throw new MojoExecutionException("Failure occurred while writing the catalog file.", e);
		}
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
			getLog().info("Use insecure TLS connection for [" + connection.getURL() + "]");
			TLSHelper.insecure((HttpsURLConnection) connection);
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
		}
		String message = "Returned: " + connection.getResponseCode();
		String details = "";
		if (connection.getInputStream() != null) {
			details = new String(IOUtils.toByteArray(connection.getInputStream()), StandardCharsets.UTF_8);
			if (!details.isEmpty()) {
				message += " - " + details;
			}
		}
		getLog().info(message);
		if (!skipCatalog) {
			Catalog data = new Catalog(chart, connection.getURL(), connection.getContentType(), details);
			catalogHelmChart(createCatalogContent(data));
		}
		connection.disconnect();
	}

	private boolean verifyUpload(Path chartPath) throws MojoExecutionException {
		String chartName;
		try {
			chartName = YAML_MAPPER.readValue(chartPath.toFile(), HelmChart.class).getName();
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to read chart from " + chartPath, e);
		}

		long startTimeMillis = System.currentTimeMillis();
		long timeoutMillis = uploadVerificationTimeout * 1000;
		long cutoffMillis = startTimeMillis + timeoutMillis;
		boolean verificationSuccess = false;

		while (System.currentTimeMillis() < cutoffMillis && !verificationSuccess) {
			try {
				helm()
					.arguments("show", "chart", chartName,
						"--version", getChartVersion(), "--repo", getHelmUploadUrl())
					.execute("show chart failed");
				verificationSuccess = true;
			} catch (Exception MojoExecutionException) {
				getLog().info("Upload verification failed, retrying...");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ie) {
					throw new MojoExecutionException("Upload verification interrupted", ie);
				}
			}
		}
		return verificationSuccess;
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
		String uploadUrl = getFileUrlForUploadToArtifactory(file, repo);

		HttpURLConnection connection = (HttpURLConnection) new URL(uploadUrl).openConnection();
		connection.setDoOutput(true);
		connection.setRequestMethod("PUT");
		connection.setRequestProperty("Content-Type", "application/gzip");

		verifyAndSetAuthentication(true);

		return connection;
	}

	private String getFileUrlForUploadToArtifactory(File file, HelmRepository repo) {
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
		return uploadUrl;
	}

	private HttpURLConnection getConnectionForUploadToNexus(File file) throws IOException, MojoExecutionException {
		String uploadUrl = getFileUrlForUploadToNexus(file);

		HttpURLConnection connection = (HttpURLConnection) new URL(uploadUrl).openConnection();
		connection.setDoOutput(true);
		connection.setRequestMethod("PUT");
		connection.setRequestProperty("Content-Type", "application/gzip");

		verifyAndSetAuthentication(false);

		return connection;
	}

	private String getFileUrlForUploadToNexus(File file) {
		String uploadUrl = getHelmUploadUrl();
		// Append slash if not already in place
		if (!uploadUrl.endsWith("/")) {
				uploadUrl += "/";
			}
		uploadUrl = uploadUrl + file.getName();
		return uploadUrl;
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
