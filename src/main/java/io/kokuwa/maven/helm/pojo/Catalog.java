package io.kokuwa.maven.helm.pojo;

import java.net.URL;
import java.nio.file.Path;

import lombok.Data;

@Data
public class Catalog {
	private final Path chart;
	private final URL uploadUrl;
	private final String uploadResponseType;
	private final String uploadResponse;
}
