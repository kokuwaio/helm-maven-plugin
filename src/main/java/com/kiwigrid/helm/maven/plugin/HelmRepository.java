package com.kiwigrid.helm.maven.plugin;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * POJO for extra repo configuration
 *
 * @author Fabian Schlegel
 * @since 22.2.18
 */
public class HelmRepository {

	@Parameter(property = "helm.extraRepos.name", required = true)
	private String name;

	@Parameter(property = "helm.extraRepos.url", required = true)
	private String url;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public String toString() {
		return "[" + name + " / " + url + "]";
	}
}
