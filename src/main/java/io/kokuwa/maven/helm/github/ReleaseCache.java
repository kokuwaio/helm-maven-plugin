package io.kokuwa.maven.helm.github;

import lombok.Data;

/**
 * Pojo for caching Github data.
 *
 * @author Stephan Schnabel
 */
@Data
public class ReleaseCache {

	private String etag;
	private ReleaseResponse response;
}
