package io.kokuwa.maven.helm.github;

import lombok.Data;

/**
 * Pojo for caching Github data.
 *
 * @author Stephan Schnabel
 * @since 6.1.0
 */
@Data
public class ReleaseCache {

	private String etag;
	private ReleaseResponse response;
}
