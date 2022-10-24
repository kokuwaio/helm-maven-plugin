package io.kokuwa.maven.helm.pojo;

import lombok.Data;
import lombok.ToString;

/**
 * POJO for K8S cluster configuration
 *
 * @author Kirill Nazarov
 * @since 29.12.21
 */
@Deprecated // java8 (since = "6.3.0", forRemoval = true)
@Data
public class K8SCluster {

	private String apiUrl;
	private String namespace;
	private String asUser;
	private String asGroup;
	@ToString.Exclude
	private String token;
}
