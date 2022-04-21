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
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.MatchPatterns;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

import io.kokuwa.maven.helm.github.Github;
import io.kokuwa.maven.helm.pojo.HelmRepository;
import io.kokuwa.maven.helm.pojo.K8SCluster;


/**
 * Base class for mojos
 *
 * @author Fabian Schlegel
 * @since 06.11.17
 */
public abstract class AbstractHelmMojo extends AbstractMojo {

	@Component(role = org.sonatype.plexus.components.sec.dispatcher.SecDispatcher.class, hint = "default")
	private SecDispatcher securityDispatcher;

	@Parameter(property = "helm.useLocalHelmBinary", defaultValue = "false")
	private boolean useLocalHelmBinary;

	@Parameter(property = "helm.autoDetectLocalHelmBinary", defaultValue = "true")
	private boolean autoDetectLocalHelmBinary;

	@Parameter(property = "helm.executableDirectory", defaultValue = "${project.build.directory}/helm")
	private String helmExecutableDirectory;

	@Parameter(property = "helm.outputDirectory", defaultValue = "${project.build.directory}/helm/repo")
	private String outputDirectory;

	@Parameter(property = "helm.excludes")
	private String[] excludes;

	@Parameter(property = "helm.chartDirectory", required = true)
	private String chartDirectory;

	@Parameter(property = "helm.chartVersion")
	private String chartVersion;

	@Parameter(property = "helm.chartVersion.timestampOnSnapshot")
	private boolean timestampOnSnapshot;

	@Parameter(property = "helm.chartVersion.timestampFormat", defaultValue = "yyyyMMddHHmmss")
	private String timestampFormat;

	@Parameter(property = "helm.appVersion")
	private String appVersion;

	@Parameter(property = "helm.uploadRepo.stable")
	private HelmRepository uploadRepoStable;

	@Parameter(property = "helm.uploadRepo.snapshot")
	private HelmRepository uploadRepoSnapshot;

	@Parameter(property = "helm.downloadUrl")
	private String helmDownloadUrl;

	@Parameter(property = "helm.version")
	private String helmVersion;

	/** UserAgent to use for accessing Github api. */
	@Parameter(property = "helm.githubUserAgent", defaultValue = "kokuwaio/helm-maven-plugin")
	private String githubUserAgent;

	/** Directory where to store Github cache. */
	@Parameter(property = "helm.tmpDir", defaultValue = "${java.io.tmpdir}/helm-maven-plugin")
	private String tmpDir;

	@Parameter(property = "helm.registryConfig")
	private String registryConfig;

	@Parameter(property = "helm.repositoryCache")
	private String repositoryCache;

	@Parameter(property = "helm.repositoryConfig")
	private String repositoryConfig;

	@Parameter(property = "helm.extraRepos")
	private HelmRepository[] helmExtraRepos;

	@Parameter(property = "helm.security", defaultValue = "~/.m2/settings-security.xml")
	private String helmSecurity;

	@Parameter(property = "helm.skip", defaultValue = "false")
	protected boolean skip;

	@Parameter(property = "helm.k8s")
	private K8SCluster k8sCluster;

	/**
	 * The current user system settings for use in Maven.
	 */
	@Parameter(defaultValue = "${settings}", readonly = true)
	private Settings settings;

	@Parameter(defaultValue="${project.groupId}", readonly=true)
	String projectGroupId;

	@Parameter(defaultValue="${project.version}", readonly=true)
	String projectVersion;

	private Clock clock = Clock.systemDefaultZone();
	private StripSensitiveDataLog log;

	@Override
	public void setLog(Log log) {
		super.setLog(new StripSensitiveDataLog(log));
	}

	Path getHelmExecuteablePath() throws MojoExecutionException {
		String helmExecutable = SystemUtils.IS_OS_WINDOWS ? "helm.exe" : "helm";
		Optional<Path> path;
		if (isUseLocalHelmBinary() && isAutoDetectLocalHelmBinary()) {
			path = findInPath(helmExecutable);
		} else {
			path = Optional.of(Paths.get(helmExecutableDirectory, helmExecutable))
					.map(Path::toAbsolutePath)
					.filter(Files::exists);
		}

		return path.orElseThrow(() -> new MojoExecutionException("Helm executable is not found."));
	}

	/**
	 * Finds the absolute path to a given {@code executable} in {@code PATH} environment variable.
	 *
	 * @param executable the name of the executable to search for
	 * @return the absolute path to the executable if found, otherwise an empty optional.
	 */
	private Optional<Path> findInPath(String executable) {

		String[] paths = getPathsFromEnvironmentVariables();
		return Stream.of(paths)
				.map(Paths::get)
				.map(path -> path.resolve(executable))
				.filter(Files::exists)
				.map(Path::toAbsolutePath)
				.findFirst();
	}

	String[] getPathsFromEnvironmentVariables() {

		return System.getenv("PATH").split(Pattern.quote(File.pathSeparator));
	}

	String getCurrentTimestamp(){
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(getTimestampFormat());
		LocalDateTime currentTime = LocalDateTime.now(clock);
		return dateTimeFormatter.format(currentTime);
	}

	/**
	 * Calls cli with specified command
	 *
	 * @param command the command to be executed
	 * @param errorMessage a readable error message that will be shown in case of exceptions
	 * @param verbose logs STDOUT to Maven info log
	 * @throws MojoExecutionException on error
	 */
	void callCli(String command, String errorMessage, boolean verbose) throws MojoExecutionException {
		callCli(command, errorMessage, verbose, null);
	}

	/**
	 * Calls cli with specified command
	 *
	 * @param command the command to be executed
	 * @param errorMessage a readable error message that will be shown in case of exceptions
	 * @param verbose logs STDOUT to Maven info log
	 * @param stdin STDIN which is passed to the helm process
	 * @throws MojoExecutionException on error
	 */
	void callCli(String command, String errorMessage, boolean verbose, String stdin) throws MojoExecutionException {
		int exitValue;

		command += getK8SArgs();

		getLog().debug(command);

		try {
			Process p = Runtime.getRuntime().exec(command);
			new Thread(() -> {
				if (StringUtils.isNotEmpty(stdin)) {
					try (OutputStream outputStream = p.getOutputStream()) {
						outputStream.write( stdin.getBytes(StandardCharsets.UTF_8) );
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
			getLog().error("Error processing command [" + command + "]", e);
			throw new MojoExecutionException("Error processing command", e);
		}

		if (exitValue != 0) {
			throw new MojoExecutionException(errorMessage);
		}
	}

	String getK8SArgs() {
		StringBuilder k8sConfigArgs = new StringBuilder();
		if (k8sCluster != null){
			if(StringUtils.isNotEmpty(k8sCluster.getApiUrl())) {
				k8sConfigArgs.append(" --kube-apiserver ").append(k8sCluster.getApiUrl());
			}
			if(StringUtils.isNotEmpty(k8sCluster.getNamespace())) {
				k8sConfigArgs.append(" --namespace ").append(k8sCluster.getNamespace());
			}
			if(StringUtils.isNotEmpty(k8sCluster.getAsUser())) {
				k8sConfigArgs.append(" --kube-as-user ").append(k8sCluster.getAsUser());
			}
			if(StringUtils.isNotEmpty(k8sCluster.getAsGroup())) {
				k8sConfigArgs.append(" --kube-as-group ").append(k8sCluster.getAsGroup());
			}
			if(StringUtils.isNotEmpty(k8sCluster.getToken())) {
				k8sConfigArgs.append(" --kube-token ").append(k8sCluster.getToken());
			}
		}
		return k8sConfigArgs.toString();
	}

	List<String> getChartDirectories(String path) throws MojoExecutionException {
		List<String> exclusions = new ArrayList<>();

		if (getExcludes() != null) {
			exclusions.addAll(Arrays.asList(getExcludes()));
		}

		exclusions.addAll(FileUtils.getDefaultExcludesAsList());

		MatchPatterns exclusionPatterns = MatchPatterns.from(exclusions);

		try (Stream<Path> files = Files.walk(Paths.get(path), FileVisitOption.FOLLOW_LINKS)) {
			List<String> chartDirs = files.filter(p -> p.getFileName().toString().equalsIgnoreCase("chart.yaml"))
					.map(p -> p.getParent().toString())
					.filter(shouldIncludeDirectory(exclusionPatterns))
					.collect(Collectors.toList());

			if (chartDirs.isEmpty()) {
				getLog().warn("No Charts detected - no Chart.yaml files found below " + path);
			}

			return chartDirs;
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to scan chart directory at " + path, e);
		}
	}

	private Predicate<String> shouldIncludeDirectory(MatchPatterns exclusionPatterns) {
		return inputDirectory -> {

			boolean isCaseSensitive = Boolean.FALSE;
			boolean matches = exclusionPatterns.matches(inputDirectory, isCaseSensitive);

			if (matches) {
				getLog().debug("Skip excluded directory " + inputDirectory);
				return Boolean.FALSE;
			}

			return Boolean.TRUE;
		};
	}

	List<String> getChartFiles(String path) throws MojoExecutionException {
		try (Stream<Path> files = Files.walk(Paths.get(path))) {
			return files.filter(this::isChartFile)
					.map(Path::toString)
					.collect(Collectors.toList());
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to scan repo directory at " + path, e);
		}
	}

	private boolean isChartFile(Path p) {
		String filename = p.getFileName().toString();
		return filename.endsWith(".tgz") || filename.endsWith("tgz.prov");
	}

	/**
	 * Returns the proper upload URL based on the provided chart version.
	 * Charts w/ an SNAPSHOT suffix will be uploaded to SNAPSHOT repo.
	 *
	 * @return Upload URL based on chart version
	 */
	String getHelmUploadUrl() {
		String uploadUrl = uploadRepoStable.getUrl();
		if (chartVersion != null && chartVersion.endsWith("-SNAPSHOT")
				&& uploadRepoSnapshot != null
				&& StringUtils.isNotEmpty(uploadRepoSnapshot.getUrl()))
		{
			uploadUrl = uploadRepoSnapshot.getUrl();
		}

		return uploadUrl;
	}

	HelmRepository getHelmUploadRepo() {
		if (chartVersion != null && chartVersion.endsWith("-SNAPSHOT")
				&& uploadRepoSnapshot != null
				&& StringUtils.isNotEmpty(uploadRepoSnapshot.getUrl()))
		{
			return uploadRepoSnapshot;
		}
		return uploadRepoStable;
	}

	/**
	 * Get credentials for given helm repo. If username is not provided the repo
	 * name will be used to search for credentials in <code>settings.xml</code>.
	 *
	 * @param repository Helm repo with id and optional credentials.
	 * @return Authentication object or <code>null</code> if no credentials are present.
	 * @throws IllegalArgumentException Unable to get authentication because of misconfiguration.
	 * @throws MojoExecutionException Unable to get password from settings.xml
	 */
	PasswordAuthentication getAuthentication(HelmRepository repository)
			throws IllegalArgumentException, MojoExecutionException
	{
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
			return new PasswordAuthentication(server.getUsername(),
					getSecDispatcher().decrypt(server.getPassword()).toCharArray());
		} catch (SecDispatcherException e) {
			throw new MojoExecutionException(e.getMessage());
		}
	}

	protected SecDispatcher getSecDispatcher() {
		if (securityDispatcher instanceof DefaultSecDispatcher) {
			((DefaultSecDispatcher) securityDispatcher).setConfigurationFile(getHelmSecurity());
		}
		return securityDispatcher;
	}

	protected String formatIfValueIsNotEmpty(String format, String value) {
		if (StringUtils.isNotEmpty(value)) {
			return String.format(format, value);
		}
		return "";
	}

	public String getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public String getHelmExecutableDirectory() {
		return helmExecutableDirectory;
	}

	public void setHelmExecutableDirectory(String helmExecuteableDirectory) {
		this.helmExecutableDirectory = helmExecuteableDirectory;
	}

	public String getHelmDownloadUrl() {
		return helmDownloadUrl;
	}

	public void setHelmDownloadUrl(String helmDownloadUrl) {
		this.helmDownloadUrl = helmDownloadUrl;
	}

	public String getHelmVersion() throws MojoExecutionException {
		if (helmVersion == null) {
			helmVersion = new Github(getLog(), Paths.get(tmpDir), githubUserAgent).getHelmVersion();
		}
		return helmVersion;
	}

	public void setHelmVersion(String helmVersion) {
		this.helmVersion = helmVersion;
	}

	public String[] getExcludes() {
		return excludes;
	}

	public void setExcludes(String[] excludes) {
		this.excludes = excludes;
	}

	public String getChartDirectory() {
		return chartDirectory;
	}

	public void setChartDirectory(String chartDirectory) {
		this.chartDirectory = chartDirectory;
	}

	public String getRegistryConfig() {
		return registryConfig;
	}

	public void setRegistryConfig(String registryConfig) {
		this.registryConfig = registryConfig;
	}

	public String getRepositoryCache() {
		return repositoryCache;
	}

	public void setRepositoryCache(String repositoryCache) {
		this.repositoryCache = repositoryCache;
	}

	public String getRepositoryConfig() {
		return repositoryConfig;
	}

	public void setRepositoryConfig(String repositoryConfig) {
		this.repositoryConfig = repositoryConfig;
	}

	public String getChartVersion() {
		return chartVersion;
	}

	public void setChartVersion(String chartVersion) {
		this.chartVersion = chartVersion;
	}

	public String getChartVersionWithProcessing() {
		if (isTimestampOnSnapshot() && chartVersion.endsWith("-SNAPSHOT")) {
			return chartVersion.replace("SNAPSHOT", getCurrentTimestamp());
		}
		return chartVersion;
	}

	public String getAppVersion() {
		return appVersion;
	}

	public void setAppVersion(String appVersion) {
		this.appVersion = appVersion;
	}

	public HelmRepository[] getHelmExtraRepos() {
		return helmExtraRepos;
	}

	public void setHelmExtraRepos(HelmRepository[] helmExtraRepos) {
		this.helmExtraRepos = helmExtraRepos;
	}

	public HelmRepository getUploadRepoStable() {
		return uploadRepoStable;
	}

	public void setUploadRepoStable(HelmRepository uploadRepoStable) {
		this.uploadRepoStable = uploadRepoStable;
	}

	public HelmRepository getUploadRepoSnapshot() {
		return uploadRepoSnapshot;
	}

	public void setUploadRepoSnapshot(HelmRepository uploadRepoSnapshot) {
		this.uploadRepoSnapshot = uploadRepoSnapshot;
	}

	public String getHelmSecurity() {
		return helmSecurity;
	}

	public void setHelmSecurity(String helmSecurity) {
		this.helmSecurity = helmSecurity;
	}

	public Settings getSettings() {
		return settings;
	}

	public boolean isUseLocalHelmBinary() {
		return useLocalHelmBinary;
	}

	public void setUseLocalHelmBinary(boolean useLocalHelmBinary) {
		this.useLocalHelmBinary = useLocalHelmBinary;
	}

	public boolean isAutoDetectLocalHelmBinary() {
		return autoDetectLocalHelmBinary;
	}

	public void setAutoDetectLocalHelmBinary(boolean autoDetectLocalHelmBinary) {
		this.autoDetectLocalHelmBinary = autoDetectLocalHelmBinary;
	}

	public K8SCluster getK8SCluster() {
		return k8sCluster;
	}

	public void setK8SCluster(K8SCluster k8sCluster) {
		this.k8sCluster = k8sCluster;
	}

	public String getProjectGroupId() {
		return projectGroupId;
	}

	public void setProjectGroupId(String projectGroupId) {
		this.projectGroupId = projectGroupId;
	}

	public String getProjectVersion() {
		return projectVersion;
	}

	public void setProjectVersion(String projectVersion) {
		this.projectVersion = projectVersion;
	}

	public boolean isTimestampOnSnapshot() {
		return timestampOnSnapshot;
	}

	public void setTimestampOnSnapshot(boolean timestampOnSnapshot) {
		this.timestampOnSnapshot = timestampOnSnapshot;
	}

	public String getTimestampFormat() {
		return timestampFormat;
	}

	public void setTimestampFormat(String timestampFormat) {
		this.timestampFormat = timestampFormat;
	}

	public void setGithubUserAgent(String githubUserAgent) {
		this.githubUserAgent = githubUserAgent;
	}

	public void setTmpDir(String tmpDir) {
		this.tmpDir = tmpDir;
	}
}
