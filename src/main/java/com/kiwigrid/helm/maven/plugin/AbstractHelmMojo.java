package com.kiwigrid.helm.maven.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Base class for mojos
 *
 * @author Fabian Schlegel
 * @since 06.11.17
 */
public abstract class AbstractHelmMojo extends AbstractMojo {

	@Parameter(property = "helm.executableDirectory", defaultValue = "${project.build.directory}/helm")
	private String helmExecuteableDirectory;

	/**
	 * If no executeable is set this plugin tries to determine helm executeable based on operation system.
	 */
	@Parameter(property = "helm.executable")
	private String helmExecuteable;

	@Parameter(property = "helm.outputDirectory", defaultValue = "${project.build.directory}/helm/repo")
	private String outputDirectory;

	@Parameter(property = "helm.excludes")
	private String[] excludes;

	@Parameter(property = "helm.chartDirectory", required = true)
	private String chartDirectory;

	@Parameter(property = "helm.chartVersion")
	private String chartVersion;

	@Parameter(property = "helm.uploadUrl.stable", required = true)
	private String helmUploadUrlStable;

	@Parameter(property = "helm.uploadUrl.snapshot", required = true)
	private String helmUploadUrlSnapshot;

	@Parameter(property = "helm.downloadUrl")
	private String helmDownloadUrl;

	@Parameter(property = "helm.homeDirectory")
	private String helmHomeDirectory;

	@Parameter(property = "helm.extraRepos")
	private HelmRepository[] helmExtraRepos;

	Path getHelmExecuteablePath() throws MojoExecutionException {
		if (helmExecuteable == null) {
			helmExecuteable = SystemUtils.IS_OS_WINDOWS ? "helm.exe" : "helm";
		}
		Path path = Paths.get(helmExecuteableDirectory, helmExecuteable).toAbsolutePath();
		if (!Files.exists(path)) {
			throw new MojoExecutionException("Helm executeable at " + path + " not found.");
		}
		return path;
	}

	/**
	 * Calls cli with specified command
	 *
	 * @param command the command to be executed
	 * @param errorMessage a readable error message that will be shown in case of exceptions
	 * @param verbose logs STDOUT to Maven info log
	 * @throws MojoExecutionException on error
	 */
	void callCli(String command, String errorMessage, final boolean verbose) throws MojoExecutionException {

		int exitValue;

		getLog().debug(command);

		try {
			final Process p = Runtime.getRuntime().exec(command);
			new Thread(() -> {
				BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
				BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				String inputLine;
				String errorLine;
				try {
					while ((inputLine = input.readLine()) != null) {
						if (verbose) {
							getLog().info(inputLine);
						} else {
							getLog().debug(inputLine);
						}
					}
					while ((errorLine = error.readLine()) != null) {
						getLog().error(errorLine);
					}
				} catch (IOException e) {
					getLog().error(e);
				}
			}).start();
			p.waitFor();
			exitValue = p.exitValue();
		} catch (Exception e) {
			throw new MojoExecutionException("Error processing command [" + command + "]", e);
		}

		if (exitValue != 0) {
			throw new MojoExecutionException(errorMessage);
		}
	}

	List<String> getChartDirectories(String path) throws MojoExecutionException {
		try (Stream<Path> files = Files.walk(Paths.get(path))) {
			return files.filter(p -> p.getFileName().toString().equalsIgnoreCase("chart.yaml"))
					.map(p -> p.getParent().toString())
					.collect(Collectors.toList());
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to scan chart directory at " + path, e);
		}
	}

	List<String> getChartTgzs(String path) throws MojoExecutionException {
		try (Stream<Path> files = Files.walk(Paths.get(path))) {
			return files.filter(p -> p.getFileName().toString().endsWith("tgz"))
					.map(p -> p.toString())
					.collect(Collectors.toList());
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to scan repo directory at " + path, e);
		}
	}

	/**
	 * Returns the proper upload URL based on the provided chart version.
	 * Charts w/ an SNAPSHOT suffix will be uploaded to SNAPSHOT repo.
	 *
	 * @return Upload URL based on chart version
	 */
	String getHelmUploadUrl() {
		String uploadUrl = helmUploadUrlStable;
		if(chartVersion.endsWith("-SNAPSHOT")) {
			uploadUrl = helmUploadUrlSnapshot;
		}
		return uploadUrl;
	}

	public String getHelmExecuteable() {
		return helmExecuteable;
	}

	public void setHelmExecuteable(String helmExecuteable) {
		this.helmExecuteable = helmExecuteable;
	}

	public String getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public String getHelmExecuteableDirectory() {
		return helmExecuteableDirectory;
	}

	public void setHelmExecuteableDirectory(String helmExecuteableDirectory) {
		this.helmExecuteableDirectory = helmExecuteableDirectory;
	}

	public String getHelmDownloadUrl() {
		return helmDownloadUrl;
	}

	public void setHelmDownloadUrl(String helmDownloadUrl) {
		this.helmDownloadUrl = helmDownloadUrl;
	}

	public String[] getExcludes() {
		return excludes;
	}

	public void setExcludes(String[] excludes) {
		this.excludes = excludes;
	}

	public String getChartDirectory() {
		return chartDirectory;
	}

	public void setChartDirectory(String chartDirectory) {
		this.chartDirectory = chartDirectory;
	}

	public String getHelmHomeDirectory() {
		return helmHomeDirectory;
	}

	public void setHelmHomeDirectory(String helmHomeDirectory) {
		this.helmHomeDirectory = helmHomeDirectory;
	}

	public String getHelmUploadUrlStable() {
		return helmUploadUrlStable;
	}

	public void setHelmUploadUrlStable(String helmUploadUrlStable) {
		this.helmUploadUrlStable = helmUploadUrlStable;
	}

	public String getHelmUploadUrlSnapshot() {
		return helmUploadUrlSnapshot;
	}

	public void setHelmUploadUrlSnapshot(String helmUploadUrlSnapshot) {
		this.helmUploadUrlSnapshot = helmUploadUrlSnapshot;
	}

	public String getChartVersion() {
		return chartVersion;
	}

	public void setChartVersion(String chartVersion) {
		this.chartVersion = chartVersion;
	}

	public HelmRepository[] getHelmExtraRepos() {
		return helmExtraRepos;
	}

	public void setHelmExtraRepos(HelmRepository[] helmExtraRepos) {
		this.helmExtraRepos = helmExtraRepos;
	}
}



