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
@Data
public class K8SCluster {

	@Parameter(property = "helm.k8s.api-url")
	private String apiUrl;

	/**
	 * Namespace to use while running helm command
	 * @deprecated
	 * This field should be used from base mojo
	 * <p> Use {@link io.kokuwa.maven.helm.AbstractHelmMojo#getNamespace()} instead.
	 */
	@Deprecated
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
