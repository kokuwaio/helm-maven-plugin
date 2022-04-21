package io.kokuwa.maven.helm.pojo;

import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

import lombok.Getter;
import lombok.Setter;

/**
 * POJO for K8S cluster configuration
 *
 * @author Kirill Nazarov
 * @since 29.12.21
 */
@Getter
@Setter
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
	private String token;

	@Override
	public String toString() {
		return "["
				+ (StringUtils.isNotEmpty(apiUrl) ? "[Override] Cluster API Url:" + apiUrl : "")
				+ (StringUtils.isNotEmpty(namespace) ? " [Override] Namespace: " + namespace : "")
				+ (StringUtils.isNotEmpty(asUser) ? " [Override] As User: " + asUser : "")
				+ (StringUtils.isNotEmpty(asGroup) ? " [Override] As Group: " + asGroup : "")
				+ (StringUtils.isNotEmpty(token) ? " [Override] Token: " + asGroup.replaceAll(".", "*").substring(0, 8)
						: "")
				+ "]".trim();
	}
}
