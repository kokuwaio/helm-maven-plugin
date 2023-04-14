package io.kokuwa.maven.helm.pojo;

import lombok.Data;
import lombok.ToString;

/**
 * POJO for extra repo configuration
 *
 * @author Fabian Schlegel
 * @since 1.8
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

	/**
	 * Add artifact id to helm upload url (Artifactory only).
	 *
	 * @since 6.7.0
	 */
	@ToString.Exclude
	private boolean useArtifactId;

	/**
	 * If <code>true</code>, replaces (overwrite) the repo if it already exists.
	 * Will be combined with "helm.repo.add.force-update".
	 *
	 * @since 6.6.0
	 */
	private boolean forceUpdate = false;
}
