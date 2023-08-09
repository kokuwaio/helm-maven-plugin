package io.kokuwa.maven.helm.pojo;

import java.util.ArrayList;

import lombok.Data;

/**
 * POJO for list of Chart.yaml dependencies
 *
 * @since 6.9.1
 */
@Data
public class Dependencies {
	private ArrayList<Dependency> dependencies;

	@Data
	public static class Dependency {
		private String name;
		private String version;
		private String repository;

		/**
		 * Determines whether the repository/version will be updated 
		 * for a given dependency.
		 */
		private boolean overwrite = false;		
	}
}
