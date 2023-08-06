package io.kokuwa.maven.helm.pojo;

import java.util.List;

import lombok.Data;

/**
 * POJO for list of "Chart.yaml" dependencies.
 *
 * @since 6.10.0
 */
@Data
public class Dependencies {

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
