package io.kokuwa.maven.helm.github;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

/**
 * Pojo for caching Github data.
 *
 * @author Stephan Schnabel
 * @since 6.1.0
 */
@Data
public class ReleaseCache {

	@JsonIgnore
	private Instant timestamp;
	private String etag;
	private ReleaseResponse response;
}
