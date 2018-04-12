package com.kiwigrid.helm.maven.plugin;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * POJO for extra repo configuration
 *
 * @author Fabian Schlegel
 * @since 22.2.18
 */
public class HelmRepository {

	@Parameter(property = "helm.repo.name", required = true)
	private String name;

	@Parameter(property = "helm.repo.url", required = true)
	private String url;

	@Parameter(property = "helm.repo.username")
	private String username;

	@Parameter(property = "helm.repo.password")
	private String password;

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

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public String toString() {
		return "[" + name + " / " + url + "]";
	}
}
