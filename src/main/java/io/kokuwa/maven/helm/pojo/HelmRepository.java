package io.kokuwa.maven.helm.pojo;

import org.apache.maven.plugins.annotations.Parameter;

import lombok.Getter;
import lombok.Setter;

/**
 * POJO for extra repo configuration
 *
 * @author Fabian Schlegel
 * @since 22.2.18
 */
@Getter
@Setter
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

	@Parameter(property = "helm.upload.by.groupId")
	private boolean useGroupId;

	@Override
	public String toString() {
		return "[" + name + " / " + url + "]";
	}
}
