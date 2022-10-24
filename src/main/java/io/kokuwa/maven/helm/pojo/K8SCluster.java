package io.kokuwa.maven.helm.pojo;

import org.apache.maven.plugins.annotations.Parameter;

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

	@Parameter(property = "helm.k8s.api-url")
	private String apiUrl;

	@Parameter(property = "helm.k8s.namespace")
	private String namespace;

	@Parameter(property = "helm.k8s.as-user")
	private String asUser;

	@Parameter(property = "helm.k8s.as-group")
	private String asGroup;

	@Parameter(property = "helm.k8s.token")
	@ToString.Exclude
	private String token;
}
