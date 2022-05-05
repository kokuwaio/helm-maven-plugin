package io.kokuwa.maven.helm;

import java.io.IOException;
import java.net.PasswordAuthentication;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.kokuwa.maven.helm.pojo.HelmRepository;
import lombok.Setter;

@Mojo(name = "push", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
@Setter
public class PushMojo extends AbstractHelmMojo {

	private static final String LOGIN_COMMAND_TEMPLATE = "registry login -u %s %s --password-stdin";
	private static final String CHART_PUSH_TEMPLATE = "push %s oci://%s";

	@Parameter(property = "helm.push.skip", defaultValue = "false")
	private boolean skipPush;

	@Override
	public void execute() throws MojoExecutionException {

		if (skip || skipPush) {
			getLog().info("Skip push");
			return;
		}

		HelmRepository registry = getHelmUploadRepo();
		if (Objects.isNull(registry)) {
			getLog().info("there is no helm repo. skipping the upload.");
			return;
		}

		if (isUseLocalHelmBinary()) {
			getLog().debug("helm version unknown");
		} else {
			ComparableVersion helmVersion = new ComparableVersion(getHelmVersion());
			ComparableVersion minimumHelmVersion = new ComparableVersion("3.8.0");
			if (helmVersion.compareTo(minimumHelmVersion) < 0) {
				getLog().error("your helm version is " + helmVersion.toString() + ", it's required to be >=3.8.0");
				throw new IllegalStateException();
			} else {
				getLog().debug("helm version minimum satisfied. the version is: " + helmVersion);
			}
		}

		PasswordAuthentication authentication = getAuthentication(registry);
		if (authentication != null) {
			String arguments = String.format(LOGIN_COMMAND_TEMPLATE, authentication.getUserName(), registry.getUrl());
			helm(arguments, "can't login to registry", new String(authentication.getPassword()));
		}

		getLog().info("Uploading to " + registry.getUrl());
		getChartTgzs(getOutputDirectory())
				.forEach(
						chartTgzFile -> {
							getLog().info("Uploading " + chartTgzFile);
							try {
								uploadSingle(Paths.get(chartTgzFile), registry);
							} catch (MojoExecutionException e) {
								throw new RuntimeException(e);
							}
						});
	}

	private void uploadSingle(Path tgz, HelmRepository registry) throws MojoExecutionException {
		helm(String.format(CHART_PUSH_TEMPLATE, tgz, registry.getUrl()), "Upload failed");
	}

	List<String> getChartTgzs(String path) throws MojoExecutionException {

		try (Stream<Path> files = Files.walk(Paths.get(path))) {
			return files
					.filter(p -> FileNameUtils.getExtension(p.toFile().getName()).equals("tgz"))
					.map(Path::toString)
					.collect(Collectors.toList());
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to scan repo directory at " + path, e);
		}
	}
}
