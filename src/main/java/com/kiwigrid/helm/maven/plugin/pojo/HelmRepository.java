package com.kiwigrid.helm.maven.plugin.pojo;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * POJO for extra repo configuration
 *
 * @author Fabian Schlegel
 * @since 22.2.18
 */
public class HelmRepository {

	/**
	 * Name of repository. If no username/password is configured this name is
	 * interpreted as server id and used to obtain username/password from
	 * server list in <code>settings.xml</code>-
	 */
	@Parameter(property = "helm.repo.name", required = true)
	private String name;

	@Parameter(property = "helm.repo.url", required = true)
	private String url;

	/**
	 * Username for basic authentication. If present credentials in server list will be ignored.
	 */
	@Parameter(property = "helm.repo.username")
	private String username;

	/**
	 * Password for basic authentication. If present credentials in server list will be ignored.
	 */
	@Parameter(property = "helm.repo.password")
	private String password;

	@Parameter(property = "helm.repo.type")
	private RepoType type;

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

	public RepoType getType() {
		return type;
	}

	public void setType(RepoType type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "[" + name + " / " + url + "]";
	}
}
