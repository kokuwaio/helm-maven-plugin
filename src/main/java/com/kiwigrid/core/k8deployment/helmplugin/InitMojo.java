package com.kiwigrid.core.k8deployment.helmplugin;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Mojo for initializing helm
 *
 * @author Fabian Schlegel
 * @since 06.11.17
 */
@Mojo(name = "init", defaultPhase = LifecyclePhase.INITIALIZE)
public class InitMojo extends AbstractHelmMojo {

	public void execute()
			throws MojoExecutionException
	{
		getLog().info("Initializing Helm...");
		getLog().info("Creating output directory...");
		callCli("mkdir -p " + getOutputDirectory(), "Unable to create output directory at " + getOutputDirectory(),
				false);
		getLog().info("Downloading Helm...");
		callCli("wget -qO "
						+ getHelmExecuteableDirectory()
						+ File.separator
						+ "helm.tar.gz "
						+ getHelmDownloadUrl(),
				"Unable to download helm", false);
		getLog().info("Unpacking Helm...");
		callCli("tar -xf "
				+ getHelmExecuteableDirectory()
				+ File.separator
				+ "helm.tar.gz -C "
				+ getHelmExecuteableDirectory(), "Unable to unpack helm to " + getHelmExecuteableDirectory(), false);
		getLog().info("Run helm init...");
		callCli(getHelmExecuteableDirectory()
				+ File.separator
				+ "linux-amd64"
				+ File.separator
				+ "helm init --client-only", "Unable to call helm init", false);

		getLog().info("Enable incubator repo...");
		callCli(getHelmExecuteableDirectory()
				+ File.separator
				+ "linux-amd64"
				+ File.separator
				+ "helm repo add incubator http://storage.googleapis.com/kubernetes-charts-incubator", "Unable add incubator repo", false);
	}
}
