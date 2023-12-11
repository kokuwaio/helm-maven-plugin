package io.kokuwa.maven.helm.pojo;

import java.util.List;

import lombok.Data;

/**
 * POJO for "Chart.yaml" file and its dependencies.
 *
 * @since 6.10.0
 */
@Data
public class HelmChart {

	private String apiVersion;
	private String name;
	private String version;
	private List<Dependency> dependencies;

	@Data
	public static class Dependency {

		private String name;
		private String repository;
		private String version;

		/** Determines whether the repository/version will be updated for a given dependency. */
		private boolean overwrite = false;
	}
}
