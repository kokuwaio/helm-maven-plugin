package com.kiwigrid.helm.maven.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import com.kiwigrid.helm.maven.plugin.pojo.HelmRepository;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

/**
 * Mojo for initializing helm
 *
 * @author Fabian Schlegel
 * @since 06.11.17
 */
@Mojo(name = "init", defaultPhase = LifecyclePhase.INITIALIZE)
public class InitMojo extends AbstractHelmMojo {

	@Parameter(property = "helm.init.skipRefresh", defaultValue = "false")
	private boolean skipRefresh;
	@Parameter(property = "helm.init.skip", defaultValue = "false")
	private boolean skipInit;

	public void execute() throws MojoExecutionException {
		
		if (skip || skipInit) {
			getLog().info("Skip init");
			return;
		}

		getLog().info("Initializing Helm...");
		Path outputDirectory = Paths.get(getOutputDirectory()).toAbsolutePath();
		if (!Files.exists(outputDirectory)) {
			getLog().info("Creating output directory...");
			try {
				Files.createDirectories(outputDirectory);
			} catch (IOException e) {
				throw new MojoExecutionException("Unable to create output directory at " + outputDirectory, e);
			}
		}

		if(isUseLocalHelmBinary()) {
			verifyLocalHelmBinary();
			getLog().info("Using local HELM binary ["+ getHelmExecutableDirectory() +"]");
		} else {
			downloadAndUnpackHelm();
		}

		if (getHelmExtraRepos() != null) {
			for (HelmRepository repository : getHelmExtraRepos()) {
				getLog().info("Adding repo " + repository);
				PasswordAuthentication auth = getAuthentication(repository);
				callCli(getHelmExecutableDirectory()
								+ File.separator
								+ "helm repo add "
								+ repository.getName()
								+ " "
								+ repository.getUrl()
								+ (StringUtils.isNotEmpty(getHelmHomeDirectory()) ? " --home=" + getHelmHomeDirectory() : "")
								+ (auth != null ? " --username=" + auth.getUserName() + " --password=" + String.valueOf(auth.getPassword()) : ""),
						"Unable add repo",
						false);
			}
		}
	}

	protected void downloadAndUnpackHelm() throws MojoExecutionException {

		Path directory = Paths.get(getHelmExecutableDirectory());
		if (Files.exists(directory.resolve(SystemUtils.IS_OS_WINDOWS ? "helm.exe" : "helm"))) {
			getLog().info("Found helm executable, skip init.");
			return;
		}

		getLog().info("Downloading Helm ...");
		boolean found = false;
		try (TarArchiveInputStream is = new TarArchiveInputStream(
				new GZIPInputStream(new URL(getHelmDownloadUrl()).openStream()))) {

			// create directory if not present
			Files.createDirectories(directory);

			// get helm executable entry
			while (is.getNextEntry() != null) {

				String name = is.getCurrentEntry().getName();
				if (is.getCurrentEntry().isDirectory() || (!name.endsWith("helm.exe") && !name.endsWith("helm"))) {
					getLog().debug("Skip archive entry with name: " + name);
					continue;
				}

				getLog().debug("Use archive entry with name: " + name);
				Path helm = directory.resolve(name.endsWith(".exe") ? "helm.exe" : "helm");
				try (FileOutputStream output = new FileOutputStream(helm.toFile())) {
					IOUtils.copy(is, output);
				}

				Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(helm);
				permissions.add(PosixFilePermission.OWNER_EXECUTE);
				Files.setPosixFilePermissions(helm, permissions);

				found = true;
				break;
			}

		} catch (IOException e) {
			throw new MojoExecutionException("Unable to download and extract helm executable.", e);
		}
		if (!found) {
			throw new MojoExecutionException("Unable to find helm executable in tar file.");
		}

		initHelmClient();
	}

	private void verifyLocalHelmBinary() throws MojoExecutionException {
		callCli(getHelmExecuteablePath() + " version --client", "Unable to verify local HELM binary", false);
		initHelmClient();
	}

	public boolean isSkipRefresh() {
		return skipRefresh;
	}

	public void setSkipRefresh(boolean skipRefresh) {
		this.skipRefresh = skipRefresh;
	}

	private void initHelmClient() throws MojoExecutionException {
		getLog().info("Run helm init...");

		callCli(getHelmExecuteablePath()
						+ " init --client-only" + (skipRefresh ? " --skip-refresh" : "")
						+ (StringUtils.isNotEmpty(getHelmHomeDirectory()) ? " --home=" + getHelmHomeDirectory() : ""),
				"Unable to call helm init",
				false);
	}
}
