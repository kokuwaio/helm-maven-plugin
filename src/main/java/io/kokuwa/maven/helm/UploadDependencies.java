package io.kokuwa.maven.helm;

import java.io.File;
import java.nio.file.Paths;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kokuwa.maven.helm.pojo.HelmRepository;
import io.kokuwa.maven.helm.pojo.RepoType;

@Mojo(name = "upload-dependencies")
public class UploadDependencies extends UploadMojo {

	private static final Logger logger = LoggerFactory.getLogger(UploadDependencies.class);

	@Parameter(property = "url", required = true)
	private String url;

	@Parameter(property = "id", required = true)
	private String id;

	@Parameter(property = "type", required = true)
	private RepoType type;

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;

	@Override
	public void execute() throws MojoExecutionException {
		String projectPath = project.getBasedir().getAbsolutePath();
		if (!isHelmChart(projectPath)) {
			return;
		}
		uploadRepoStable = createHelmRepository(id, url, type);
		uploadRepoSnapshot = createHelmRepository(id, url, type);
		outputDirectory = new File(Paths.get(projectPath, "charts").toString());
		if (!outputDirectory.exists()) {
			return;
		}
		try {
			super.execute();
		} catch (Exception e) {
			logger.info(String.format("%s:%s", e.getMessage(), e.getCause()));
		}
	}

	private boolean isHelmChart(String path) {
		File chartFile = new File(Paths.get(path, "Chart.yaml").toString());
		return chartFile.exists();
	}

	private HelmRepository createHelmRepository(String id, String url, RepoType type) {
		HelmRepository repo = new HelmRepository();
		repo.setName(id);
		repo.setUrl(url);
		repo.setType(type);
		return repo;
	}

}
