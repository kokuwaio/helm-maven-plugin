package com.kiwigrid.core.k8deployment.helmplugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

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

	@Parameter(property = "helmExecutableDirectory", defaultValue = "${project.build.directory}/helm")
	private String helmExecuteableDirectory;

	@Parameter(property = "helmExecutable", defaultValue = "${project.build.directory}/helm/linux-amd64/helm")
	private String helmExecuteable;

	@Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/helm/repo")
	private String outputDirectory;

	@Parameter(property = "excludes")
	private String[] excludes;

	@Parameter(property = "chartDirectory", required = true)
	private String chartDirectory;

	@Parameter(property = "helmRepoUrl", required = true)
	private String helmRepoUrl;

	@Parameter(property = "indexFileForMerge")
	private String indexFileForMerge;

	@Parameter(property = "helmDownloadUrl")
	private String helmDownloadUrl;

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

		try {
			final Process p = Runtime.getRuntime().exec(command);
			new Thread(new Runnable() {
				public void run() {
					BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
					BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
					String inputLine;
					String errorLine;
					try {
						while ((inputLine = input.readLine()) != null)
							if (verbose) {
								getLog().info(inputLine);
							} else {
								getLog().debug(inputLine);
							}
						while ((errorLine = error.readLine()) != null)
							getLog().error(errorLine);
					} catch (IOException e) {
						getLog().error(e);
					}
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
		try {
			return Files.walk(Paths.get(path))
					.filter(p -> p.getFileName().toString().equalsIgnoreCase("chart.yaml"))
					.map(p -> p.getParent().toString())
					.collect(Collectors.toList());
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to scan chart directory at " + path, e);
		}
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

	public String getHelmRepoUrl() {
		return helmRepoUrl;
	}

	public void setHelmRepoUrl(String helmRepoUrl) {
		this.helmRepoUrl = helmRepoUrl;
	}

	public String getIndexFileForMerge() {
		return indexFileForMerge;
	}

	public void setIndexFileForMerge(String indexFileForMerge) {
		this.indexFileForMerge = indexFileForMerge;
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
}
