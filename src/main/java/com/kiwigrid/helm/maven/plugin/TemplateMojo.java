package com.kiwigrid.helm.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

/**
 * Mojo for running helm template.
 *
 * @author Kirill Nazarov
 * @since 28.12.28
 */

@Mojo(name = "template", defaultPhase = LifecyclePhase.TEST)
public class TemplateMojo extends AbstractHelmWithValueOverrideMojo {

    @Parameter(property = "helm.template.skip", defaultValue = "false")
    private boolean skipDryRun;

    @Parameter(property = "helm.additional.arguments")
    private String additionalArguments;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip || skipDryRun) {
            getLog().info("Skip helm template");
            return;
        }
        for (String inputDirectory : getChartDirectories(getChartDirectory())) {
            getLog().info("\n\nPerform template for chart " + inputDirectory + "...");

            callCli(getHelmExecuteablePath()
                            + " template "
                            + " " + inputDirectory
                            + " " + (StringUtils.isNotEmpty(additionalArguments) ? additionalArguments : "")
                            + getValuesOptions()
                            + (StringUtils.isNotEmpty(getRegistryConfig()) ? " --registry-config=" + getRegistryConfig() : "")
                            + (StringUtils.isNotEmpty(getRepositoryCache()) ? " --repository-cache=" + getRepositoryCache() : "")
                            + (StringUtils.isNotEmpty(getRepositoryConfig()) ? " --repository-config=" + getRepositoryConfig() : ""),
                    "There are test failures", true);
        }
    }
}
