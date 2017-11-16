package com.kiwigrid.core.k8deployment.helmplugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Mojo for creating the repository index
 *
 * @author Fabian Schlegel
 * @since 06.11.17
 */
@Mojo(name = "index", defaultPhase = LifecyclePhase.PACKAGE)
public class IndexMojo extends AbstractHelmMojo {

	public void execute()
			throws MojoExecutionException
	{
		getLog().info("Indexing Repo at " + getOutputDirectory() + "...");
		callCli(getHelmExecuteable()
				+ " repo index "
				+ getOutputDirectory()
				+ " --url "
				+ getHelmRepoUrl()
				+ " --merge "
				+ getIndexFileForMerge(), "Unable to index repo at " + getOutputDirectory(), true);
	}
}
