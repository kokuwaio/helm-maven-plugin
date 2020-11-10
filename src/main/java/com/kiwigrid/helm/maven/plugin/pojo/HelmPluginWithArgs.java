package com.kiwigrid.helm.maven.plugin.pojo;

import org.apache.maven.plugins.annotations.Parameter;

import lombok.Getter;
import lombok.Setter;

/**
 * POJO for Helm Plugin configuration with additional arguments for invoking the plugin
 *
 * @author Rob Vesse
 * @since 10.11.2020
 */
@Getter
@Setter
public class HelmPluginWithArgs extends HelmPlugin {

    @Parameter(name = "helm.plugin.args")
	private String[] args;
}
