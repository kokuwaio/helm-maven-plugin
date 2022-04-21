package io.kokuwa.maven.helm.pojo;

import org.apache.maven.plugins.annotations.Parameter;

import lombok.Data;
import lombok.ToString;

/**
 * POJO for extra repo configuration
 *
 * @author Fabian Schlegel
 * @since 22.2.18
 */
@Data
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
	@ToString.Exclude
	private String username;

	/**
	 * Password for basic authentication. If present credentials in server list will be ignored.
	 */
	@Parameter(property = "helm.repo.password")
	@ToString.Exclude
	private String password;

	@Parameter(property = "helm.repo.type")
	@ToString.Exclude
	private RepoType type;

	@Parameter(property = "helm.upload.by.groupId")
	@ToString.Exclude
	private boolean useGroupId;
}
