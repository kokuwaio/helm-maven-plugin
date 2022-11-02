package io.kokuwa.maven.helm;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;

import io.kokuwa.maven.helm.pojo.HelmRepository;
import lombok.Setter;
import lombok.SneakyThrows;

/**
 * Mojo for initializing helm.
 *
 * @author Fabian Schlegel
 * @since 1.0
 */
@Mojo(name = "init", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
@Setter
public class InitMojo extends AbstractHelmMojo {

	public static final String STABLE_HELM_REPO = "https://charts.helm.sh/stable";

	/**
	 * Set this to <code>true</code> to skip invoking init goal.
	 *
	 * @since 3.3
	 */
	@Parameter(property = "helm.init.skip", defaultValue = "false")
	private boolean skipInit;

	/**
	 * If <code>true</code>, stable repo (https://charts.helm.sh/stable) will be added.
	 *
	 * @since 5.1
	 */
	@Parameter(property = "helm.init.add-default-repo", defaultValue = "true")
	private boolean addDefaultRepo;

	/**
	 * If <code>true</code>, upload repos (uploadRepoStable, uploadRepoSnapshot) will be added, if configured.
	 *
	 * @since 5.10
	 */
	@Parameter(property = "helm.init.add-upload-repos", defaultValue = "false")
	private boolean addUploadRepos;

	/**
	 * Additional repositories to add.
	 *
	 * @since 1.8
	 */
	@Parameter
	private HelmRepository[] helmExtraRepos;

	/**
	 * If <code>true</code>, replaces (overwrite) the repo if they already exists. Can be also specified on repository
	 * level in "helmExtraRepos".
	 *
	 * @since 6.6.0
	 */
	@Parameter(property = "helm.repo.add.force-update", defaultValue = "false")
	private boolean repositoryAddForceUpdate;

	/**
	 * Download url of helm.
	 *
	 * @since 1.0
	 */
	@Parameter(property = "helm.downloadUrl")
	private URL helmDownloadUrl;

	/**
	 * Username used to authenticate while downloading helm binary package.
	 *
	 * @since 6.3.0
	 */
	@Parameter(property = "helm.downloadUser")
	private String helmDownloadUser;

	/**
	 * Password used to authenticate while downloading helm binary package.
	 *
	 * @since 6.3.0
	 */
	@Parameter(property = "helm.downloadPassword")
	private String helmDownloadPassword;

	/**
	 * ServerId which has username and password used to authenticate while downloading helm binary package.
	 *
	 * @since 6.3.0
	 */
	@Parameter(property = "helm.downloadServerId")
	private String helmDownloadServerId;

	@Override
	public void execute() throws MojoExecutionException {

		if (skip || skipInit) {
			getLog().info("Skip init");
			return;
		}

		getLog().info("Initializing Helm...");
		Path outputDirectory = getOutputDirectory();
		if (!outputDirectory.toFile().exists()) {
			getLog().info("Creating output directory...");
			try {
				Files.createDirectories(outputDirectory);
			} catch (IOException e) {
				throw new MojoExecutionException("Unable to create output directory at " + outputDirectory, e);
			}
		}

		if (isUseLocalHelmBinary()) {
			verifyLocalHelmBinary();
			getLog().info("Using local HELM binary [" + getHelmExecuteablePath() + "]");
		} else {
			downloadAndUnpackHelm();
		}

		if (addDefaultRepo) {
			HelmRepository stableHelmRepo = new HelmRepository();
			stableHelmRepo.setName("stable");
			stableHelmRepo.setUrl(STABLE_HELM_REPO);
			addRepository(stableHelmRepo, false);
		}

		if (addUploadRepos) {
			if (getUploadRepoStable() != null) {
				addRepository(getUploadRepoStable(), true);
			}

			// add the upload snapshot repo only if it's name differs to the upload repo stable name
			if (getUploadRepoSnapshot() != null && (getUploadRepoStable() == null
					|| !getUploadRepoStable().getName().equals(getUploadRepoSnapshot().getName()))) {
				addRepository(getUploadRepoSnapshot(), true);
			}
		}

		if (helmExtraRepos != null) {
			for (HelmRepository repository : helmExtraRepos) {
				addRepository(repository, true);
			}
		}
	}

	private void addRepository(HelmRepository repository, boolean authenticationRequired)
			throws MojoExecutionException {
		getLog().info("Adding repo [" + repository + "]");
		String arguments = "repo add " + repository.getName() + " " + repository.getUrl();
		PasswordAuthentication auth = authenticationRequired ? getAuthentication(repository) : null;
		if (auth != null) {
			arguments += " --username=" + auth.getUserName() + " --password=" + String.valueOf(auth.getPassword());
		}
		if (repositoryAddForceUpdate || repository.isForceUpdate()) {
			arguments += " --force-update";
		}
		helm(arguments, "Unable add repo", null);
	}

	@SneakyThrows(MalformedURLException.class)
	private void downloadAndUnpackHelm() throws MojoExecutionException {

		Path directory = getHelmExecutableDirectory();
		if (Files.exists(getHelmExecuteableName())) {
			getLog().info("Found helm executable, skip init.");
			return;
		}

		URL url = helmDownloadUrl;
		if (url == null) {
			url = new URL(String.format("https://get.helm.sh/helm-v%s-%s-%s.%s",
					getHelmVersion(),
					getOperatingSystem(),
					getArchitecture(),
					getExtension()));
		}

		getLog().debug("Downloading Helm: " + url);
		boolean found = false;

		Server downloadServer = getSettings().getServer(helmDownloadServerId);

		if (StringUtils.isNotBlank(helmDownloadUser) && StringUtils.isNotBlank(helmDownloadPassword)
				&& downloadServer != null) {
			throw new MojoExecutionException("Either use only helm.downloadUser and helm.downloadPassword " +
					"or helm.downloadServerId properties");
		}

		if (StringUtils.isNotBlank(helmDownloadUser) && StringUtils.isNotBlank(helmDownloadPassword)) {
			Authenticator.setDefault(new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(helmDownloadUser, helmDownloadPassword.toCharArray());
				}
			});
		}

		if (downloadServer != null) {
			Authenticator.setDefault(new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(
							downloadServer.getUsername(),
							downloadServer.getPassword().toCharArray());
				}
			});
		}

		try (InputStream dis = url.openStream();
				InputStream cis = createCompressorInputStream(dis);
				ArchiveInputStream is = createArchiverInputStream(cis)) {

			// create directory if not present
			Files.createDirectories(directory);

			// get helm executable entry
			ArchiveEntry entry = null;
			while ((entry = is.getNextEntry()) != null) {

				String name = entry.getName();
				if (entry.isDirectory() || !name.endsWith("helm.exe") && !name.endsWith("helm")) {
					getLog().debug("Skip archive entry with name: " + name);
					continue;
				}

				getLog().debug("Use archive entry with name: " + name);
				Path helm = directory.resolve(name.endsWith(".exe") ? "helm.exe" : "helm");
				try (FileOutputStream output = new FileOutputStream(helm.toFile())) {
					IOUtils.copy(is, output);
				}

				addExecPermission(helm);

				found = true;
				break;
			}

		} catch (IOException e) {
			throw new MojoExecutionException("Unable to download and extract helm executable.", e);
		}

		if (!found) {
			throw new MojoExecutionException("Unable to find helm executable in tar file.");
		}
	}

	private void addExecPermission(Path helm) throws IOException {
		Set<String> fileAttributeView = FileSystems.getDefault().supportedFileAttributeViews();

		if (fileAttributeView.contains("posix")) {
			Set<PosixFilePermission> permissions;
			try {
				permissions = Files.getPosixFilePermissions(helm);
			} catch (UnsupportedOperationException e) {
				getLog().debug("Exec file permission is not set", e);
				return;
			}
			permissions.add(PosixFilePermission.OWNER_EXECUTE);
			Files.setPosixFilePermissions(helm, permissions);

		} else if (fileAttributeView.contains("acl")) {
			String username = System.getProperty("user.name");
			UserPrincipal userPrincipal = FileSystems.getDefault().getUserPrincipalLookupService()
					.lookupPrincipalByName(username);
			AclEntry aclEntry = AclEntry.newBuilder().setPermissions(AclEntryPermission.EXECUTE)
					.setType(AclEntryType.ALLOW).setPrincipal(userPrincipal).build();

			AclFileAttributeView acl = Files.getFileAttributeView(helm, AclFileAttributeView.class,
					LinkOption.NOFOLLOW_LINKS);
			List<AclEntry> aclEntries = acl.getAcl();
			aclEntries.add(aclEntry);
			acl.setAcl(aclEntries);
		}
	}

	private void verifyLocalHelmBinary() throws MojoExecutionException {
		helm("version", "Unable to verify local HELM binary", null);
	}

	private ArchiveInputStream createArchiverInputStream(InputStream is) throws MojoExecutionException {

		// Stream must support mark to allow for auto detection of compressor
		InputStream inputStream = is.markSupported() ? is : new BufferedInputStream(is);

		try {
			return new ArchiveStreamFactory().createArchiveInputStream(inputStream);
		} catch (ArchiveException e) {
			throw new MojoExecutionException("Unsupported archive type downloaded", e);
		}
	}

	private InputStream createCompressorInputStream(InputStream is) throws MojoExecutionException {

		// Stream must support mark to allow for auto detection of compressor
		InputStream inputStream = is.markSupported() ? is : new BufferedInputStream(is);

		// Detect if stream is compressed
		String compressorType = null;
		try {
			compressorType = CompressorStreamFactory.detect(inputStream);
		} catch (CompressorException e) {
			getLog().debug("Unknown type of compressed stream", e);
		}

		// If compressed then wrap with compressor stream
		if (compressorType != null) {
			try {
				return new CompressorStreamFactory().createCompressorInputStream(compressorType, inputStream);
			} catch (CompressorException e) {
				throw new MojoExecutionException("Unsupported compressor type: " + compressorType);
			}
		}

		return inputStream;
	}

	private String getArchitecture() {
		String architecture = System.getProperty("os.arch").toLowerCase(Locale.US);

		if (architecture.equals("x86_64") || architecture.equals("amd64") || architecture.equals("aarch64")) {
			return "amd64";
		} else if (architecture.equals("x86") || architecture.equals("i386")) {
			return "386";
		} else if (architecture.contains("arm64")) {
			return "arm64";
		} else if (architecture.equals("aarch32") || architecture.startsWith("arm")) {
			return "arm";
		} else if (architecture.contains("ppc64le")
				|| architecture.contains("ppc64") && System.getProperty("sun.cpu.endian").equals("little")) {
			return "ppc64le";
		}

		throw new IllegalStateException("Unsupported architecture: " + architecture);
	}

	private String getExtension() {
		return Os.OS_FAMILY.equals(Os.FAMILY_WINDOWS) ? "zip" : "tar.gz";
	}

	private String getOperatingSystem() {
		switch (Os.OS_FAMILY) {
			case Os.FAMILY_UNIX:
				return "linux";
			case Os.FAMILY_MAC:
				return "darwin";
			case Os.FAMILY_WINDOWS:
				return "windows";
			default:
				throw new IllegalStateException("Unsupported OS: " + Os.OS_FAMILY);
		}
	}
}
