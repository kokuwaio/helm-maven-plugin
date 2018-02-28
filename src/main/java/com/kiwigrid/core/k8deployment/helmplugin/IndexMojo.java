package com.kiwigrid.core.k8deployment.helmplugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.StringUtils;

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
		callCli(getHelmExecuteablePath()
				+ " repo index "
				+ getOutputDirectory()
				+ " --url "
				+ getHelmRepoUrl()
				+ " --merge "
				+ getIndexFileForMerge()
				+ (StringUtils.isNotEmpty(getHelmHomeDirectory()) ? " --home=" + getHelmHomeDirectory() : ""),
				"Unable to index repo at " + getOutputDirectory(), true);
	}
}
