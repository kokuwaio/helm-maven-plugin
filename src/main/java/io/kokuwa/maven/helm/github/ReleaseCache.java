package io.kokuwa.maven.helm.github;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Pojo for caching Github data.
 *
 * @author Stephan Schnabel
 */
@Data
@Accessors(chain = true)
public class ReleaseCache {

	private String etag;
	private ReleaseResponse response;
}
