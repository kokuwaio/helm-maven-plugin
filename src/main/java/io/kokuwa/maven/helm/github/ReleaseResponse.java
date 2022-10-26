package io.kokuwa.maven.helm.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * Response for fetching Github releases.
 *
 * @author Stephan Schnabel
 * @since 6.1.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReleaseResponse {

	@JsonProperty(value = "tag_name", required = true)
	private String tagName;
}
