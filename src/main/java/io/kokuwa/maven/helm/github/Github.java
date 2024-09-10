package io.kokuwa.maven.helm.github;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * Utility for Github requests with caching.
 *
 * @author Stephan Schnabel
 * @since 6.1.0
 */
@RequiredArgsConstructor
public class Github {

	private static final String LATEST_RELEASE_URL = "https://api.github.com/repos/helm/helm/releases/latest";
	private static final String RELEASE_FILE = "github-release.json";

	private final ObjectMapper mapper = new ObjectMapper();
	private final Log log;
	private final Path tmpDir;
	private final String userAgent;

	public String getHelmVersion() throws MojoExecutionException {

		// strip leading v, this plugin worked without before and nothing should break
		String tagName = getReleaseResponse().getTagName();
		String version = tagName.startsWith("v") ? tagName.substring(1) : tagName;

		log.info("Use " + version + " as helm version");
		return version;
	}

	private ReleaseResponse getReleaseResponse() throws MojoExecutionException {

		Optional<ReleaseCache> cache = readCache();

		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(LATEST_RELEASE_URL).openConnection();
			connection.setRequestProperty("User-Agent", userAgent);
			connection.setRequestProperty("Accept", "application/json");
			cache.map(ReleaseCache::getEtag).ifPresent(etag -> connection.setRequestProperty("If-None-Match", etag));

			int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
				log.debug("Cache not modified");
				return cache.get().getResponse();
			}
			if (responseCode != HttpURLConnection.HTTP_OK) {
				throw new MojoExecutionException(
						"Failed to get helm version from github api, response code was " + responseCode);
			}

			ReleaseResponse response = mapper.readValue(connection.getInputStream(), ReleaseResponse.class);
			log.debug("Got valid response from github");
			writeCache(new ReleaseCache().setEtag(connection.getHeaderField("ETag")).setResponse(response));

			return response;
		} catch (IOException e) {
			throw new MojoExecutionException("Failed to get helm version", e);
		}
	}

	private void writeCache(ReleaseCache cache) {
		Path file = tmpDir.resolve(RELEASE_FILE);
		try {
			Files.createDirectories(tmpDir);
			mapper.writeValue(tmpDir.resolve(RELEASE_FILE).toFile(), cache);
			log.debug("Wrote Github cache to " + file);
		} catch (IOException e) {
			log.warn("Failed to write cache to " + file, e);
		}
	}

	private Optional<ReleaseCache> readCache() {

		Path file = tmpDir.resolve(RELEASE_FILE);
		if (!Files.exists(file)) {
			log.debug("Github cache not found at " + file);
			return Optional.empty();
		}

		try {
			ReleaseCache cache = mapper.readValue(file.toFile(), ReleaseCache.class);
			log.debug("Github cache found at " + file);
			return Optional.of(cache);
		} catch (IOException e) {
			log.warn("Failed to read cache from " + file, e);
		}

		try {
			Files.delete(file);
		} catch (IOException e) {
			log.warn("Failed to delete invalid cache file " + file, e);
		}

		return Optional.empty();
	}
}
