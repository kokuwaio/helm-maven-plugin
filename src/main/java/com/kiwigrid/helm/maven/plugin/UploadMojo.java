package com.kiwigrid.helm.maven.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.charset.Charset;

import com.kiwigrid.helm.maven.plugin.exception.BadUploadException;
import java.util.UUID;
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
				if(getHelmUseMultipart().equals("false")) {
					uploadSingle(chartPackageFile);
				} else {
					uploadSingleMultipart(chartPackageFile);
				}
			} catch (BadUploadException | IOException e) {
				getLog().error(e.getMessage());
				throw new MojoExecutionException("Error uploading " + chartPackageFile + " to " + getHelmUploadUrl(),
						e);
			}
		}
	}

	private void uploadSingleMultipart(final String file) throws IOException, BadUploadException {
		final File fileToUpload = new File(file);
		String uploadUrl = getHelmUploadUrl() + fileToUpload.getName();

		final HttpURLConnection connection = (HttpURLConnection) new URL(uploadUrl).openConnection();

		String boundaryString = "----"+UUID.randomUUID().toString();

		connection.setDoOutput(true);
		connection.setRequestMethod("POST");
		connection.addRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundaryString);

		OutputStream outputStreamToRequestBody = connection.getOutputStream();
		OutputStreamWriter httpRequestBodyWriter = new OutputStreamWriter(outputStreamToRequestBody);

		httpRequestBodyWriter.write("\n--" + boundaryString + "\n");
		httpRequestBodyWriter.write("Content-Disposition: form-data;"
				+ "name=\""+getHelmUseMultipartName()+"\";"
				+ "filename=\""+ fileToUpload.getName() +"\""
				+ "\nContent-Type: application/gzip\n\n");
		httpRequestBodyWriter.flush();

		FileInputStream fileInputStream = new FileInputStream(fileToUpload);

		int bytesRead;
		byte[] dataBuffer = new byte[1024];
		while((bytesRead = fileInputStream.read(dataBuffer)) != -1) {
			outputStreamToRequestBody.write(dataBuffer, 0, bytesRead);
		}
		outputStreamToRequestBody.flush();

		httpRequestBodyWriter.write("\n--" + boundaryString + "--\n");
		httpRequestBodyWriter.flush();

		outputStreamToRequestBody.close();
		httpRequestBodyWriter.close();

	}

	private void uploadSingle(String file) throws IOException, BadUploadException {
		final File fileToUpload = new File(file);
		String uploadUrl = getHelmUploadUrl() + fileToUpload.getName();

		final HttpURLConnection connection = (HttpURLConnection) new URL(uploadUrl).openConnection();
		connection.setDoOutput(true);
		connection.setRequestMethod("PUT");
		connection.setRequestProperty("Content-Type", "application/gzip");
		configureAuthenticationIfSet();

		try (FileInputStream fileInputStream = new FileInputStream(fileToUpload)) {
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
