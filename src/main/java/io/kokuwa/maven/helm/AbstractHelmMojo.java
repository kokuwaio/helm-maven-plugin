package io.kokuwa.maven.helm;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.PasswordAuthentication;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.MatchPatterns;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

import io.kokuwa.maven.helm.github.Github;
import io.kokuwa.maven.helm.pojo.HelmRepository;
import io.kokuwa.maven.helm.pojo.K8SCluster;
import lombok.Getter;
import lombok.Setter;

/**
 * Base class for mojos
 *
 * @author Fabian Schlegel
 * @since 1.0
 */
@Getter
@Setter
public abstract class AbstractHelmMojo extends AbstractMojo {

	/** Path of helm executeable. */
	private final Path helmExecuteableName = Paths.get(Os.isFamily(Os.FAMILY_WINDOWS) ? "helm.exe" : "helm");

	@Component(role = org.sonatype.plexus.components.sec.dispatcher.SecDispatcher.class, hint = "default")
	private SecDispatcher securityDispatcher;

	/**
	 * Controls whether a local binary should be used instead of downloading it.
	 * If set to <code>true</code> path has to be set with property "helm.executableDirectory".
	 *
	 * @since 4.0
	 */
	@Parameter(property = "helm.useLocalHelmBinary", defaultValue = "false")
	private boolean useLocalHelmBinary;

	/**
	 * Controls whether the local binary should be auto-detected from PATH environment variable.
	 * If set to <code>false</code> the binary in "helm.executableDirectory" is used.
	 * This property has no effect unless "helm.useLocalHelmBinary" is set to <code>true</code>.
	 *
	 * @since 4.1
	 */
	@Parameter(property = "helm.autoDetectLocalHelmBinary", defaultValue = "true")
	private boolean autoDetectLocalHelmBinary;

	/**
	 * Directory of your helm installation.
	 *
	 * @since 1.3
	 */
	@Parameter(property = "helm.executableDirectory", defaultValue = "${project.build.directory}/helm")
	private File helmExecutableDirectory;

	/**
	 * Chart output directory.
	 *
	 * @since 1.0
	 */
	@Parameter(property = "helm.outputDirectory", defaultValue = "${project.build.directory}/helm/repo")
	private File outputDirectory;

	/**
	 * List of chart directories to exclude.
	 *
	 * @since 1.0
	 */
	@Parameter(property = "helm.excludes")
	private String[] excludes;

	/**
	 * Root directory of your charts.
	 *
	 * @since 1.0
	 */
	@Parameter(property = "helm.chartDirectory", required = true)
	private File chartDirectory;

	/**
	 * Version of the charts. The version have to be in the SEMVER-Format (https://semver.org/), required by helm.
	 *
	 * @since 1.5
	 */
	@Parameter(property = "helm.chartVersion")
	private String chartVersion;

	/**
	 * Upload repository for stable charts.
	 *
	 * @since 1.10
	 */
	@Parameter
	private HelmRepository uploadRepoStable;

	/**
	 * Upload repository for snapshot charts (determined by version postfix 'SNAPSHOT').
	 *
	 * @since 1.10
	 */
	@Parameter
	private HelmRepository uploadRepoSnapshot;

	/**
	 * Version of helm to download.
	 *
	 * @since 5.5
	 */
	@Parameter(property = "helm.version")
	private String helmVersion;

	/**
	 * UserAgent to use for accessing Github api to identify latest version.
	 *
	 * @since 6.1.0
	 */
	@Parameter(property = "helm.githubUserAgent", defaultValue = "kokuwaio/helm-maven-plugin")
	private String githubUserAgent;

	/**
	 * Directory where to store Github cache.
	 *
	 * @since 6.1.0
	 */
	@Parameter(property = "helm.tmpDir", defaultValue = "${java.io.tmpdir}/helm-maven-plugin")
	private File tmpDir;

	/**
	 * Enable verbose output.
	 *
	 * @since 6.2.0
	 */
	@Parameter(property = "helm.debug", defaultValue = "false")
	private boolean debug;

	/**
	 * Path to the registry config file (default ~/.config/helm/registry/config.json).
	 *
	 * @since 5.0
	 */
	@Parameter(property = "helm.registryConfig")
	private File registryConfig;

	/**
	 * Path to the file containing cached repository indexes (default ~/.cache/helm/repository).
	 *
	 * @since 5.0
	 */
	@Parameter(property = "helm.repositoryCache")
	private File repositoryCache;

	/**
	 * Path to the file containing repository names and URLs (default ~/.config/helm/repositories.yaml).
	 *
	 * @since 5.0
	 */
	@Parameter(property = "helm.repositoryConfig")
	private File repositoryConfig;

	/**
	 * Path to security settings.
	 *
	 * @since 3.0
	 */
	@Parameter(property = "helm.security", defaultValue = "~/.m2/settings-security.xml")
	private String helmSecurity;

	/**
	 * Set this to <code>true</code> to skip all goals.
	 *
	 * @since 3.2
	 */
	@Parameter(property = "helm.skip", defaultValue = "false")
	protected boolean skip;

	/**
	 * Deprecated, use: "helm.kube*"
	 *
	 * @since 5.10
	 * @deprecated Duplicate with flags in {@link AbstractHelmMojo}. Will be removed in 7.x
	 */
	@Deprecated // java8 (since = "6.3.0", forRemoval = true)
	@Parameter
	private K8SCluster k8sCluster;

	/** The current user system settings for use in Maven. */
	@Parameter(defaultValue = "${settings}", readonly = true)
	private Settings settings;

	/**
	 * Namespace scope for this request.
	 *
	 * @since 6.4.0
	 */
	@Parameter(property = "helm.namespace")
	private String namespace;

	/**
	 * The address and the port for the Kubernetes API server.
	 *
	 * @since 6.4.0
	 */
	@Parameter(property = "helm.kubeApiServer")
	private String kubeApiServer;

	/**
	 * Username to impersonate for the operation.
	 *
	 * @since 6.4.0
	 */
	@Parameter(property = "helm.kubeAsUser")
	private String kubeAsUser;

	/**
	 * Group to impersonate for the operation, this flag can be repeated to specify multiple groups.
	 *
	 * @since 6.4.0
	 */
	@Parameter(property = "helm.kubeAsGroup")
	private String kubeAsGroup;

	/**
	 * Bearer token used for authentication.
	 *
	 * @since 6.4.0
	 */
	@Parameter(property = "helm.kubeToken")
	private String kubeToken;

	/**
	 * The certificate authority file for the Kubernetes API server connection.
	 *
	 * @since 6.4.0
	 */
	@Parameter(property = "helm.kubeCaFile")
	private File kubeCaFile;

	@Override
	public void setLog(Log log) {
		super.setLog(new StripSensitiveDataLog(log));
	}

	Path getHelmExecuteablePath() throws MojoExecutionException {
		Stream<Path> optional;
		if (useLocalHelmBinary && autoDetectLocalHelmBinary) {
			optional = Stream.of(System.getenv("PATH").split(Pattern.quote(File.pathSeparator))).map(Paths::get);
		} else {
			optional = Stream.of(helmExecutableDirectory.toPath());
		}
		return optional
				.map(directory -> directory.resolve(helmExecuteableName))
				.map(Path::toAbsolutePath)
				.filter(Files::isRegularFile)
				.filter(Files::isExecutable)
				.findFirst()
				.orElseThrow(() -> new MojoExecutionException("Helm executable not found."));
	}

	void helm(String arguments, String errorMessage, String stdin) throws MojoExecutionException {

		// get command

		StringBuilder command = new StringBuilder().append(getHelmExecuteablePath()).append(" ").append(arguments);
		if (debug) {
			command.append(" --debug");
		}
		if (registryConfig != null) {
			command.append(" --registry-config=").append(registryConfig);
		}
		if (repositoryConfig != null) {
			command.append(" --repository-config=").append(repositoryConfig);
		}
		if (repositoryCache != null) {
			command.append(" --repository-cache=").append(repositoryCache);
		}
		if (StringUtils.isNotEmpty(namespace)) {
			command.append(" --namespace=").append(namespace);
		}
		if (StringUtils.isNotEmpty(kubeApiServer)) {
			command.append(" --kube-apiserver=").append(kubeApiServer);
		}
		if (StringUtils.isNotEmpty(kubeAsUser)) {
			command.append(" --kube-as-user=").append(kubeAsUser);
		}
		if (StringUtils.isNotEmpty(kubeAsGroup)) {
			command.append(" --kube-as-group=").append(kubeAsGroup);
		}
		if (StringUtils.isNotEmpty(kubeToken)) {
			command.append(" --kube-token=").append(kubeToken);
		}
		if (kubeCaFile != null) {
			command.append(" --kube-ca-file=").append(kubeCaFile);
		}

		// execute helm

		String commandWithK8sArgs = command.append(getK8SArgs()).toString();
		getLog().debug(commandWithK8sArgs);

		// TODO: Remove in next major release
		warnOnMixOfK8sClusterAndGlobalFlags();

		int exitValue;
		try {
			Process p = Runtime.getRuntime().exec(commandWithK8sArgs);
			new Thread(() -> {
				if (StringUtils.isNotEmpty(stdin)) {
					try (OutputStream outputStream = p.getOutputStream()) {
						outputStream.write(stdin.getBytes(StandardCharsets.UTF_8));
					} catch (IOException ex) {
						getLog().error("failed to write to stdin of helm", ex);
					}
				}

				BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String inputLine;
				try {
					while ((inputLine = input.readLine()) != null) {
						getLog().info(inputLine);
					}
				} catch (IOException e) {
					getLog().error(e);
				}
			}).start();
			new Thread(() -> {
				BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				String errorLine;
				try {
					while ((errorLine = error.readLine()) != null) {
						getLog().error(errorLine);
					}
				} catch (IOException e) {
					getLog().error(e);
				}
			}).start();
			p.waitFor();
			exitValue = p.exitValue();
		} catch (Exception e) {
			getLog().error("Error processing command [" + commandWithK8sArgs + "]", e);
			throw new MojoExecutionException("Error processing command", e);
		}

		if (exitValue != 0) {
			throw new MojoExecutionException(errorMessage);
		}
	}

	List<Path> getChartDirectories() throws MojoExecutionException {

		List<String> exclusions = new ArrayList<>();
		if (excludes != null) {
			exclusions.addAll(Arrays.asList(excludes));
		}
		exclusions.addAll(FileUtils.getDefaultExcludesAsList());
		MatchPatterns exclusionPatterns = MatchPatterns.from(exclusions);

		try (Stream<Path> files = Files.walk(chartDirectory.toPath(), FileVisitOption.FOLLOW_LINKS)) {
			List<Path> chartDirectories = files
					.filter(p -> p.getFileName().toString().equalsIgnoreCase("chart.yaml"))
					.map(Path::getParent)
					.filter(p -> !exclusionPatterns.matches(p.toString(), false))
					.sorted(Comparator.reverseOrder())
					.collect(Collectors.toList());
			if (chartDirectories.isEmpty()) {
				getLog().warn("No Charts detected - no Chart.yaml files found below " + chartDirectory);
			}
			return chartDirectories;
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to scan chart directory at " + chartDirectory, e);
		}
	}

	List<Path> getChartArchives() throws MojoExecutionException {
		try (Stream<Path> files = Files.walk(getOutputDirectory())) {
			return files
					.filter(s -> Stream.of(".tgz", "tgz.prov").anyMatch(e -> s.getFileName().toString().endsWith(e)))
					.peek(p -> getLog().debug("Found chart file for upload: " + p))
					.collect(Collectors.toList());
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to scan repo directory at " + outputDirectory, e);
		}
	}

	HelmRepository getHelmUploadRepo() {
		if (chartVersion != null && chartVersion.endsWith("-SNAPSHOT")
				&& uploadRepoSnapshot != null
				&& StringUtils.isNotEmpty(uploadRepoSnapshot.getUrl())) {
			return uploadRepoSnapshot;
		}
		return uploadRepoStable;
	}

	/**
	 * Get credentials for given helm repo. If username is not provided the repo name will be used to search for
	 * credentials in <code>settings.xml</code>.
	 *
	 * @param repository Helm repo with id and optional credentials.
	 * @return Authentication object or <code>null</code> if no credentials are present.
	 * @throws IllegalArgumentException Unable to get authentication because of misconfiguration.
	 * @throws MojoExecutionException   Unable to get password from settings.xml
	 */
	PasswordAuthentication getAuthentication(HelmRepository repository)
			throws IllegalArgumentException, MojoExecutionException {
		String id = repository.getName();

		if (repository.getUsername() != null) {
			if (repository.getPassword() == null) {
				throw new IllegalArgumentException("Repo " + id + " has a username but no password defined.");
			}
			getLog().debug("Repo " + id + " has credentials defined, skip searching in server list.");
			return new PasswordAuthentication(repository.getUsername(), repository.getPassword().toCharArray());
		}

		Server server = settings.getServer(id);
		if (server == null) {
			getLog().info("No credentials found for " + id + " in configuration or settings.xml server list.");
			return null;
		}

		getLog().debug("Use credentials from server list for " + id + ".");
		if (server.getUsername() == null || server.getPassword() == null) {
			throw new IllegalArgumentException("Repo "
					+ id
					+ " was found in server list but has no username/password.");
		}

		try {
			if (securityDispatcher instanceof DefaultSecDispatcher) {
				((DefaultSecDispatcher) securityDispatcher).setConfigurationFile(helmSecurity);
			}
			return new PasswordAuthentication(server.getUsername(),
					securityDispatcher.decrypt(server.getPassword()).toCharArray());
		} catch (SecDispatcherException e) {
			throw new MojoExecutionException(e.getMessage());
		}
	}

	String getK8SArgs() {
		StringBuilder k8sConfigArgs = new StringBuilder();
		if (k8sCluster != null) {
			if (StringUtils.isNotEmpty(k8sCluster.getApiUrl())) {
				k8sConfigArgs.append(" --kube-apiserver=").append(k8sCluster.getApiUrl());
			}
			if (StringUtils.isNotEmpty(k8sCluster.getNamespace())) {
				k8sConfigArgs.append(" --namespace=").append(k8sCluster.getNamespace());
			}
			if (StringUtils.isNotEmpty(k8sCluster.getAsUser())) {
				k8sConfigArgs.append(" --kube-as-user=").append(k8sCluster.getAsUser());
			}
			if (StringUtils.isNotEmpty(k8sCluster.getAsGroup())) {
				k8sConfigArgs.append(" --kube-as-group=").append(k8sCluster.getAsGroup());
			}
			if (StringUtils.isNotEmpty(k8sCluster.getToken())) {
				k8sConfigArgs.append(" --kube-token=").append(k8sCluster.getToken());
			}
			if (k8sConfigArgs.length() > 0) {
				getLog().warn("NOTE: <k8sCluster> option will be removed in future major release.");
			}
		}
		return k8sConfigArgs.toString();
	}

	private void warnOnMixOfK8sClusterAndGlobalFlags() {

		if (k8sCluster == null) {
			return;
		}

		StringBuilder warnMessage = new StringBuilder();
		if (StringUtils.isNotEmpty(k8sCluster.getApiUrl()) && StringUtils.isNotEmpty(kubeApiServer)) {
			warnMessage.append("Both <kubeApiServer> and <k8sCluster><apiUrl/></k8sCluster> are set.\n");
		}
		if (StringUtils.isNotEmpty(k8sCluster.getNamespace()) && StringUtils.isNotEmpty(namespace)) {
			warnMessage.append("Both <namespace> and <k8sCluster><namespace/></k8sCluster> are set.\n");
		}
		if (StringUtils.isNotEmpty(k8sCluster.getAsUser()) && StringUtils.isNotEmpty(kubeAsUser)) {
			warnMessage.append("Both <kubeAsUser> and <k8sCluster><asUser/></k8sCluster> are set.\n");
		}
		if (StringUtils.isNotEmpty(k8sCluster.getAsGroup()) && StringUtils.isNotEmpty(kubeAsGroup)) {
			warnMessage.append("Both <kubeAsGroup> and <k8sCluster><asGroup/></k8sCluster> are set.\n");
		}
		if (StringUtils.isNotEmpty(k8sCluster.getToken()) && StringUtils.isNotEmpty(kubeToken)) {
			warnMessage.append("Both <kubeToken> and <k8sCluster><token/></k8sCluster> are set.\n");
		}

		if (warnMessage.length() > 0) {
			warnMessage.append("As per current implementation - <k8sCluster><*></k8sCluster> options win.\n");
			warnMessage.append("NOTE: <k8sCluster> option will be removed in future major release.");
			getLog().warn(warnMessage.toString());
		}
	}

	// getter

	public String getHelmVersion() throws MojoExecutionException {
		if (helmVersion == null) {
			helmVersion = new Github(getLog(), tmpDir.toPath(), githubUserAgent).getHelmVersion();
		}
		return helmVersion;
	}

	public Path getOutputDirectory() {
		return outputDirectory.toPath();
	}

	public Path getHelmExecutableDirectory() {
		return helmExecutableDirectory.toPath();
	}
}
