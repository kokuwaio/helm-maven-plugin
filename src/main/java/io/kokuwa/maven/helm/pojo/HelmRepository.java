package io.kokuwa.maven.helm.pojo;

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
	private String name;

	private String url;

	/**
	 * Username for basic authentication. If present credentials in server list will be ignored.
	 */
	@ToString.Exclude
	private String username;

	/**
	 * Password for basic authentication. If present credentials in server list will be ignored.
	 */
	@ToString.Exclude
	private String password;

	@ToString.Exclude
	private RepoType type;

	@ToString.Exclude
	private boolean useGroupId;
}
